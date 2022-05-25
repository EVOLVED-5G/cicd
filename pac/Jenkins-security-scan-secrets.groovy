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
        stage('Get Repo and clone'){
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH https://$TOKEN@github.com/Telefonica/Evolved5g-${NETAPP_NAME}                                                
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL  
                    rm -rf Evolved5g-${NETAPP_NAME}/* 
                    cp -R ${NETAPP_NAME}/* Evolved5g-${NETAPP_NAME}/
                    cd Evolved5g-${NETAPP_NAME}/
                    git add -A .
                    git diff-index --quiet HEAD || git commit -m 'Update repo in Telefonica repo'
                    git push -u origin $GIT_NETAPP_BRANCH
                    '''
                }
           }
        }

        stage('Launch Github Actions command') {
            steps {
                dir ("${env.WORKSPACE}/") {                                                                                                                                     
                    sh '''#!/bin/bash
                    curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-secrets?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&format=md"
                    curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-secrets?token=fb1d3b71-2c1e-49cb-b04b-54534534ef0a&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&format=json" > report-tr-repo-secrets-$NETAPP_NAME_LOWER.json
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
                    git diff-index --quiet HEAD || git commit -m 'Addig Trivy secreats leaks scan report'
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
                    sh '''#!/bin/bash
                        report_file="report-tr-repo-secrets-$NETAPP_NAME_LOWER.json"
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



