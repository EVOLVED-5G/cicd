String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
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
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {

        stage('Launch Github Actions command') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''#!/bin/bash

                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/ -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*" | tr -d '"' )

                    images=($response)

                    for x in "${images[@]}"
                    do
                        curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-image?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&image=dockerhub.hi.inet/evolved-5g/$x&update_wiki=true&repository=Telefonica/Evolved5g-${NETAPP_NAME}&branch=${GIT_NETAPP_BRANCH}&output_format=md"
                        curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-image?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&image=dockerhub.hi.inet/evolved-5g/$x&update_wiki=false&repository=Telefonica/Evolved5g-${NETAPP_NAME}&branch=${GIT_NETAPP_BRANCH}&output_format=json" > report-tr-img-${NETAPP_NAME_LOWER}.json
                    done
                    '''
                }
            }
        }
        stage('Get wiki repo and update Evolved Wiki'){
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    git clone https://$TOKEN@github.com/Telefonica/Evolved5g-${NETAPP_NAME}.wiki.git
                    git clone $GIT_NETAPP_URL.wiki.git
                    cp -R Evolved5g-${NETAPP_NAME}.wiki/* ${NETAPP_NAME}.wiki/
                    cd ${NETAPP_NAME}.wiki/
                    git add -A .
                    git diff-index --quiet HEAD || git commit -m 'Addig Trivy report'
                    git push  https://$TOKEN_EVOLVED@github.com/EVOLVED-5G/$NETAPP_NAME.wiki.git
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
                    sh '''
                    report_file="report-tr-img-${NETAPP_NAME_LOWER}.json"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME/$report_file"

                    curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                        --data-binary @"$report_file" \
                        "$url"

                    cp $report_file $report_file.txt
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

