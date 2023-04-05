String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

def getPath(deployment) {
    String var = deployment
    if ('verification'.equals(var)) {
        return ''
    }else if ('validation'.equals(var)) {
        return 'validation'
    }else {
        return 'certification'
    }
}

def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    }else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    }else {
        return 'evol5-slave'
    }
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma'])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY = credentials('artifactory_credentials')
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        TOKEN = credentials('github_token_cred')
        TOKEN_TRIVY = credentials('token_trivy')
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        STAGE = getPath("${params.STAGE}")
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        DOCKER_PATH = '/usr/src/app'
        REPORT_FILENAME = '005-report-tr-img'
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
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
        stage('Launch Github Actions command') {
            options {
                    timeout(time: 30, unit: 'MINUTES')
                    retry(3)
            }
            steps {
                dir("${env.WORKSPACE}/") {
                    sh 'printenv'
                    sh '''#!/bin/bash

                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/${STAGE}/${NETAPP_NAME_LOWER} -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*" | tr -d '"' )
                    images=($response)

                    for x in "${images[@]}"
                    do
                        curl -s -H "Content-Type: application/json" -X POST "http://epg-trivy.hi.inet:5000/v1/scan-image?token=$TOKEN_TRIVY&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=markdown&image=dockerhub.hi.inet/evolved-5g/$STAGE/$NETAPP_NAME_LOWER/$x"
                        curl -s -H "Content-Type: application/json" -X POST "http://epg-trivy.hi.inet:5000/v1/scan-image?token=$TOKEN_TRIVY&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=json&image=dockerhub.hi.inet/evolved-5g/$STAGE/$NETAPP_NAME_LOWER/$x" > ${REPORT_FILENAME}-$x.json
                    done
                    '''
                }
            }
        }
        stage('Get wiki repo and update Evolved Wiki') {
            options {
                timeout(time: 10, unit: 'MINUTES')
                retry(1)
            }
            steps {
                dir("${env.WORKSPACE}/") {
                    sh '''
                   git clone https://$TOKEN@github.com/Telefonica/Evolved5g-${NETAPP_NAME}.wiki.git
                   git clone $GIT_NETAPP_URL.wiki.git
                   cp -R Evolved5g-${NETAPP_NAME}.wiki/* ${NETAPP_NAME}.wiki/
                   cd ${NETAPP_NAME}.wiki/
                   git add -A .
                   git config user.email "evolved5g@gmail.com"
                   git config user.name "Evolved5G"
                   git diff-index --quiet HEAD || git commit -m 'Addig Trivy report'
                   git push  https://$TOKEN_EVOLVED@github.com/EVOLVED-5G/$NETAPP_NAME.wiki.git
                   '''
                }
            }
        }

        stage('Upload report to Artifactory') {
            when {
                expression {
                    return REPORTING
                }
            }
            options {
                retry(2)
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/ -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*" | tr -d '"' )
                    versionT=0.35.0
                    declare -a files=("json" "md" "pdf")
                    images=($response)
                    for x in "${images[@]}"
                    do
                        urlT=https://github.com/EVOLVED-5G/$NETAPP_NAME/wiki/dockerhub.hi.inet-evolved-5g-$STAGE-$NETAPP_NAME_LOWER-$x
                        python3 utils/report_generator.py --template templates/scan-image.md.j2 --json ${REPORT_FILENAME}-$x.json --output ${REPORT_FILENAME}-$x.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit commit --version $versionT --url $urlT

                        docker run -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/${REPORT_FILENAME}-$x.pdf $DOCKER_PATH/${REPORT_FILENAME}-$x.md || exit 1

                        # Check to see if the image has succesfully passed all tests
                        if grep -q "failed" ${REPORT_FILENAME}-$x.md; then
                            result=false
                        else
                            result=true
                        fi
                        if  $result ; then
                            echo "Scan secrets was completed succesfuly"
                        else
                            exit 1
                        fi

                        for y in "${files[@]}"
                        do
                            report_file="${REPORT_FILENAME}-$x.$y"
                            url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                            curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                --data-binary @"$report_file" \
                                "$url"
                        done
                    done
                    '''
                }
            }
        }
    }
    post {
        always {
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: 'jenkins-evolved5G@tid.es',
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
