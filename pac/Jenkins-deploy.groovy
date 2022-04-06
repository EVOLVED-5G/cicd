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

def getContext(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-capif/api-ocp-epg-hi-inet:6443/system:serviceaccount:evol5-capif:deployer";
    } else {
        return "kubernetes-admin@kubernetes";
    }
}

def getPath(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "kubeconfig";
    } else {
        return "~/kubeconfig";
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
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '1', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'dummy-netapp-evolved5g.apps-dev.hi.inet', description: 'netapp hostname')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        APP_REPLICAS="${params.APP_REPLICAS}"
        DUMMY_NETAPP_HOSTNAME="${params.DUMMY_NETAPP_HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= "${params.OPENSHIFT_URL}"
        // For the moment NETAPP_NAME and NAMESPACE are the same, but I separated in case we want to put a different name to each one
        NETAPP_NAME = netappName("${params.GIT_URL}")
        NAMESPACE_NAME = getNamespace("${params.DEPLOYMENT}",netappName("${params.GIT_URL}"))
        DEPLOYMENT = "${params.DEPLOYMENT}"
        CONFIG_PATH = getPath("${params.DEPLOYMENT}")
        CONFIG_CONTEXT = getContext("${params.DEPLOYMENT}") 
    }

    stages {
        stage ('Load privder and backend info'){
            steps {
                dir ("${env.WORKSPACE}/iac/terraform/") {
                    sh '''
                    sed -i -e "s,CONFIG_PATH,${CONFIG_PATH},g" -e "s,CONFIG_CONTEXT,${CONFIG_CONTEXT},g" provider.tf
                    '''
                }
            }
        }
        stage ('Configure Provider for the specific deployment') {
            parallel{
                stage('Configuration in Openshift'){
                    when {
                        allOf {
                            expression { DEPLOYMENT == "openshift"}
                        }
                    }
                    steps {
                        dir ("${env.WORKSPACE}/iac/terraform/") {
                            sh '''
                            kubectl config use-context evol5-capif/api-ocp-epg-hi-inet:6443/system:serviceaccount:evol5-capif:deployer
                            '''
                        }
                    }
                }
                stage('Configuration in Kubernetes'){
                    when {
                        allOf {
                            expression { DEPLOYMENT == "kubernetes-athens"}
                        }
                    }
                    steps {
                        dir ("${env.WORKSPACE}/iac/terraform/") {
                            sh '''
                            kubectl config use-context kubernetes-admin@kubernetes
                            '''
                        }
                    }
                }

            }
        }           
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
                                    dir ("${env.WORKSPACE}/iac/terraform/") {
                                        sh '''
                                            export KUBECONFIG="./kubeconfig"
                                            oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                                        '''
                                        readFile('kubeconfig')
                                    }
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
                                dir ("${env.WORKSPACE}/iac/terraform/") {
                                    sh '''
                                        export KUBECONFIG="~/kubeconfig"
                                    '''
                                }
                            
                            }
                        }
                    }
                }
            }
        }    
        stage ('Create namespace in if it does not exist') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                        kubectl create namespace $NAMESPACE_NAME
                        '''
                    }
                }
            }
        }
        stage ('Log into AWS ECR') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-pull', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
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
        }
        stage ('Initiate and configure app in kubernetes') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            terraform init                                                           \
                                -backend-config="bucket=evolved5g-${DEPLOYMENT}-terraform-states"    \
                                -backend-config="key=${NETAPP_NAME}"
                        '''
                    }
                }
            }
        }
        stage ('Deploy Netapp') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            export AWS_PROFILE=default
                            terraform validate
                            terraform plan -var app_replicas=${APP_REPLICAS} -var namespace_name=${NAMESPACE_NAME} -var netapp_name=${NETAPP_NAME} -out deployment.tfplan
                            terraform apply --auto-approve deployment.tfplan
                        '''
                    }
                }
            }
        }
        stage ('Expose service in platform') {
            parallel {
                stage ('Expose service in Openshift') {
                    when {
                        allOf {
                            expression { DEPLOYMENT == "openshift"}
                        }
                    }
                    stages{
                            stage ('Expose service in Openshift') {
                                steps {
                                    withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                                        catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                            sh '''
                                                oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                                                oc create service nodeport dummy-netapp --tcp=8080:8080
                                                oc expose service dummy-netapp --hostname=$DUMMY_NETAPP_HOSTNAME
                                            '''
                                    }
                                }
                            }
                        }
                    }
                }            
                stage ('Expose service in Kubernetes'){
                    when {
                        allOf {
                            expression { DEPLOYMENT == "kubernetes-athens"}
                        }
                    }
                    stages{
                        stage('Expose in Kubernetes') {
                            steps {
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    sh '''
                                        kubectl create service nodeport dummy-netapp --tcp=8080:8080
                                        kubectl expose service dummy-netapp --hostname=$DUMMY_NETAPP_HOSTNAME
                                    '''
                                }
                            }
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