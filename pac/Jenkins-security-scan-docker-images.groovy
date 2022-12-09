String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

def getPath(deployment) {
    String var = deployment
    if("verification".equals(var)) {
        return "";
    }else if("validation".equals(var)){
        return "validation"
    }else {
        return "certification";
    }
}

pipeline {
    agent { node {label 'evol5-openshift'}  }
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ["verification", "validation", "certification"])
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
        TOKEN_TRIVY = credentials('token_trivy')
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        STAGE=getPath("${params.STAGE}")
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        DOCKER_PATH="/usr/src/app"
    }

    stages {

        stage('Launch Github Actions command') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(3)
                }
            steps {

                dir ("${env.WORKSPACE}/") {
                    sh 'printenv'
                    sh '''#!/bin/bash

                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/${STAGE}/${NETAPP_NAME_LOWER} -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*" | tr -d '"' )
                    images=($response)

                    for x in "${images[@]}"
                    do
                        curl -s -H "Content-Type: application/json" -X POST "http://epg-trivy.hi.inet:5000/v1/scan-image?token=$TOKEN_TRIVY&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=markdown&image=dockerhub.hi.inet/evolved-5g/$STAGE/$NETAPP_NAME_LOWER/$x" 
                        curl -s -H "Content-Type: application/json" -X POST "http://epg-trivy.hi.inet:5000/v1/scan-image?token=$TOKEN_TRIVY&update_wiki=true&repository=Telefonica/Evolved5g-$NETAPP_NAME&branch=$GIT_NETAPP_BRANCH&output_format=json&image=dockerhub.hi.inet/evolved-5g/$STAGE/$NETAPP_NAME_LOWER/$x" > report-tr-img-$x.json
                    done
                    '''
                }
            }
        }
        stage('Get wiki repo and update Evolved Wiki'){
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
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
                    sh '''#!/bin/bash
                    response=$(curl -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/docker/evolved-5g/ -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep "${NETAPP_NAME_LOWER}*" | tr -d '"' )
                    versionT=0.35.0
                    declare -a files=("json" "md" "pdf")
                    images=($response)
                    docker build  -t pdf_generator utils/docker_generate_pdf/.
                    for x in "${images[@]}"
                    do  
                        urlT=https://github.com/EVOLVED-5G/$NETAPP_NAME/wiki/dockerhub.hi.inet-evolved-5g-$STAGE-$NETAPP_NAME_LOWER-$x
                        python3 utils/report_sonar_generator.py --template templates/scan-image.md.j2 --json report-tr-img-$x.json --output report-tr-img-$x.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit commit --version $versionT --url $urlT
                        
                        docker run -v "$WORKSPACE":$DOCKER_PATH pdf_generator markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/report-tr-img-$x.pdf $DOCKER_PATH/report-tr-img-$x.md

                        # Check to see if the image has succesfully passed all tests
                        if grep -q "failed" report-tr-img-$x.md; then
                            result=false
                        else
                            result=true
                        fi
                        if  $result ; then
                            echo "Scan secrets was completed succesfuly"
                        else
                            exit 1
                        fi

                        for y in "${files[@]}"
                        do
                            report_file="report-tr-img-$x.$y"
                            url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                            curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                --data-binary @"$report_file" \
                                "$url"
                        done
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