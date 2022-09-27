/*
NTH: Create library with helper methods
*/
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-slave2'}  }
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(2)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        SCANNERHOME = tool 'Sonar Scanner 5';
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
        SQ_TOKEN=credentials('SONARQUBE_TOKEN')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
        DOCKER_PATH="/usr/src/app"
    }

    stages {
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(2)
                }
            steps {
                dir ("${WORKSPACE}/") {
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
        stage('SonarQube Analysis and Wait for Quality Gate') {
            steps {
                 dir ("${WORKSPACE}/") {
                    withSonarQubeEnv('Evol5-SonarQube') {
                        sh '''
                            ${SCANNERHOME}/bin/sonar-scanner -X \
                                -Dsonar.projectKey=Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH} \
                                -Dsonar.projectBaseDir="${WORKSPACE}/${NETAPP_NAME}/" \
                                -Dsonar.sources="${WORKSPACE}/${NETAPP_NAME}/src/" \
                                -Dsonar.host.url=http://195.235.92.134:9000 \
                                -Dsonar.login=$SQ_TOKEN \
                                -Dsonar.projectName=Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH} \
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
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    //TODO: IMPROVE WAIT FOR REPORT READY
                    //qualityGateStatus=true provokes an error
                    sh '''
                    sleep 15
                    sonar-report \
                        --sonarurl="http://195.235.92.134:9000" \
                        --sonartoken="$SQ_TOKEN" \
                        --qualityGateStatus="false" \
                        --sonarcomponent="Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}" \
                        --project="Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}" \
                        --application="Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}" \
                        --sinceleakperiod="false" \
                        --allbugs="true" \
                        --noRulesInReport= "true" \
                        --saveReportJson "report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.json" > report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.html
                    '''
                }
            }
        }

        
        stage('Generate Markdown report and Upload reports to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#! /bin/bash

                    python3 utils/report_generator.py --template templates/scan-sonar.md.j2 --json report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.json --output report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.md
                    docker build  -t pdf_generator utils/docker_generate_pdf/.
                    docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.pdf $DOCKER_PATH/report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.md
                    declare -a files=("json" "html" "md" "pdf")

                    for x in "${files[@]}"
                    do

                        report_file="report-sonar-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.$x"
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
    post {
        always {
            emailext attachmentsPattern: '**/sonar-report_Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.html.txt',
                body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: "jenkins-evolved5G",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        }
    }
}
