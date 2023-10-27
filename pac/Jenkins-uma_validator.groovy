String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

String getPathAWS(deployment) {
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
    } else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'evol5-cosmote'
    } else {
        return 'evol5-slave'
    }
}

def getElcmUrl(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return ' '
    } else if ('kubernetes-athens'.equals(var)) {
        return 'http://10.220.2.237:8080/elcm/api/v1/'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'http://10.220.2.237:8080/elcm/api/v1/'
    } else {
        return 'http://10.11.23.220:5551/elcm/api/v1'
    }
}

def getAnalyticsUrl(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return ' '
    } else if ('kubernetes-athens'.equals(var)) {
        return 'http://10.220.2.238:5003'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'http://10.220.2.238:5003'
    } else {
        return 'http://10.11.23.220:5003'
    }
}

def getReportFilename(String netappNameLower) {
    return '000-report-platform-assesment-' + netappNameLower
}

String getArtifactoryUrl(phase) {
    return 'http://artifactory.hi.inet/artifactory/misc-evolved5g/' + phase
}


String getHost(String url) {
    String host = url.split('/')[2].split(':')[0]
    return host
}

def valid_slaves = ['kubernetes-athens','kubernetes-cosmote','kubernetes-uma']

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        retry(1)
    }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: 'Version of NetworkApp')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote', 'openshift'])
        string(name: 'ELCM_URL', defaultValue: 'http://10.11.23.220:5551/elcm/api/v1', description: 'URL to ELCM')
        string(name: 'ANALYTICS_URL', defaultValue: 'http://10.11.23.220:5003', description: 'URL to Analytics')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        VERSION = "${params.VERSION}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        STAGE = "${params.STAGE}"
        PATH_AWS = getPathAWS("${params.STAGE}")
        CHECKPORTS_PATH = 'utils/checkports'
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        DOCKER_PATH = '/usr/src/app'
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
        ENVIRONMENT = "${params.DEPLOYMENT}"
        ELCM_URL = getElcmUrl("${params.DEPLOYMENT}")
        ANALYTICS_URL = getAnalyticsUrl("${params.DEPLOYMENT}")
        ELCM_HOST = getHost(ELCM_URL)
        ANALYTICS_HOST = getHost(ANALYTICS_URL)
    }

    stages {
        stage('Certification Stage') {
            steps {
                script {
                    if( "${STAGE}" != 'certification' ) {
                        currentBuild.result = 'ABORTED'
                        error("This job will be only executed on Certification Stage.")
                        return
                    }
                    if( "${DEPLOYMENT}" in valid_slaves) {
                        currentBuild.result = 'ABORTED'
                        error("This job can be only executed on UMA Stage.")
                        return
                    }
                }
            }
        }
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
        stage('Check the connectivity with UMA ECLM') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    ping -c 3 ${ELCM_HOST}
                    ping -c 3 ${ANALYTICS_HOST}
                    '''
                }
            }
        }

        stage('Execute the Experiment in the platform') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    pip3 install -r utils/platform_assesment/requirements.txt
                    python3 utils/platform_assesment/platform_assesment.py ${ELCM_URL} ${ANALYTICS_URL} ${REPORT_FILENAME}.json ${ENVIRONMENT}
                    '''
                }
            }
        }
    }

    post {
        always {
            retry(2) {
                script {
                    dir ("${env.WORKSPACE}") {
                        if ("${params.REPORTING}".toBoolean() == true) {
                            sh '''#!/bin/bash
                            if [ -f "${REPORT_FILENAME}.json" ]; then
                                echo "$FILE exists."

                                urlT=https://github.com/EVOLVED-5G/$NETAPP_NAME_LOWER/wiki/Telefonica-Evolved5g-$NETAPP_NAME_LOWER
                                versionT=${VERSION}

                                python3 utils/report_generator.py --template templates/${STAGE}/step-platform-assessment.md.j2 --json ${REPORT_FILENAME}.json --output $REPORT_FILENAME.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $versionT --url $urlT --name $NETAPP_NAME --logs ${NETAPP_NAME_LOWER}-build-runtime_error.log
                                docker run --rm -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/$REPORT_FILENAME.pdf $DOCKER_PATH/$REPORT_FILENAME.md
                                declare -a files=("json" "md" "pdf")

                                for x in "${files[@]}"
                                    do
                                        report_file="${REPORT_FILENAME}.$x"
                                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                                        curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                            --data-binary @"$report_file" \
                                            "$url"
                                    done
                            else
                                echo "No report file generated"
                            fi
                            '''
                        }
                    }
                }
            }
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a -f --volumes
            '''
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