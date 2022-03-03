String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    var= var.toLowerCase()
    return var ;
}
pipeline {
    agent {node {label params.AGENT == "any" ? "" : params.AGENT }}

    parameters {
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '1', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'dummy-netapp-evolved5g.apps-dev.hi.inet', description: 'netapp hostname')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        choice(name: "AGENT", choices: ["evol5-slave", "evol5-athens"]) 
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
        NAMESPACE_NAME = netappName("${params.GIT_URL}")
    }

    stages {
        stage('Login openshift') {
            steps {
                when {
                    expression {
                        return env.BRANCH_NAME != 'Openshiftv4';
                    }
                }
                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
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
        stage ('Create namespace in if it does not exist') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            kubectl create namespace $NAMESPACE_NAME
                        '''
                    }
                }
            }
        }
        stage ('Deploy app in kubernetess') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            terraform init
                            terraform validate
                            terraform plan -var app_replicas=${APP_REPLICAS} -var namespace_name=${NAMESPACE_NAME} -var netapp_name=${NETAPP_NAME}-out deployment.tfplan
                            terraform apply --auto-approve deployment.tfplan
                        '''
                    }
                }
            }
        }
        stage ('Expose service') {
            steps {
                withCredentials([string(credentialsId: 'openshiftv4', variable: 'TOKEN')]) {
                    catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                        sh '''
                            oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                            oc expose service dummy-netapp --hostname=$DUMMY_NETAPP_HOSTNAME
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