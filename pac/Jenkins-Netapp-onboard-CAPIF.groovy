
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
            steps {
                    
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                            result=false

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            NAMESPACE=$(helm ls --kubeconfig /home/contint/.kube/config --all-namespaces -f "^$RELEASE_NAME" | awk 'NR==2{print $2}')
                            echo "NAMESPACE $NAMESPACE"
                            
                            DISCOVER_LOG=$(kubectl --kubeconfig /home/contint/.kube/config \
                            -n $NAMESPACE logs -l io.kompose.service=api-invoker-management | grep "Invoker Created")

                            if [[ $DISCOVER_LOG ]]; then
                                echo "DISCOVER_LOG: $DISCOVER_LOG"
                                result=true
                                echo "DISCOVER APIs work correctly"
                            else
                                echo "DISCOVER_LOG: $DISCOVER_LOG"
                                result=false
                                exit 1
                            fi
                            '''
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
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                            result=false
                            TMP_NS_CAPIF=evol5-capif

                            echo "RELEASE_NAME: $RELEASE_NAME"
                            echo "TMP_NS_CAPIF: $TMP_NS_CAPIF"
                           
                            oc login --insecure-skip-tls-verify --token=$TOKEN_NS_CAPIF 

                            DISCOVER_LOG=$(kubectl logs \
                            -l io.kompose.service=api-invoker-management | grep "Invoker Created")

                            if [[ $DISCOVER_LOG ]]; then
                                echo "DISCOVER_LOG: $DISCOVER_LOG"
                                result=true
                                echo "DISCOVER APIs work correctly"
                            else
                                echo "DISCOVER_LOG: $DISCOVER_LOG"
                                result=false
                                exit 1
                            fi
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