pipeline {
    agent { node {label 'evol5-slave'}  }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        GIT_COMMIT="${params.GIT_COMMIT}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
        PASSWORD_ARTIFACTORY= credentials("artifactory_credentials")
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
                cd "${WORKSPACE}/${NETAPP_NAME}"
                debricked-scan ${WORKSPACE}/${NETAPP_NAME} debricked:scan "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" ${NETAPP_NAME} "$GIT_COMMIT" null cli > scan_vul_${NETAPP_NAME}_"$GIT_COMMIT".report
                debricked-license debricked:license-report  "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" "$UPLOAD_ID" > compliance_${NETAPP_NAME}_"$GIT_COMMIT".report
                '''
            }
        }

        stage('Upload report to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                        # get Commit Information
                        cd "${WORKSPACE}/${NETAPP_NAME}"
                        GIT_COMMIT=$(git log --format="%H" -n 1)

                        declare -a files=("report")

                        for x in "${files[@]}"
                            do
                                report_file="report-compliance-repo-$NETAPP_NAME_LOWER.$x"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                    --data-binary @"$report_file" \
                                    "$url"
                            done
                    '''
                }
            }
        }
    }

    // post {
    //     unsuccessful {
    //         echo "Sending Report!"
    //         emailext body: '''${SCRIPT, template="groovy-html.template"}''',
    //             mimeType: 'text/html',
    //             subject: "Evolved 5G - Compliance Analysis Result ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
    //             from: 'pro-dcip-evol5-01@tid.es',
    //             to: "evolved5g.devops@telefonica.com",
    //             replyTo: "jenkins-evolved5G",
    //             compressLog: true,
    //             attachLog: true
    //     }
    //     success {
    //         echo "Sending Report!"
    //         emailext attachmentsPattern: '**/*.report.txt',
    //             body: '''${SCRIPT, template="groovy-html.template"}''',
    //             mimeType: 'text/html',
    //             subject: "Evolved 5G - ${NETAPP_NAME} - Compliance Analysis Result ${currentBuild.currentResult}",
    //             from: 'pro-dcip-evol5-01@tid.es',
    //             to: "evolved5g.devops@telefonica.com",
    //             replyTo: "jenkins-evolved5G",
    //             compressLog: true,
    //             attachLog: true
    //     }
    //     cleanup{
    //         /* clean up our workspace */
    //         deleteDir()
    //         /* clean up tmp directory */
    //         dir("${env.workspace}@tmp") {
    //             deleteDir()
    //         }
    //         /* clean up script directory */
    //         dir("${env.workspace}@script") {
    //             deleteDir()
    //         }
    //     }
    // }
}