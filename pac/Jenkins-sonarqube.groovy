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

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        // GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        // GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        // GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        SCANNERHOME = tool 'Sonar Scanner 5';
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
        SQ_TOKEN=credentials('SONARQUBE_TOKEN')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {
        stage('Get the code!') {
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
                        --saveReportJson "sonar-report_Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.json" > sonar-report_Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.html
                    '''
                }
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
                    sh '''#! /bin/bash
                    declare -a files=("json" "html")

                    for x in "${files[@]}"
                    do

                        report_file="sonar-report_Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.$x"
                        url="$ARTIFACTORY_URL/$NETAPP_NAME/$report_file"

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
                replyTo: "no-reply@tid.es",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        }
    }
}
