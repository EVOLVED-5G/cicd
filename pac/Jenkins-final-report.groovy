String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    } else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'evol5-cosmote'
    } else {
        return 'evol5-slave'
    }
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(3)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'Netapp branch name')
        string(name: 'VERSION_NETAPP', defaultValue: '1.0', description: 'Version Network App')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote'])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY = credentials('artifactory_credentials')
        VERSION_NETAPP = "${params.VERSION_NETAPP}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        STAGE = "${params.STAGE}"
        TOKEN = credentials('github_token_cred')
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        DOCKER_PATH = '/usr/src/app'
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
        USE_5G_APIS_REPORT_FILENAME = '006-report-use-of-5g-api'
    }

    stages {
        stage('Prepare pdf generator tools') {
            when {
                expression {
                    return REPORTING
                }
            }
            options {
                retry(2)
            }

            steps {
                dir("${env.WORKSPACE}") {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker_pull_cred',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                        sh '''
                        docker login --username ${USER} --password ${PASS} dockerhub.hi.inet
                        docker pull ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION}
                        '''
                }
                }
            }
        }
        stage('Generate steps summary an use of 5g APIs') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                mkdir executive_summary
                cd executive_summary

                response=$(curl -s "http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/$STAGE/$NETAPP_NAME_LOWER/$BUILD_ID" -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep ".json" | tr -d '"' )
                artifacts=($response)

                for x in "${artifacts[@]}"
                do
                    echo $x
                    url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$x"
                    curl -u $PASSWORD_ARTIFACTORY $url -o $x
                done

                cd ..

                commit=$(git ls-remote ${GIT_NETAPP_URL}.git | grep $GIT_NETAPP_BRANCH | awk '{ print $1}')
                python3 utils/report_generator.py --template templates/${STAGE}/step-final-report-summary.md.j2 --json executive_summary/report-steps-"$NETAPP_NAME_LOWER".json --output executive_summary/report-steps-$NETAPP_NAME_LOWER.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $VERSION_NETAPP --url url --name $NETAPP_NAME
                docker run --rm -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/executive_summary/report-steps-$NETAPP_NAME_LOWER.pdf $DOCKER_PATH/executive_summary/report-steps-$NETAPP_NAME_LOWER.md
                python3 utils/report_generator.py --template templates/${STAGE}/step-use-of-5g-apis.md.j2 --json executive_summary/report-steps-"$NETAPP_NAME_LOWER".json --output ${USE_5G_APIS_REPORT_FILENAME}.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $VERSION_NETAPP --url url --name $NETAPP_NAME
                docker run --rm -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/${USE_5G_APIS_REPORT_FILENAME}.pdf $DOCKER_PATH/${USE_5G_APIS_REPORT_FILENAME}.md
                '''
                }
            }
        }
        stage('Download jsons report to Artifactory') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    sudo apt update || echo "Error updating system references"
                    sudo apt install -y poppler-utils pdftk || echo "error installing Poppler utils and pdftk"
                    response=$(curl -s "http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/$STAGE/$NETAPP_NAME_LOWER/$BUILD_ID" -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep ".pdf" | tr -d '"' )
                    artifacts=($response)

                    for x in "${artifacts[@]}"
                    do
                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$x"
                        curl -u $PASSWORD_ARTIFACTORY $url -o $x
                    done

                    [ -e final_report.pdf ] && rm final_report.pdf || echo "No previous final report generated"
                    today=$(date +'%d/%m/%Y')
                    [ -e *-licenses*.pdf ] && mv *-licenses*.pdf executive_summary/ || echo "No licenses file found"
                    pdfunite *.pdf mid_report1.pdf
                    [ -e executive_summary/*-licenses*.pdf ] && pdfunite mid_report1.pdf executive_summary/*-licenses*.pdf mid_report.pdf || pdfunite mid_report1.pdf mid_report.pdf

                    pip install -r utils/requirements.txt
                    python3 utils/cover.py -t "$NETAPP_NAME" -d $today -s $STAGE

                    

                    # Remember install PDFTK for watermarking
                    pdftk mid_report.pdf multistamp utils/watermark.pdf output mid_report_watermark.pdf
                    pdftk executive_summary/report-steps-$NETAPP_NAME_LOWER.pdf multistamp utils/watermark.pdf output executive_summary/report-steps-$NETAPP_NAME_LOWER_watermark.pdf
                    
                    
                    if [ "$STAGE" == "certification" ]
                    then
                        echo "Adding fingerprint"
                        FINGERPRINT=$(jq -r .fingerprint.certificationid executive_summary/report-steps-"$NETAPP_NAME_LOWER".json)
                        VERSION=$(jq -r .fingerprint.Version executive_summary/report-steps-"$NETAPP_NAME_LOWER".json)
                        python3 utils/fingerprint/fingerprint.py -f $FINGERPRINT -n $NETAPP_NAME -v $VERSION
                        pdftk fingerprint.pdf multistamp utils/watermark.pdf output fingerprint_watermark.pdf
                        pdfunite cover.pdf executive_summary/report-steps-$NETAPP_NAME_LOWER_watermark.pdf mid_report_watermark.pdf fingerprint_watermark.pdf utils/endpage.pdf final_report.pdf
                    else
                        echo "Only Certification stage will include fingerprint"
                        pdfunite cover.pdf executive_summary/report-steps-$NETAPP_NAME_LOWER_watermark.pdf mid_report_watermark.pdf utils/endpage.pdf final_report.pdf
                    fi

                    '''
                }
            }
        }

        stage('Upload documents to Artifactory') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                    report_file="final_report.pdf"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/attachments/$report_file"

                    curl -v -f -i -X PUT -u $PASSWORD_ARTIFACTORY \
                        --data-binary @"$report_file" \
                        "$url"
                    '''
                }
            }
        }
    }
    post {
        always {
            script {
                sh '''
                docker image prune -a -f
                '''
            }
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                    mimeType: 'text/html',
                    subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                    from: 'jenkins-evolved5G@tid.es',
                    replyTo: 'jenkins-evolved5G',
                    recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
                }
            }
        }
        cleanup {
            /* clean up our workspace */
            deleteDir()
            /* clean up tmp directory */
            dir("${env.workspace}@tmp") {
                deleteDir()
            }
            /* clean up script directory */
            dir("${env.workspace}@script") {
                deleteDir()
            }
        }
    }
}
