package pipelines;

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

pipeline{

    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }

    options {
        disableConcurrentBuilds()
        buildDiscarder(logRotator(daysToKeepStr: '14', numToKeepStr: '30', artifactDaysToKeepStr: '14', artifactNumToKeepStr: '30'))
        ansiColor('xterm')
        timeout(time: 60, unit: 'MINUTES')
        retry(1)
    }

    parameters{
        choice(name: 'DEPLOYMENT', choices: ['kubernetes-uma', 'kubernetes-athens', 'kubernetes-cosmote', 'openshift'], description: 'Environment where tests will run')
        string(name: 'ROBOT_DOCKER_IMAGE_VERSION', defaultValue: '3.1.1', description: 'Robot Docker image version')
        string(name: 'CAPIF_HOST', defaultValue: 'capifcore', description: 'CAPIF host')
        string(name: 'CAPIF_HTTP_PORT', defaultValue: '8080', description: 'CAPIF http port')
        string(name: 'CAPIF_HTTPS_PORT', defaultValue: '443', description: 'CAPIF https port')
        string(name: 'NEF_API_HOSTNAME', defaultValue: 'https://localhost:4443', description: 'NEF Emulator api hostname')
        string(name: 'ADMIN_USER', defaultValue: 'admin@my-email.com', description: 'NEF Admin username')
        password(name: 'ADMIN_PASS', defaultValue: 'pass', description: 'NEF Admin password')
        // Next variables are only useful on validation and certification, in order to upload information to artifactory
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        string(name: 'VERSION', defaultValue: '1.0', description: 'Version of NetworkApp')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
    }

    environment {
        NEF_REPOSITORY_DIRECTORY = "${WORKSPACE}/nef_validation"
        ROBOT_TESTS_DIRECTORY = "${NEF_REPOSITORY_DIRECTORY}/tests"
        ROBOT_RESULTS_DIRECTORY = "${WORKSPACE}/results"
        NGINX_HOSTNAME = "${params.NEF_API_HOSTNAME}"
        ROBOT_VERSION = "${params.ROBOT_DOCKER_IMAGE_VERSION}"
        ROBOT_IMAGE_NAME = 'dockerhub.hi.inet/dummy-netapp-testing/robot-test-image'
        DEPLOYMENT = "${params.DEPLOYMENT}"
        STAGE = "${params.STAGE}"
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        VERSION = "${params.VERSION}"
        NEF_VALIDATION_GITHUB_URL = "https://github.com/EVOLVED-5G/NEF-Validation"
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
    }

    stages{
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
                    rm -rf "$NEF_REPOSITORY_DIRECTORY"
                    mkdir "$NEF_REPOSITORY_DIRECTORY"
                    cd "$NEF_REPOSITORY_DIRECTORY"
                    git clone --single-branch --branch capif $NEF_VALIDATION_GITHUB_URL .
                    '''
                }
            }
        }
        stage ("Setup Robot FW && Run tests"){
            stages{
                stage("Substitute registration values"){
                    steps{
                        dir ("${NEF_REPOSITORY_DIRECTORY}/tools/capif-registration/") {
                            sh '''
                                sed -i "s/CAPIF_HOST/${CAPIF_HOST}/g" capif-registration.json
                                sed -i "s/CAPIF_HTTP_PORT/${CAPIF_HTTP_PORT}/g" capif-registration.json
                                sed -i "s/CAPIF_HTTPS_PORT/${CAPIF_HTTPS_PORT}/g" capif-registration.json
                                sed -i "s/CAPIF_HOST/${CAPIF_HOST}/g" register.sh
                                sed -i "s/CAPIF_HTTP_PORT/${CAPIF_HTTP_PORT}/g" register.sh
                            '''
                        }
                    }
                }
                stage("Setup RobotFramwork container"){
                    steps{
                        script{
                            dir ("${WORKSPACE}") {
                                sh '''
                                mkdir -p "${ROBOT_RESULTS_DIRECTORY}"
                                docker run --rm -d -t \
                                    --name robot \
                                    --network="host" \
                                    -v "${NEF_REPOSITORY_DIRECTORY}"/tests:/opt/robot-tests/tests/ \
                                    -v "${NEF_REPOSITORY_DIRECTORY}"/libraries:/opt/robot-tests/libraries/ \
                                    -v "${NEF_REPOSITORY_DIRECTORY}"/resources:/opt/robot-tests/resources/ \
                                    -v "${ROBOT_RESULTS_DIRECTORY}":/opt/robot-tests/results/ \
                                    -v "${NEF_REPOSITORY_DIRECTORY}"/tools/capif-registration:/opt/robot-tests/capif-registration \
                                    --env NEF_URL=${NGINX_HOSTNAME} \
                                    --env BUILD_NUMBER=$BUILD_NUMBER \
                                    --env NGINX_HOSTNAME=${NGINX_HOSTNAME} \
                                    --env ADMIN_USER=${ADMIN_USER} \
                                    --env ADMIN_PASS=$ADMIN_PASS \
                                    --env CERTS_PATH=/opt/robot-tests/capif-registration/capif_onboarding \
                                    --env CAPIF_HOST=${CAPIF_HOST}:${CAPIF_HTTPS_PORT}  \
                                    --env CAPIF_HTTPS_PORT=${CAPIF_HTTPS_PORT} \
                                    ${ROBOT_IMAGE_NAME}:${ROBOT_VERSION}
                                sudo chown contint:contint -R nef_validation
                                '''
                            }
                        }
                    }
                }
                stage("Register to Capif"){
                    steps{
                        sh '''
                            docker exec -t robot bash /opt/robot-tests/capif-registration/register.sh
                        '''
                    }
                }
                stage("Run test cases."){
                    steps{
                        sh '''
                            docker exec -t robot bash \
                            -c "pabot --processes 1 --outputdir /opt/robot-tests/results/ /opt/robot-tests/tests/; \
                                rebot --outputdir /opt/robot-tests/results --output output.xml --merge /opt/robot-tests/results/output.xml;"
                        '''
                    }
                }
            }
        }
    }

    post{
        always{
            script {
                catchError(buildResult: 'SUCCESS', stageResult: 'SUCCESS'){
                    sh '''
                        docker kill robot
                    '''
                }
                script {
                    /* Manually clean up /keys due to permissions failure */
                    echo 'Robot test executed'
                    echo ' clean dockerhub credentials'
                    sh 'sudo rm -f ${HOME}/.docker/config.json'
                }
            }
            publishHTML([allowMissing: true,
                    alwaysLinkToLastBuild: false,
                    keepAll: true,
                    reportDir: 'results',
                    reportFiles: 'report.html',
                    reportName: 'Robot Framework Tests Report NEF',
                    reportTitles: '',
                    includes:'**/*'])
            junit allowEmptyResults: true, testResults: 'results/xunit.xml'
            script {
                dir("${env.ROBOT_RESULTS_DIRECTORY}") {
                    sh '''
                    #!/bin/bash

                    results_file="NEF_robot_tests.tar.gz"
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
                echo "Deleting directories."
                cleanWs deleteDirs: true
            }
            echo "Done."
        }
        success{
            echo "Test ran successfully."
        }
    }

}