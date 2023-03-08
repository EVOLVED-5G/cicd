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
        retry(2)
    }
    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of NetworkApp pods to run')
        string(name: 'RELEASE_NAME', defaultValue: 'dummy-network-app', description: 'Name to NetworkApp')
        string(name: 'FOLDER_NETWORK_APP', defaultValue: 'dummy-network-app', description: 'Folder where the NetworkApp is')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_CICD_BRANCH}"
        AWS_DEFAULT_REGION = 'eu-central-1'
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
        stage ('Log into AWS ECR') {
            when {
                allOf {
                    expression { DEPLOYMENT == "kubernetes-athens"}
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-pull', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    sh '''
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$RELEASE_NAME-${BUILD_NUMBER}
                    kubectl create namespace $RELEASE_NAME-${BUILD_NUMBER}
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=$RELEASE_NAME-${BUILD_NUMBER}                                                 \
                    --docker-server=709233559969.dkr.ecr.eu-central-1.amazonaws.com                 \
                    --docker-username=AWS
                    '''
                }    
            }    
        }
        stage ('Upgrade app in kubernetes') {
            when {
                allOf {
                    expression { DEPLOYMENT == "kubernetes-athens" }
                }
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''#!/bin/bash
                           OUTPUT=($(helm ls --all-namespaces -q -f $RELEASE_NAME))
                           echo "$OUTPUT"
                           ARRAY=$(declare -p OUTPUT | grep -q '^declare -a' && echo array || echo no array)
                            if [[ $ARRAY == "array" ]]; then
                                if [[ " ${OUTPUT[@]} " =~ " ${RELEASE_NAME} " ]]; then
                                    echo "Release name $RELEASE_NAME already exists, use another release name"
                                    exit 1
                                else
                                    echo "applying helm"
                                    helm upgrade --install --debug --kubeconfig /home/contint/.kube/config \
                                    --create-namespace -n $RELEASE_NAME-${BUILD_NUMBER} \
                                    --wait $RELEASE_NAME ./cd/helm/$FOLDER_NETWORK_APP/ \
                                    --set nef_hostname=$HOSTNAME --set app_replicas=$APP_REPLICAS \
                                    --atomic
                                fi
                            fi
                    '''

                }
            }
        }
        stage ('Upgrade app in Openshift') {
            when {
                allOf {
                    expression { DEPLOYMENT == "openshift"}
                }
            }
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    helm upgrade --install --debug -n evol5-$RELEASE_NAME \
                    --wait $RELEASE_NAME ./cd/helm/$FOLDER_NETWORK_APP/ \
                    --set nef_hostname=$HOSTNAME --set app_replicas=$APP_REPLICAS \
                    --atomic
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