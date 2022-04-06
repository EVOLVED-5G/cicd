String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    var= var.toLowerCase()
    return var ;
}
pipeline {
    agent { node {label 'evol5-slave'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_URL', defaultValue: '', description: '')
        string(name: 'GIT_BRANCH', defaultValue: '', description: '')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        NETAPP_NAME = netappName("${params.GIT_URL}")
    }

    stages {
        stage('Get the code!') {
            steps {
                sh '''
                rm -rf dummyapp
                mkdir dummyapp
                cd dummyapp
                git clone --single-branch --branch $GIT_BRANCH $GIT_URL .
                '''
            }
        }
        stage('Build') {
            steps {
                dir ("${env.WORKSPACE}/dummyapp/") {
                    sh '''
                    docker build -t evolved-5g/${NETAPP_NAME} .
                    '''
                }
            }
        }
        stage('Publish in AWS') {
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                        $(aws ecr get-login-password)
                        docker image tag evolved-5g/${NETAPP_NAME} 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/${NETAPP_NAME} 709233559969.dkr.ecr.eu-central-1.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        docker image push --all-tags 
                        '''
                    }    
                }   
            }
        }
        stage('Publish in Artefactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/dummyapp/") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                        docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}
                        '''
                    }
                }
            }
        }
        stage('Cleaning docker images and containers') {
            steps {
                dir ("${env.WORKSPACE}/dummyapp/") {
                    sh '''
                    docker stop $(docker ps -q)
                    docker rm $(docker ps -a -q)
                    docker rmi $(docker images -a -q)
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

