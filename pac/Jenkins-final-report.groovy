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
        TOKEN = credentials('github_token_cred')
        TOKEN_TRIVY = credentials('token_trivy')
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {
        stage('Download jsons report to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    response=$(curl -v -s http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/validation/$NETAPP_NAME/$BUILD_NUMBER -u $PASSWORD_ARTIFACTORY | jq ".children[].name" | grep  -i ".md" | tr -d '"' )
                    artifacts=($response)
                    
                    for x in "${artifacts[@]}"
                    do  
                        url=$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_NUMBER/$x
                        curl -u $PASSWORD_ARTIFACTORY -0 $url -o $x
                        echo "\n" >> final_report.md
                        cat $x >> final_report.md
                        echo "\n" >> final_report.md
                    done

                    pandoc -s final_report.md --metadata title="Final report" -o final_report.html
                    pandoc final_report.html --pdf-engine=xelatex -o final_report.pdf

                    declare -a files=("html" "pdf" "md")

                    for x in "${files[@]}"
                    do
                        report_file="final_report.$x"
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