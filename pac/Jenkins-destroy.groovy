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
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        string(name: 'NETAPP_NAME', defaultValue: 'dummy-netapp', description: 'dummy-netapp')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens","kubernethes-uma"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= "${params.OPENSHIFT_URL}"
        DEPLOYMENT = "${params.DEPLOYMENT}"
        NETAPP_NAME = "${params.NETAPP_NAME}"
        CONFIG_PATH = getPath("${params.DEPLOYMENT}")
        CONFIG_CONTEXT = getContext("${params.DEPLOYMENT}") 
    }
    
    stages {
        stage('Configuring prvider file'){
            steps{
                dir ("${env.WORKSPACE}/iac/terraform/") {
                    sh '''
                    sed -i -e "s,CONFIG_PATH,${CONFIG_PATH},g" -e "s,CONFIG_CONTEXT,${CONFIG_CONTEXT},g" provider.tf
                    '''
                }
            }
        }
        stage('Destroying infrastructure in Openshift or Kubernetes'){
            parallel{
                stage ('Destroying in Openshift') {
                    when {
                        allOf {
                            expression { DEPLOYMENT == "openshift"}
                        }
                    }
                    stages{
                        stage('Login openshift to get kubernetes credentials') {
                            steps {
                                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                                    dir ("${env.WORKSPACE}/iac/terraform/") {
                                        sh '''
                                            export KUBECONFIG="./kubeconfig"
                                            oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                                            kubectl config use-context evol5-capif/api-ocp-epg-hi-inet:6443/system:serviceaccount:evol5-capif:deployer
                                        '''
                                        readFile('kubeconfig')
                                    }
                                }
                            }
                        }
                        stage ('Remove service expose in openshift') {
                            steps {
                                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                        dir ("${env.WORKSPACE}/iac/terraform/") {
                                            sh '''
                                                oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                                                oc delete service dummy-netapp
                                                oc delete route dummy-netapp
                                            '''
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                stage ('Destroying in Kubernetes') {
                    when {
                        allOf {
                            expression { DEPLOYMENT == "kubernetes-athens"}
                        }
                    }
                    stages{
                        stage('Login openshift to get kubernetes credentials') {
                            steps { 
                                dir ("${env.WORKSPACE}/iac/terraform/") {
                                    sh '''
                                        export KUBECONFIG="~/kubeconfig"
                                        kubectl config use-context kubernetes-admin@kubernetes
                                    '''
                                }
                            }
                        }
                        stage('Removing services') {
                            steps { 
                                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                                    dir ("${env.WORKSPACE}/iac/terraform/") {
                                        sh '''
                                        kubectl delete service dummy-netapp -n ${NETAPP_NAME}
                                        '''
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        stage ('Undeploy app in kubernetess') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            terraform init                                                           \
                                -backend-config="bucket=evolved5g-${DEPLOYMENT}-terraform-states"    \
                                -backend-config="key=${NETAPP_NAME}"
                            terraform destroy --auto-approve
                        '''
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