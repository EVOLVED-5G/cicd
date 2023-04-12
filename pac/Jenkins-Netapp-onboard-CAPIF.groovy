
def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    }else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    }else {
        return 'evol5-slave'
    }
}

//Define a global variable in order to get this result
pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'RELEASE_NAME', defaultValue: 'fogus', description: 'Helm release name')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }
// Parsear información sobre los logs de CAPIF y obtener la traza que demuestra que la NETAPP ha sido onboarded
// Preguntar NACHO cual es el log que da esta informacion -- Pedir a Nacho una KEY especifica
// kubectl/oc logs y parsear la salida  --

    environment {
        GIT_BRANCH="${params.GIT_CICD_BRANCH}"
        RELEASE_NAME = "${params.RELEASE_NAME}"
        DEPLOYMENT = "${params.DEPLOYMENT}"
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
        stage('Verify is NetworkApp is onboarded - Kubernetes') {
            when {
                anyOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                    expression { DEPLOYMENT == "kubernetes-uma" }
                }
            }
            steps {
                // value_pod=$(kubectl --kubeconfig /home/contint/.kube/config -n $NAMESPACE get pods | grep ^python-netapp | awk '{print $1}')
                    // kubectl --kubeconfig /home/contint/.kube/config -n $NAMESPACE exec $value_pod -- python 1_netapp_to_capif.py 
                    // if [[ $line == *"POST /api-invoker-management/v1/onboardedInvokers HTTP"* ]]; then
                    
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    result=false
                    NAMESPACE=$(helm ls --all-namespaces -f $RELEASE_NAME | awk 'NR==2{print $2}')
                    echo namespace=$NAMESPACE
                    logs=$(kubectl --kubeconfig /home/contint/.kube/config -n $NAMESPACE logs -l io.kompose.service=api-invoker-management | grep "Invoker Created")
                    echo $logs

                    if [[ $logs ]]; then
                        echo "API_INVOKER_LOGS: $logs"
                        result=true
                        echo "Onboard of Network App works correctly"
                    else
                        echo "API_INVOKER_LOGS: $logs"
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
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    result=false
                    NAMESPACE=evol5-nef
                    echo namespace=$NAMESPACE
                    // value_pod=$(kubectl -n $NAMESPACE get pods | grep ^python-netapp | awk '{print $1}')
                    // kubectl -n $NAMESPACE exec $value_pod -- python 1_netapp_to_capif.py 
                    value=$(kubectl -n $NAMESPACE get pods | grep ^api-invoker-management | awk '{print $1}')
                    logs=$(kubectl -n $NAMESPACE logs --tail=20 $value)
                    echo $logs
                    while IFS= read -r line; do
                        if [[ $line == *"POST /api-invoker-management/v1/onboardedInvokers HTTP"* ]]; then
                            result=true
                        fi
                    done <<< "$logs"
                    echo $result
                    if  $result ; then
                        echo "NETAPP was onboarded successfuly"
                    else
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