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
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    } else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'evol5-cosmote'
    } else {
        return 'evol5-slave'
    }
}

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}

    parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote'])
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        HOSTNAME="${params.HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        DEPLOYMENT_NAME = "marketplace"
        NAMESPACE_NAME = "marketplace"
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
                sh '''
                helm uninstall $DEPLOYMENT_NAME
                '''
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