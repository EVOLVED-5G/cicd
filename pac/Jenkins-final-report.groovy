String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'Netapp branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY= credentials("artifactory_credentials")
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        TOKEN = credentials('github_token_cred')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
        DOCKER_PATH="/usr/src/app"
    }

    stages {

        stage('Generate executive summary') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
        
                    mkdir executive_summary
                    cd executive_summary

                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/validation/$NETAPP_NAME_LOWER/$BUILD_ID -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep ".json" | tr -d '"' )
                    
                    
                    artifacts=($response)

                    for x in "${artifacts[@]}"
                    do  
                        url="http://artifactory.hi.inet:80/artifactory/misc-evolved5g/validation/$NETAPP_NAME_LOWER/$BUILD_ID/$x"
                        curl -u $PASSWORD_ARTIFACTORY $url -o $x
                    done
                    
                    cd ..
                    python3 utils/report_generator.py --template templates/scan-steps.md.j2 --json executive_summary/report-steps-"$NETAPP_NAME_LOWER".json --output executive_summary/report-steps-$NETAPP_NAME_LOWER.md
                    cd executive_summary
                    rm -f report-steps-"$NETAPP_NAME_LOWER".json

                    jq -s . *.json > final_json.json
                    jq '{"json": .}' < final_json.json  > report.json

                    cd ..
                    python3 utils/report_generator.py --template templates/scan-report.md.j2 --json executive_summary/report.json --output executive_summary/executive-summary-$NETAPP_NAME_LOWER.md

                    docker build  -t pdf_generator utils/docker_generate_pdf/.
                    docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/executive_summary/executive-summary-$NETAPP_NAME_LOWER.pdf $DOCKER_PATH/executive_summary/executive-summary-$NETAPP_NAME_LOWER.md
                    docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/executive_summary/report-steps-$NETAPP_NAME_LOWER.pdf $DOCKER_PATH/executive_summary/report-steps-$NETAPP_NAME_LOWER.md
                    '''
                }
            }
        }
        stage('Download jsons report to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/validation/$NETAPP_NAME_LOWER/$BUILD_ID -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep ".pdf" | tr -d '"' )
                    artifacts=($response)

                    for x in "${artifacts[@]}"
                    do  
                        url="http://artifactory.hi.inet:80/artifactory/misc-evolved5g/validation/$NETAPP_NAME_LOWER/$BUILD_ID/$x"
                        curl -u $PASSWORD_ARTIFACTORY $url -o $x
                    done
                    
                    today=$(date +'%d/%m/%Y')
                    pdfunite *.pdf mid_report.pdf
                    python3 utils/cover.py -t "$NETAPP_NAME_LOWER VALIDATION REPORT" -d $today -b $BUILD_ID
                    pdfunite cover.pdf executive_summary/executive-summary-$NETAPP_NAME_LOWER.pdf executive_summary/report-steps-$NETAPP_NAME_LOWER.pdf mid_report.pdf utils/endpage.pdf final_report.pdf

                    '''
                }
            }
        }

        stage('Upload documents to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                    report_file="final_report.pdf"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                    curl -v -f -i -X PUT -u $PASSWORD_ARTIFACTORY \
                        --data-binary @"$report_file" \
                        "$url"
                    '''
                }
            }
        }
    }
    post {
        always {
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: "jenkins-evolved5G",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        }
        cleanup{
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