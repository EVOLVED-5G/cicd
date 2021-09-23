pipeline {
    agent { node {label 'evol5-slave'}  }

    parameters {
        string(name: 'GIT_URL', defaultValue: '', description: '')
        string(name: 'GIT_BRANCH', defaultValue: '', description: '')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        OPENSHIFT_URL= 'https://openshift-epg.hi.inet:443'
    }

    stages {
        stage('Get the code!') {
            steps {
                sh '''
                rm -rf dummyapp
                mkdir -p dummyapp
                cd dummyapp
                git clone --single-branch --branch $GIT_BRANCH $GIT_URL .
                '''
            }
        }
        stage('Login openshift') {
            steps {
                withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
                    dir ("${env.WORKSPACE}/dummyapp/iac/terraform/") {
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
                    dir ("${env.WORKSPACE}/dummyapp/iac/terraform/") {
                        sh '''
                            ls
                            cat kubeconfig
                            terraform init
                            terraform validate
                            terraform plan -out deployment.tfplan
                            terraform apply --auto-approve deployment.tfplan
                        '''
                    }
                }
            }
        }
        // stage ('Expose service') {
        //     steps {
        //         withCredentials([string(credentialsId: '18e7aeb8-5552-4cbb-bf66-2402ca6772de', variable: 'TOKEN')]) {
        //             dir ("${env.WORKSPACE}/dummyapp/iac/terraform/") {
        //                 sh '''
        //                     oc login --insecure-skip-tls-verify --token=$TOKEN $OPENSHIFT_URL
        //                     oc expose service dummy-netapp-service
        //                 '''
        //             }
        //         }
        //     }
        // }
    }
}