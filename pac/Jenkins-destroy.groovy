pipeline {
    agent {node {label params.AGENT == "any" ? "" : params.AGENT }}

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        string(name: 'NETAPP_NAME', defaultValue: 'dummy-netapp', description: 'dummy-netapp')
        choice(name: "AGENT", choices: ["evol5-slave", "evol5-athens"]) 
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens"])  
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= "${params.OPENSHIFT_URL}"
        DEPLOYMENT = "${params.DEPLOYMENT}"
        NETAPP_NAME = "${params.NETAPP_NAME}"
    }
    
    stages {
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
                                    '''
                                }
                            }
                        }
                        stage('Removing services') {
                            steps { 
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