
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

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'RELEASE_NAME', defaultValue: 'capif', description: 'Helm Release name to CAPIF')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        RELEASE_NAME = "${params.RELEASE_NAME}"

    }

    stages {
        
        stage('Verify is Network App is onboarded - Kubernetes') {
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
                            result=false

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            NAMESPACE=$(helm ls --kubeconfig /home/contint/.kube/config --all-namespaces -f "^$RELEASE_NAME" | awk 'NR==2{print $2}')
                            echo "NAMESPACE $NAMESPACE"

                            INVOKER_LOG=$(kubectl --kubeconfig /home/contint/.kube/config \
                            -n $NAMESPACE logs -l io.kompose.service=api-invoker-management | grep "Invoker Created")

                            if [[ $INVOKER_LOG ]]; then
                                echo "INVOKER_LOG: $INVOKER_LOG"
                                result=true
                                kubectl -n $NAMESPACE get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $NAMESPACE logs 
                                echo "Network App is onboarded correctly in CAPIF"
                            else
                                echo "There was an error, the Network App cannot be onboarded correctly in CAPIF"
                                echo "NGINX_LOG:"
                                kubectl -n $NAMESPACE get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $NAMESPACE logs 
                                result=false
                                exit 1
                            fi
                            '''
                        } catch (e) {
                            unstable("There was an error, the Network App cannot be onboarded correctly in CAPIF")
                        }
                    }
                }
            }
        }
        stage('Verify is NetworkApp is onboarded - Openshift') {
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
                            result=false
                            TMP_NS_CAPIF=evol5-capif

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            echo "TMP_NS_CAPIF: $TMP_NS_CAPIF"

                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF 

                            INVOKER_LOG=$(kubectl logs \
                            -l io.kompose.service=api-invoker-management | grep "Invoker Created")

                            if [[ $INVOKER_LOG ]]; then
                                echo "INVOKER_LOG: $INVOKER_LOG"
                                result=true
                                kubectl -n $TMP_NS_CAPIF get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $TMP_NS_CAPIF logs
                                echo "Network App is onboarded correctly in CAPIF"
                            else
                                echo "There was an error, the Network App cannot be onboarded correctly in CAPIF"
                                echo "INVOKER_LOG: $INVOKER_LOG"
                                kubectl -n $TMP_NS_CAPIF get pods | grep nginx | awk '{print $1}' | xargs kubectl -n $TMP_NS_CAPIF logs 
                                result=false
                                exit 1
                            fi
                            '''
                        } catch (e) {
                            unstable("There was an error, the Network App cannot be onboarded correctly in CAPIF")
                        }
                    }
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