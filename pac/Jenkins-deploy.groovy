pipeline {
    agent {node {label params.AGENT == "any" ? "" : params.AGENT }}

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '1', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'dummy-netapp-evolved5g.apps-dev.hi.inet', description: 'netapp hostname')
        string(name: 'OPENSHIFT_URL', defaultValue: 'https://api.ocp-epg.hi.inet:6443', description: 'openshift url')
        choice(name: "AGENT", choices: ["evol5-slave", "evol5-athens"]) 
    }

    environment {
        GIT_BRANCH="${params.GIT_BRANCH}"
        APP_REPLICAS="${params.APP_REPLICAS}"
        DUMMY_NETAPP_HOSTNAME="${params.DUMMY_NETAPP_HOSTNAME}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= "${params.OPENSHIFT_URL}"
    }

    stages {
        stage('Login openshift') {
            steps {
                withCredentials([string(credentialsId: 'kubeconfigOSv4', variable: 'TOKEN')]) {
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
        stage ('Deploy app in kubernetess') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            terraform init
                            terraform validate
                            terraform plan -var app_replicas=${APP_REPLICAS} -out deployment.tfplan
                            terraform apply --auto-approve deployment.tfplan
                        '''
                    }
                }
            }
        }
        stage ('Expose service') {
            steps {
                withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
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