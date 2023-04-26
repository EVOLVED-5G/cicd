
String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

def getAgent(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-openshift";
    }else if("kubernetes-athens".equals(var)){
        return "evol5-athens"
    }else {
        return "evol5-slave";
    }
}

def results = [:]

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        string(name: 'RELEASE_NAME', defaultValue: 'capif', description: 'Helm Release name to CAPIF')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        RELEASE_NAME = "${params.RELEASE_NAME}"
        REPORT_FILENAME = '006-report-nef-logging'
        ARTIFACTORY_URL = 'http://artifactory.hi.inet/artifactory/misc-evolved5g/validation'
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
    }

    stages {
        
        stage('Verify is NEF create a log on CAPIF - Kubernetes') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            options {
                retry(12)
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    script {
                        try {
                            sh '''#!/bin/bash
                            sleep 60

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            NAMESPACE=$(helm ls --kubeconfig /home/contint/.kube/config --all-namespaces -f "^$RELEASE_NAME" | awk 'NR==2{print $2}')
                            echo "NAMESPACE $NAMESPACE"

                            INVOCATION_LOGS=$(kubectl --kubeconfig /home/contint/.kube/config \
                            -n $NAMESPACE logs -l io.kompose.service=api-invocation-logs | grep "Added log entry to apis:")

                            if [[ $INVOCATION_LOGS ]]; then
                                echo "INVOCATION_LOGS: $INVOCATION_LOGS"
                                echo $INVOCATION_LOGS |sed -r 's/^Added log entry to apis: //'|sed -r "s/\'/\"/g"> ${REPORT_FILENAME}.json
                                result=true
                                kubectl -n $NAMESPACE get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $NAMESPACE logs 
                                echo "Network App is onboarded correctly in CAPIF"
                            else
                                echo "The NEF Services logs are not present in CAPIF"
                                echo "NGINX_LOG:"
                                kubectl -n $NAMESPACE get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $NAMESPACE logs 
                                result=false
                                exit 1
                            fi
                            '''
                        } catch (e) {
                            unstable("The NEF Services logs are not present in CAPIF")
                        }
                    }
                }
            }
        }
        stage('Verify is NEF create a log on CAPIF - Openshift') {
            when {
                    allOf {
                        expression { DEPLOYMENT == "openshift"}
                    }
                }
            environment {
                TOKEN_NS_CAPIF = credentials("token-os-capif")
            }
            options {
                retry(12)
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    script {
                        try {
                            sh '''#!/bin/bash
                            sleep 60
                            TMP_NS_CAPIF=evol5-capif

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            echo "TMP_NS_CAPIF: $TMP_NS_CAPIF"

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF 

                            INVOCATION_LOGS=$(kubectl logs \
                            -l io.kompose.service=api-invocation-logs | grep "Added log entry to apis:")

                            if [[ $INVOCATION_LOGS ]]; then
                                echo "INVOCATION_LOGS: $INVOCATION_LOGS"
                                echo $INVOCATION_LOGS |sed -r 's/^Added log entry to apis: //'|sed -r "s/\'/\"/g"> ${REPORT_FILENAME}.json
                                kubectl -n $TMP_NS_CAPIF get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $TMP_NS_CAPIF logs
                                echo "Network App is onboarded correctly in CAPIF"
                            else
                                echo "The NEF Services logs are not present in CAPIF"
                                echo "INVOCATION_LOGS: $INVOCATION_LOGS"
                                kubectl -n $TMP_NS_CAPIF get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $TMP_NS_CAPIF logs 
                                result=false
                                exit 1
                            fi
                            '''
                        } catch (e) {
                            unstable("The NEF Services logs are not present in CAPIF")
                        }
                    }
                }
            }
        }
        stage('Upload report to Artifactory') {
            when {
                    allOf {
                        expression { REPORTING == true}
                    }
                }
            options {
                retry(2)
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    report_file="${REPORT_FILENAME}.json"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                    if [ -f "$report_file" ]; then
                        echo "$report_file exists. Uploading to artifactory."
                        curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                        --data-binary @"$report_file" \
                        "$url"
                    else
                        echo "$FILE does not exist."
                    fi
                '''
                }
            }
        }
    }
    post {
        always {
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: 'jenkins-evolved5G@tid.es',
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
                }
            }
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