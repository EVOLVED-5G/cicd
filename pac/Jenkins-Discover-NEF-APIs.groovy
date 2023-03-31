
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
        string(name: 'RELEASE_CAPIF', defaultValue: 'capif', description: 'Helm Release name to CAPIF')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        RELEASE_CAPIF = "${params.RELEASE_CAPIF}"

    }

    stages {        
        stage ("Login in openshift"){
            when {
                    allOf {
                        expression { DEPLOYMENT == "openshift"}
                    }
                }
            steps {
                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                    sh '''
                        oc login --insecure-skip-tls-verify --token=$TOKEN 
                    '''
                }
            }
        }
        stage('Verify if NetworkApp has discovered NEF APIs') {
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                            result=false

                            NAMESPACE=$(helm ls --kubeconfig /home/contint/.kube/config --all-namespaces -f "^$RELEASE_CAPIF" | awk 'NR==2{print $2}')
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