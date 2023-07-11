/*
NTH: Create library with helper methods
*/
String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

def getAgent(deployment) {
    // String var = deployment
    // if ('openshift'.equals(var)) {
    //     return 'evol5-openshift'
    // }else if ('kubernetes-athens'.equals(var)) {
    //     return 'evol5-athens'
    // }else {
    //     return 'evol5-slave'
    // }
    return 'evol5-slave2'
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma'])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        SCANNERHOME = tool 'Sonar Scanner 5'
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        SQ_TOKEN = credentials('SONARQUBE_TOKEN')
        SONARQB_PASSWORD = credentials('SONARQB_PASSWORD')
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        ARTIFACTORY_URL = 'http://artifactory.hi.inet/artifactory/misc-evolved5g/validation'
        STAGE = ${params.STAGE}
        DOCKER_PATH = '/usr/src/app'
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
        REPORT_FILENAME = '000-report-sonarqube'
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
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME_LOWER
                    mkdir $NETAPP_NAME_LOWER
                    cd $NETAPP_NAME_LOWER
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .

                    '''
                }
            }
        }

        //TODO: Create a project for each NETAPP
        stage('SonarQube Analysis and Wait for Quality Gate') {
            steps {
                dir("${WORKSPACE}/") {
                    withSonarQubeEnv('Evol5-SonarQube') {
                        sh '''
                            ${SCANNERHOME}/bin/sonar-scanner -X \
                                -Dsonar.projectKey=Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH} \
                                -Dsonar.projectBaseDir="${WORKSPACE}/${NETAPP_NAME_LOWER}/" \
                                -Dsonar.sources="${WORKSPACE}/${NETAPP_NAME_LOWER}/src/" \
                                -Dsonar.host.url=https://sq.mobilesandbox.cloud:9000 \
                                -Dsonar.login=$SQ_TOKEN \
                                -Dsonar.projectName=Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH} \
                                -Dsonar.language=python \
                                -Dsonar.sourceEncoding=UTF-8
                        '''
                    }
                }
            }
        }

        stage('Get SonarQube Report') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    //TODO: IMPROVE WAIT FOR REPORT READY
                    //qualityGateStatus=true provokes an error
                    sh '''
                    sleep 15
                    sonar-report \
                        --sonarurl="https://sq.mobilesandbox.cloud:9000" \
                        --sonartoken="$SQ_TOKEN" \
                        --qualityGateStatus="false" \
                        --sonarcomponent="Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}" \
                        --project="Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}" \
                        --application="Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}" \
                        --sinceleakperiod="false" \
                        --allbugs="true" \
                        --noRulesInReport= "true" \
                        --saveReportJson "${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.json" > ${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.html
                    '''
                }
            }
        }

        stage('Generate Markdown report and Upload reports to Artifactory') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#! /bin/bash

                    # get Commit Information
                    cd $NETAPP_NAME_LOWER
                    commit=$(git rev-parse HEAD)
                    cd ..
                    versionsq=$(curl -u admin:$SONARQB_PASSWORD https://sq.mobilesandbox.cloud:9000/api/system/info | jq ".System.Version")
                    urlsq=https://sq.mobilesandbox.cloud:9000/dashboard?id=Evolved5g-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}

                    python3 utils/report_generator.py --template templates/${STAGE}/step-source-code-static-analysis.md.j2 --json ${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.json --output ${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $versionsq --url $urlsq --name $NETAPP_NAME
                    docker run -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.pdf $DOCKER_PATH/${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.md
                    declare -a files=("json" "html" "md" "pdf")

                    for x in "${files[@]}"
                    do
                        report_file="${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.$x"
                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                        curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                            --data-binary @"$report_file" \
                            "$url"
                    done
                    '''
                }
            }
        }
        stage('Check stage status') {
            when {
                expression {
                    return REPORTING
                }
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    if grep -q "failed" ${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.md; then
                        result=false
                    else
                        result=true
                    fi
                    if  $result ; then
                        echo "SonarQube Scan was completed succesfuly"
                    else
                        exit 1
                    fi
                    '''
                }
            }
        }
    }
    post {
        always {
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext attachmentsPattern: '**/${REPORT_FILENAME}-${NETAPP_NAME_LOWER}-${GIT_NETAPP_BRANCH}.html',
                    body: '''${SCRIPT, template="groovy-html.template"}''',
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
