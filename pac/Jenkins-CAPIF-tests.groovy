// ################################################
// ## Robot Funtions
// ################################################
// Parse JSON to a map. @param json Json string to parse with name / value properties. @return A map of properties

String setRobotOptionsValue(String options) {
    return (options && options != '') ? options : ' '
}

String robotDockerVersion(String options) {
    return (options) ? options : 'latest'
}

String robotTestSelection(String tests, String customTest) {
    if (tests == 'CUSTOM') {
        return (customTest) ? '--include ' + customTest : ' '
    }
    return tests == 'NONE' ? ' ' : '--include ' + test_plan[tests]
}

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

test_plan = [
    'All Capif Services': 'all',
    'CAPIF Api Invoker Management': 'capif_api_invoker_management',
    'CAPIF Api Publish Service': 'capif_api_publish_service',
    'CAPIF Api Discover Service': 'capif_api_discover_service',
    'CAPIF Api Events': 'capif_api_events',
    'CAPIF Security Api': 'capif_security_api',
    'CUSTOM': 'CUSTOM'
    ]

// ################################################
// ## Pipeline
// ################################################

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '30', artifactDaysToKeepStr: '14', artifactNumToKeepStr: '30'))
        ansiColor('xterm')
        timeout(time: 60, unit: 'MINUTES')
        retry(1)
    }
    parameters {
        string(name: 'BRANCH_NAME', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'CAPIF_HOSTNAME', defaultValue: 'capifcore', description:'Nginx to forward requests')
        string(name: 'CAPIF_PORT', defaultValue: '8080', description:'Port of capif')
        string(name: 'CAPIF_TLS_PORT', defaultValue: '443', description:'Port of TLS capif')
        choice(name: 'TESTS', choices: test_plan.keySet() as ArrayList, description: 'Select option to run. Prefix')
        string(name: 'CUSTOM_TEST', defaultValue: '', description: 'If CUSTOM is set in TESTS, here you can add test tag')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '4.0', description: 'Robot Docker image version')
        string(name: 'ROBOT_TEST_OPTIONS', defaultValue: '', description: 'Options to set in test to robot testing. --variable <key>:<value>, --include <tag>, --exclude <tag>')
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote', 'openshift'], description: 'Environment where the CAPIF is tested')
        // Next variables are only useful on validation and certification, in order to upload information to artifactory
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        string(name: 'VERSION', defaultValue: '1.0', description: 'Version of NetworkApp')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
    }
    environment {
        BRANCH_NAME = "${params.BRANCH_NAME}"
        CAPIF_REPOSITORY_DIRECTORY = "${WORKSPACE}/capif_api_services"
        ROBOT_TESTS_DIRECTORY = "${CAPIF_REPOSITORY_DIRECTORY}/tests"
        ROBOT_RESULTS_DIRECTORY = "${WORKSPACE}/results"
        ROBOT_DOCKER_FILE_FOLDER = "${CAPIF_REPOSITORY_DIRECTORY}/tools/robot"
        CUSTOM_TEST = "${params.CUSTOM_TEST}"
        CAPIF_HOSTNAME = "${params.CAPIF_HOSTNAME}"
        CAPIF_PORT = "${params.CAPIF_PORT}"
        CAPIF_TLS_PORT = "${params.CAPIF_TLS_PORT}"
        ROBOT_TEST_OPTIONS = setRobotOptionsValue("${params.ROBOT_TEST_OPTIONS}")
        ROBOT_TESTS_INCLUDE = robotTestSelection("${params.TESTS}", "${params.CUSTOM_TEST}")
        ROBOT_VERSION = robotDockerVersion("${params.ROBOT_DOCKER_IMAGE_VERSION}")
        ROBOT_IMAGE_NAME = 'dockerhub.hi.inet/5ghacking/evolved-robot-test-image'
        DEPLOYMENT = "${params.DEPLOYMENT}"
        STAGE = "${params.STAGE}"
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        VERSION = "${params.VERSION}"
        CAPIF_GITHUB_URL = "https://github.com/EVOLVED-5G/CAPIF_API_Services"
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
    }
    stages {
        stage('Docker Login') {
            options {
                retry(10)
            }

            steps {
                dir("${env.WORKSPACE}") {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker_pull_cred',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                    script {
                        try {
                            sh '''
                            #!/bin/bash
                            docker login --username ${USER} --password ${PASS} dockerhub.hi.inet
                            '''
                    } catch (Exception e) {
                                echo 'Docker login has failed.'
                            }
                        }
                }
                }
            }
        }
        stage('Prepare robot docker image tool') {
            options {
                retry(10)
            }
            steps {
                dir("${env.WORKSPACE}") {
                    script {
                        try {
                            sh '''
                            #!/bin/bash
                            docker pull ${ROBOT_IMAGE_NAME}:${ROBOT_VERSION}
                            '''
                        } catch (Exception e) {
                            echo 'Robot Docker version is not currently uploaded to artifactory.'
                        }
                    }
                }
            }
        }
        stage('Prepare robot test suite') {
            options {
                timeout(time: 10, unit: 'MINUTES')
                retry(1)
            }
             steps {
                dir("${env.WORKSPACE}/") {
                    sh '''
                    #!/bin/bash
                    rm -rf "$CAPIF_REPOSITORY_DIRECTORY"
                    mkdir "$CAPIF_REPOSITORY_DIRECTORY"
                    cd "$CAPIF_REPOSITORY_DIRECTORY"
                    git clone --single-branch --branch $BRANCH_NAME $CAPIF_GITHUB_URL .
                    '''
                }
            }
        }

        stage('CAPIF: Launch tests') {
            steps {
                dir("${env.WORKSPACE}") {
                    sh '''
                    #!/bin/bash
                    echo "Executing tests in ${DEPLOYMENT}"
                    docker images|grep -Eq '^'$ROBOT_IMAGE_NAME'[ ]+[ ]'$ROBOT_VERSION''
                    if [[ $? -ne 0 ]]; then
                        echo "Building Robot docker image."
                        cd "${ROBOT_DOCKER_FILE_FOLDER}"
                        docker build  -t "${ROBOT_IMAGE_NAME}:${ROBOT_VERSION}" .
                        cd "${WORKSPACE}"
                    fi
                    mkdir -p "${ROBOT_RESULTS_DIRECTORY}"
                    docker run --tty --rm --network="host" \
                        -v "${ROBOT_TESTS_DIRECTORY}":/opt/robot-tests/tests \
                        -v "${ROBOT_RESULTS_DIRECTORY}":/opt/robot-tests/results \
                        "${ROBOT_IMAGE_NAME}":"${ROBOT_VERSION}"  \
                        --variable CAPIF_HOSTNAME:${CAPIF_HOSTNAME} \
                        --variable CAPIF_HTTP_PORT:${CAPIF_PORT} \
                        --variable CAPIF_HTTPS_PORT:${CAPIF_TLS_PORT} \
                        ${ROBOT_TESTS_INCLUDE} ${ROBOT_TEST_OPTIONS}

                    sudo chown contint:contint -R capif_api_services
                    '''
                }
            }
        }
    }
    post {
        always {
            script {
                /* Manually clean up /keys due to permissions failure */
                echo 'Robot test executed'
                echo ' clean dockerhub credentials'
                sh 'sudo rm -f ${HOME}/.docker/config.json'
            }

            publishHTML([allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'results',
                    reportFiles: 'report.html',
                    reportName: 'Robot Framework Tests Report',
                    reportTitles: '',
                    includes:'**/*'])
            junit allowEmptyResults: true, testResults: 'results/xunit.xml'

            script {
                dir("${env.ROBOT_RESULTS_DIRECTORY}") {
                    sh '''
                    #!/bin/bash

                    results_file="CAPIF_robot_tests.tar.gz"
                    rm output.xml
                    tar czvf "${results_file}" *

                    if [ -f "${results_file}" ]; then

                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/attachments/$results_file"

                        curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                            --data-binary @"${results_file}" \
                            "$url"
                    else
                        echo "compressed file was not generated"
                    fi

                    '''
                }
            }
            script {
                dir("${env.WORKSPACE}") {
                    sh "sudo rm -rf capif_api_services"
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
