

pipeline {
    agent { node {label 'evol5-slave-uma'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
    }

    environment {
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
    }

    stages {
        stage('Build') {
            steps {
                dir ("${env.WORKSPACE}/src/") {
                    sh '''
                    docker build -t evolved-5g/dummy-netapp .
                    '''
                }
            }
        }

        stage('Publish') {
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir ("${env.WORKSPACE}/src/") {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        docker image tag evolved-5g/dummy-netapp dockerhub.hi.inet/evolved-5g/dummy-netapp:${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/dummy-netapp dockerhub.hi.inet/evolved-5g/dummy-netapp:latest
                        docker image push --all-tags dockerhub.hi.inet/evolved-5g/dummy-netapp
                        '''
                    }
                }
            }
        }
    }
}

