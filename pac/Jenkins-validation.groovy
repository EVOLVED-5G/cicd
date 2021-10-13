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
        stage('Get the container') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image pull --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}
                        '''
                    }
                }
            }
        }
        stage('Test stage') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    ls
                    pwd
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
    }
}

