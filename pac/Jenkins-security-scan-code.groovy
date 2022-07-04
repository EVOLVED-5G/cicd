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
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        TOKEN_TRIVY = credentials('token_trivy')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
        DOCKER_PATH="/usr/src/app"
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
                    curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-repo?token=$TOKEN_TRIVY&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=md" 
                    curl -s -H 'Content-Type: application/json' -X POST "http://epg-trivy.hi.inet:5000/scan-repo?token=$TOKEN_TRIVY&update_wiki=false&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=json" > report-tr-repo-$NETAPP_NAME_LOWER.json
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
                    git diff-index --quiet HEAD || git commit -m 'Addig Trivy scan report'
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
                        python3 utils/report_generator.py --template templates/scan-repo.md.j2 --json report-tr-repo-$NETAPP_NAME_LOWER.json --output report-tr-repo-$NETAPP_NAME_LOWER.md
                        docker build  -t pdf_generator utils/docker_generate_pdf/.
                        docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/report-tr-repo-$NETAPP_NAME_LOWER.pdf $DOCKER_PATH/report-tr-repo-$NETAPP_NAME_LOWER.md
                        declare -a files=("json" "md" "pdf")
                        
                        for x in "${files[@]}"
                            do
                                report_file="report-tr-repo-$NETAPP_NAME_LOWER.$x"
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
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
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



