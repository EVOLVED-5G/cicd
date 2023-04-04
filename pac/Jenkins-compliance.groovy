String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

def getAgent(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-openshift";
    }else if("kubernetes-athens".equals(var)){
        return "evol5-athens"
    }else {
        return "evol5-slave";
    }
}

def getReportFilename(String netappNameLower) {
    return '003-report-licenses-repo-' + netappNameLower
}

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])
        booleanParam(name: 'REPORTING', defaultValue: true, description: 'Save report into artifactory')
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY= credentials("artifactory_credentials")
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        TOKEN = credentials('github_token_cred')
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        TOKEN_TRIVY = credentials('token_trivy')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
        DOCKER_PATH="/usr/src/app"
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
    }

    stages {

        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
            steps {
                sh '''
                rm -rf ${NETAPP_NAME}
                mkdir ${NETAPP_NAME}
                cd ${NETAPP_NAME}
                git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
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
                debricked "${WORKSPACE}/${NETAPP_NAME}" debricked:scan "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" ${NETAPP_NAME} "$GIT_COMMIT" null cli > scan_vul_${NETAPP_NAME}_"$GIT_COMMIT".report
                cat scan_vul_${NETAPP_NAME}_"$GIT_COMMIT".report
                UPLOAD_ID=$(grep "Checking scan status of upload with ID" scan_vul_${NETAPP_NAME}_$GIT_COMMIT.report | sed 's/[^0-9]*//g')
                debricked "${WORKSPACE}/${NETAPP_NAME}" debricked:license-report  "$DEBRICKED_CREDENTIALS_USR" "$DEBRICKED_CREDENTIALS_PSW" "$UPLOAD_ID" > compliance_${NETAPP_NAME}_"$GIT_COMMIT".report
                tail -1 compliance_${NETAPP_NAME}_"$GIT_COMMIT".report > compliance_${NETAPP_NAME}_"$GIT_COMMIT".report.json
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
                        cd $NETAPP_NAME
                        commit=$(git rev-parse HEAD)
                        cd ..

                        python3 utils/report_debricked_generator.py --template templates/scan-licenses.md.j2 --json ${WORKSPACE}/${NETAPP_NAME}/compliance_${NETAPP_NAME}_"$GIT_COMMIT".report.json --output ${REPORT_FILENAME}.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit
                        docker build  -t pdf_generator utils/docker_generate_pdf/.
                        docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/${REPORT_FILENAME}.pdf $DOCKER_PATH/${REPORT_FILENAME}.md
                        declare -a files=("md" "pdf")

                        for x in "${files[@]}"
                            do
                                report_file="${REPORT_FILENAME}.$x"
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