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
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DEPLOY_NAME', defaultValue: 'dummy-netapp', description: 'Netapp hostname')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_CICD_BRANCH}"
        DUMMY_NETAPP_HOSTNAME="${params.DUMMY_NETAPP_HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        DEPLOYMENT_NAME = "${params.DEPLOY_NAME}"
        NAMESPACE_NAME = "fogus" //Parametrized here and create an universal pipeline for building
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
                    kubectl delete secret docker-registry regcred --ignore-not-found --namespace=$NAMESPACE_NAME
                    kubectl create secret docker-registry regcred                                   \
                    --docker-password=$(aws ecr get-login-password)                                 \
                    --namespace=$NAMESPACE_NAME                                                     \
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
                    sh '''
                    helm upgrade --install --debug --kubeconfig /home/contint/.kube/config --create-namespace -n $NAMESPACE_NAME --wait $DEPLOYMENT_NAME ./cd/helm/$DEPLOYMENT_NAME/ --set nef_hostname=$HOSTNAME --atomic
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
                    helm upgrade --install --debug --wait $DEPLOYMENT_NAME ./cd/helm/$DEPLOYMENT_NAME/ --set nef_hostname=$HOSTNAME --atomic
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