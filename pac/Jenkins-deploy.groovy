String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    var= var.toLowerCase()
    return var ;
}

def getNamespace(deployment,name) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-capif";
    } else {
        return name;
    }
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

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}


    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Netapp hostname')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_CICD_BRANCH}"
        APP_REPLICAS="${params.APP_REPLICAS}"
        DUMMY_NETAPP_HOSTNAME="${params.DUMMY_NETAPP_HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        DEPLOYMENT_NAME = "fogus"
        NAMESPACE_NAME = "fogus" //Parametrized here and create an universal pipeline for building
        DEPLOYMENT = "${params.DEPLOYMENT}"
    }

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
        //WORK IN PROGRESS FOR THE ATHENS DEPLOYEMENT    
        stage ('Log into AWS ECR') {
            when {
                allOf {
                    expression { DEPLOYMENT == "evol5-athens"}
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
        stage ('Initiate and configure app in kubernetes') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    helm install $DEPLOYMENT_NAME ./cd/helm/$DEPLOYMENT_NAME/  --set hostname=$DUMMY_NETAPP_HOSTNAME  --set app_replicas=APP_REPLICAS
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