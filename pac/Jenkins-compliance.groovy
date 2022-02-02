pipeline {
    agent { node {label 'evol5-slave'}  }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: '')
        string(name: 'GIT_BRANCH', defaultValue: 'main', description: '')
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'NETAPP_NAME', defaultValue: 'dummyapp', description: '')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        GIT_COMMIT="${params.GIT_COMMIT}"
    }

    stages {

        stage('Get the code!') {
            steps {
                sh '''
                rm -rf ${NETAPP_NAME}
                mkdir ${NETAPP_NAME}
                cd ${NETAPP_NAME}
                git clone --single-branch --branch $GIT_BRANCH $GIT_URL .
                '''
            }
        }

        stage('Vulnerability scan') {
            environment {
                DEBRICKED_CREDENTIALS = credentials('debricked-creds')
            }

            agent {
                docker {
                    image 'debricked/debricked-cli'
                    args '--entrypoint="" -v ${WORKSPACE}/${NETAPP_NAME}:/data -w /data'
                }
            }
            steps {
                sh 'bash /home/entrypoint.sh debricked:scan "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" ${NETAPP_NAME} "$GIT_COMMIT" null cli'
            }
        }
    }
}