pipeline {
    agent { node {label 'evol5-slave'}  }

    parameters {
        string(name: 'NETAPP_NAME', defaultValue: '1.0', description: '')
        
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
    }

    stages {
        stage('Get the container from the Artifactory') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image pull  dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                        '''
                    }
                }
            }
        }

        stage('Execute the container in the platform (Docker)') {
            steps {
                dir ("${env.WORKSPACE}") {
                        sh '''
                        sudo docker run -d --name netapp -i evolved-5g/${NETAPP_NAME} 
                        '''
                    }
                
            }
        }
        stage('Test stage') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    sudo docker exec -it ${NETAPP_NAME} curl localhost:8080
                    '''
                }
            }
        }
        stage('Validation container') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    sudo docker commit netapp evolved-5g/validation/${NETAPP_NAME} 
                    '''
                }
            }
        }

        stage('Publish in the validated directory') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image tag evolved-5g/validation/dummy-netapp dockerhub.hi.inet/evolved-5g/validation/dummy-netapp:${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/validation/dummy-netapp dockerhub.hi.inet/evolved-5g/validation/dummy-netapp:latest
                        docker image push --all-tags dockerhub.hi.inet/evolved-5g/validation/dummy-netapp
                        '''
                    }
                }
            }
        }
        stage('Clean up docker enviroment') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    sudo docker rm -vf $(sudo docker ps -a -q)
                    sudo docker rmi -f $(sudo docker images -a -q)
                    '''
                }
            }
        }

    }
}

