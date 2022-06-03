def getContext(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-nef/api-ocp-epg-hi-inet:6443/system:serviceaccount:evol5-nef:deployer";
    } else { 
        return "kubernetes-admin@kubernetes";
    }
}

def getPath(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "~/.kube/config";
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
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens","kubernethes-uma"])  
    }

    environment {
        GIT_CICD_BRANCH="${params.GIT_BRANCH}"
        APP_REPLICAS="${params.APP_REPLICAS}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= "${params.OPENSHIFT_URL}"
        NEF_NAME = "nef_emulator"
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
                    cp backend.tf $NEF_NAME/
                    cp provider.tf $NEF_NAME/
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
                        stage('Login openshift') {
                            steps {
                                withCredentials([string(credentialsId: 'openshiftv4-nef', variable: 'TOKEN')]) {
                                    dir ("${env.WORKSPACE}/iac/terraform/${NEF_NAME}") {
                                        sh '''
                                            oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL

                                        '''
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
                                    '''
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
                                -backend-config="key=${NEF_NAME}"
                            terraform destroy --auto-approve
                        '''
                    }
                }
            }
        }
    }
    // post {
    //     cleanup{
    //         /* clean up our workspace */
    //         deleteDir()
    //         /* clean up tmp directory */
    //         dir("${env.workspace}@tmp") {
    //             deleteDir()
    //         }
    //         /* clean up script directory */
    //         dir("${env.workspace}@script") {
    //             deleteDir()
    //         }
    //     }
    // }
}