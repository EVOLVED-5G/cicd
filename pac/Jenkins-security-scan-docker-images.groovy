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
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY= credentials("artifactory_credentials")
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        TOKEN = credentials('github_token_cred')
    }

    stages {

        stage('Launch Github Actions command') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''#!/bin/bash

                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/ -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*")

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
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH https://$TOKEN@github.com/Telefonica/Evolved5g-${NETAPP_NAME}.wiki.git
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL.wiki.git
                    #rm -rf Evolved5g-${NETAPP_NAME}.wiki/*
                    cp -R ${NETAPP_NAME}.wiki/* Evolved5g-${NETAPP_NAME}.wiki/
                    cd Evolved5g-${NETAPP_NAME}.wiki/
                    git add .
                    git commit -m "Adding repo to Telefonica Project" || true
                    git push -u origin $GIT_NETAPP_BRANCH
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

