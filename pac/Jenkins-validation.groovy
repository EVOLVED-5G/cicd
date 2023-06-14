def getAgent(deployment) {
    String var = deployment
    if("openshift".equals(var)) {
        return "evol5-openshift";
    }else if("kubernetes-athens".equals(var)){
        return "evol5-athens"
    }else {
        return "evol5-slave";
    }
}

pipeline {
    agent {node {label getAgent("${params.DEPLOYMENT}") == "any" ? "" : getAgent("${params.DEPLOYMENT}")}}

    parameters {
        string(name: 'NETAPP_NAME', defaultValue: '1.0', description: '')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])
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
                        docker image pull dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                        '''
                    }
                }
            }
        }

        stage('Execute the container in the platform (Docker)') {
            steps {
                dir ("${env.WORKSPACE}") {
                        sh '''
                        sudo docker run -d --name netapp -i dockerhub.hi.inet/evolved-5g/${NETAPP_NAME} 
                        '''
                    }
                
            }
        }
        // stage('Test stage') {
        //     steps {
        //         dir ("${env.WORKSPACE}") {
        //             sh '''
        //             sudo docker exec -it netapp curl localhost:8080
        //             '''
        //         }
        //     }
        // }
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
                        docker image tag evolved-5g/validation/dummy-network-application dockerhub.hi.inet/evolved-5g/validation/dummy-network-application:${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/validation/dummy-network-application dockerhub.hi.inet/evolved-5g/validation/dummy-network-application:latest
                        docker image push --all-tags dockerhub.hi.inet/evolved-5g/validation/dummy-network-application
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

