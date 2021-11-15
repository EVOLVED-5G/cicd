String netappName(String url) {
    String var = url.substring(url.lastIndexOf("/") + 1, url.length)
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

        stage('Publish') {
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
    }
}

