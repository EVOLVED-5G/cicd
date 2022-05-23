/*
NTH: Create library with helper methods
*/
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        //string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        //VERSION="${params.VERSION}"
        //AWS_DEFAULT_REGION = 'eu-central-1'
        //AWS_ACCOUNT_ID = '709233559969'
        SCANNERHOME = tool 'Sonar Scanner 5';
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
    }

    stages {
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME
                    mkdir $NETAPP_NAME
                    cd $NETAPP_NAME
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    '''
                }
           }
        }

        //TODO: Create a project for each NETAPP
        stage('SonarQube Analysis') {
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''
                        sudo ${SCANNERHOME}/bin/sonar-scanner -X \
                            -Dsonar.projectKey=Evolved5g-master-${BUILD_NUMBER} \
                            -Dsonar.projectBaseDir=${WORKSPACE}/${NETAPP_NAME}/src/ \
                            -Dsonar.host.url=http://195.235.92.134:9000  \
                            -Dsonar.login=40f1332530d31e2372160616f6a458b82c5e429d \
                            -Dsonar.projectName=Evolved5g-master-${BUILD_NUMBER} \
                            -Dsonar.language=python \
                            -Dsonar.sourceEncoding=UTF-8 \
                    '''
                }
            }
        }

        stage("Quality Gate"){
            timeout(time: 10, unit: 'MINUTES') {
                def qg = waitForQualityGate()
                if (qg.status != 'OK') {
                    error "Pipeline aborted due to quality gate failure: ${qg.status}"
                    //unstable("There are Checkstyle issues")
                }
            }
        }

        // // Feature Flag para activar el salvado
        // stage('Save SonarQube Report into Artifactory') {
        //     steps {
        //          dir ("${env.WORKSPACE}/") {
        //             sh '''
        //                 sudo ${SCANNERHOME}/bin/sonar-scanner -X \
        //                     -Dsonar.projectKey=Evolved5g-master-${BUILD_NUMBER}\
        //                     -Dsonar.projectBaseDir=${env.WORKSPACE}/{NETAPP_NAME}/src/ \
        //                     -Dsonar.host.url=http://195.235.92.134:9000  \
        //                     -Dsonar.login=40f1332530d31e2372160616f6a458b82c5e429d \
        //                     -Dsonar.projectName=Evolved5g-master-${BUILD_NUMBER} \
        //                     -Dsonar.language=python \
        //                     -Dsonar.sourceEncoding=UTF-8 \
        //             '''
        //         }
        //     }
        // }
        // // Feature Flag para activar el salvado
        // stage('Semaphore') {
        //     steps {
        //          dir ("${env.WORKSPACE}/") {
        //             sh '''
        //                 sudo ${SCANNERHOME}/bin/sonar-scanner -X \
        //                     -Dsonar.projectKey=Evolved5g-master-${BUILD_NUMBER}\
        //                     -Dsonar.projectBaseDir=${env.WORKSPACE}/{NETAPP_NAME}/src/ \
        //                     -Dsonar.host.url=http://195.235.92.134:9000  \
        //                     -Dsonar.login=40f1332530d31e2372160616f6a458b82c5e429d \
        //                     -Dsonar.projectName=Evolved5g-master-${BUILD_NUMBER} \
        //                     -Dsonar.language=python \
        //                     -Dsonar.sourceEncoding=UTF-8 \
        //             '''
        //         }
        //     }
        // }

    }
    post {
        always {
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                to: "a.molina@telefonica.com",
                replyTo: "no-reply@tid.es",
                recipientProviders: [[$class: 'CulpritsRecipientProvider']]
        }
    }
}

