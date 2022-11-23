
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

//Define a global variable in order to get this result
pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}
    options {
        timeout(time: 10, unit: 'MINUTES')
        retry(2)
    }

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }
// Parsear información sobre los logs de CAPIF y obtener la traza que demuestra que la NETAPP ha sido onboarded
// Preguntar NACHO cual es el log que da esta informacion -- Pedir a Nacho una KEY especifica
// kubectl/oc logs y parsear la salida  --

stages {        
        stage ('Login in openshift or Kubernetes'){
            parallel {
                stage ('Login in Openshift platform') {
                    when {
                        allOf {
                            expression { DEPLOYMENT == "openshift"}
                        }
                    }
                    stages{
                        stage('Login openshift') {
                            steps {
                                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                                    sh '''
                                        oc login --insecure-skip-tls-verify --token=$TOKEN 
                                    '''
                                }
                            }
                        }
                    }
                }            
                stage ('Login in Kubernetes Platform'){
                    when {
                        allOf {
                            expression { DEPLOYMENT == "kubernetes-athens"}
                        }
                    }
                    stages{
                        stage('Login in Kubernetes') {
                            steps { 
                                withKubeConfig([credentialsId: 'kubeconfigAthens']) {
                                    sh '''
                                    kubectl get all -n kube-system
                                    '''
                                }
                            }
                        }
                        stage ('Create namespace in if it does not exist') {
                            steps {
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    sh '''
                                    kubectl create namespace evol-$NAMESPACE_NAME
                                    '''
                                }
                            }              
                        }
                    }
                }
            }
        }
        stage('Verify is netapp is onboarded') {
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''#!/bin/bash
                    result=false
                    value_pod=$(kubectl get pods | grep ^python-netapp | awk '{print $1}')
                    kubectl exec $value_pod -- python 1_netapp_to_capif.py 
                    value=$(kubectl get pods | grep ^api-invoker-management | awk '{print $1}')
                    logs=$(kubectl logs --tail=20 $value)
                    echo $logs
                    while IFS= read -r line; do
                        if [[ $line == *"Netapp onboarded sucessfuly"* ]]; then
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