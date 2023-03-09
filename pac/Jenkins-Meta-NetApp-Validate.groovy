import groovy.json.JsonOutput
def buildResults = [:]

String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent {
        node {
            label 'evol5-slave'
        }
    }
    options {
        timeout(time: 60, unit: 'MINUTES')
        retry(1)
    }


    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'DEPLOY_NAME', defaultValue: 'fogus', description: 'Deployment netapp name')
        choice(name: 'ENVIRONMENT', choices: ["openshift", "athens", "malaga"])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {
        stage('prueba'){
            steps{
                echo "prueba de que me envie un email"
            }
        }
        
    }

    post {
        always {
            script {
                // Emails != null, then it has been sended from microbackend
                if (emails?.trim()) {
                    dir ("${WORKSPACE}/") {
                        sh '''#!/bin/bash

                        report_file="report-sonar-${NETAPP_NAME}-evolved5g.pdf"
                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                        curl -u $PASSWORD_ARTIFACTORY "$url" -o "report-sonar-${NETAPP_NAME}-evolved5g.pdf"
                        '''
                    }
                    emails.tokenize().each() {
                        // email -> emailext subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                        //          from: 'jenkins-evolved5G@tid.es',
                        //          to: email
                        email -> emailext attachmentsPattern: "**/report-sonar-${NETAPP_NAME}-evolved5g.pdf",
                                //  attachmentsPattern: '**/report_Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH}.html.txt',
                                 body: '''${SCRIPT, template="groovy-html.template"}''',
                                 mimeType: 'text/html',
                                 subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                                 from: 'jenkins-evolved5G@tid.es',
                                 replyTo: "jenkins-evolved5G",
                                 to: email
                    }
                }
            }
            // emailext body: '''${SCRIPT, template="groovy-html.template"}''',
            //     mimeType: 'text/html',
            //     subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
            //     from: 'jenkins-evolved5G@tid.es',
            //     replyTo: "jenkins-evolved5G",
            //     recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
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
