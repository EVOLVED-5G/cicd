pipeline {
    agent any

    parameters {
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= 'https://openshift-epg.hi.inet:443'
    }

    stages {
        stage('Login openshift to get kubernetes credentials') {
            steps {
                withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
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
        stage ('Remove service expose') {
            steps {
                withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
                            oc delete route dummy-netapp
                        '''
                    }
                }
            }
        }
        stage ('Undeploy app in kubernetess') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: '328ab84a-aefc-41c1-aca2-1dfae5b150d2', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                            terraform init
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