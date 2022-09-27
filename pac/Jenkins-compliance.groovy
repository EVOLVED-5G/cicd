pipeline {
    agent { node {label 'evol5-slave'}  }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: '')
        string(name: 'GIT_BRANCH', defaultValue: 'main', description: '')
        string(name: 'NETAPP_NAME', defaultValue: 'dummyapp', description: '')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        GIT_COMMIT="${params.GIT_COMMIT}"
    }

    stages {

        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(2)
                }
            steps {
                sh '''
                rm -rf ${NETAPP_NAME}
                mkdir ${NETAPP_NAME}
                cd ${NETAPP_NAME}
                git clone --single-branch --branch $GIT_BRANCH $GIT_URL .
                '''
            }
        }

        stage('Vulnerability scan and license checking') {
            environment {
                DEBRICKED_CREDENTIALS = credentials('Debricked')
            }
            steps {
                sh '''
                cd ${WORKSPACE}/${NETAPP_NAME}
                GIT_COMMIT=$(git log --format="%H" -n 1)
                debricked-scan ${WORKSPACE}/${NETAPP_NAME} debricked:scan "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" ${NETAPP_NAME} "$GIT_COMMIT" null cli > scan_vul_${NETAPP_NAME}_"$GIT_COMMIT".report.txt
                cat scan_vul_${NETAPP_NAME}_"$GIT_COMMIT".report.txt
                UPLOAD_ID=$(grep "Checking scan status of upload with ID" scan_vul_${NETAPP_NAME}_$GIT_COMMIT.report.txt | sed 's/[^0-9]*//g')
                debricked-license debricked:license-report  "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" "$UPLOAD_ID" > compliance_${NETAPP_NAME}_"$GIT_COMMIT".report.txt
                '''
            }
        }
    }

    post {
        unsuccessful {
            echo "Sending Report!"
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Evolved 5G - Compliance Analysis Result ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'pro-dcip-evol5-01@tid.es',
                to: "evolved5g.devops@telefonica.com",
                replyTo: "jenkins-evolved5G",
                compressLog: true,
                attachLog: true
        }
        success {
            echo "Sending Report!"
            emailext attachmentsPattern: '**/*.report.txt',
                body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Evolved 5G - ${NETAPP_NAME} - Compliance Analysis Result ${currentBuild.currentResult}",
                from: 'pro-dcip-evol5-01@tid.es',
                to: "evolved5g.devops@telefonica.com",
                replyTo: "jenkins-evolved5G",
                compressLog: true,
                attachLog: true
        }
    }
}