import java.io.*;
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}


def fileE(String path ) {
    var = """${sh(
                    returnStdout: true,
                    script: "[[ -f ${path} ]] && echo 'True'"
                    )}"""
    return var
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_URL="${params.GIT_URL}"
        GIT_BRANCH="${params.GIT_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = '709233559969'
        NETAPP_FOLDER= netappName("${params.GIT_URL}")
        NETAPP_NAME = NETAPP_FOLDER.toLowerCase()
        DOCKER_VAR = 'false'
        DOCKER_COMPOSE = "${env.WORKSPACE}/${NETAPP_NAME}/docker-compose.yml"
    }

    stages {
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf ${NETAPP_NAME} 
                    mkdir ${NETAPP_NAME} 
                    cd ${NETAPP_NAME} 
                    git clone --single-branch --branch evolved5g $GIT_URL .
                    '''
                }
           }
        }

        stage('Check if there is a docker-compose in the repository') {
            steps {
                script{
                    DOCKER_VAR = fileExists "${env.WORKSPACE}/${NETAPP_NAME}/docker-compose.yml"
                }
                echo "DOCKER VAR is ${DOCKER_VAR}"
                
            }
        }

        stage('Build') {
            when {
                allOf {
                    expression {("${DOCKER_VAR}" == "false")}
                }
            }               
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker build -t ${NETAPP_NAME} .
                    '''
                }
            }
        }
        stage('Build Docker Compose') {
            when {
                allOf {
                    expression {"${DOCKER_VAR}"}
                }
            }  
            steps {
                dir ("${env.WORKSPACE}/${NETAPP_NAME}/") {
                    sh '''
                    docker-compose up --build -d
                    '''
                }
            }
        }
        stage('Modify image name and upload to AWS') {
            when {
                allOf {
                    expression {"${DOCKER_VAR}"}
                }
            }  
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {               
                    script {    
                        def cmd = "docker ps --format '{{.Image}}'"
                        def cmd2 = "docker ps --format '{{.Names}}'"
                        def image = sh(returnStdout: true, script: cmd).trim()
                        def name  = sh(returnStdout: true, script: cmd2).trim()
                        sh '''$(aws ecr get-login --no-include-email)'''
                        [image.tokenize(), name.tokenize()].transpose().each { x ->
                            sh """ docker tag "${x[0]}" ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-"${x[1]}"-${VERSION}.${BUILD_NUMBER} """
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-"${x[1]}"-${VERSION}.${BUILD_NUMBER} """
                        }
                    }
                }
            }
        }
        stage('Publish in AWS - Dockerfile') {
            when {
                allOf {
                    expression {("${DOCKER_VAR}" == "false")}
                }
            } 
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    dir ("${env.WORKSPACE}/iac/terraform/") {
                        sh '''
                        $(aws ecr get-login --no-include-email)
                        docker image tag evolved-5g/${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-${VERSION}.${BUILD_NUMBER}
                        docker image tag evolved-5g/${NETAPP_NAME} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME}-latest
                        '''
                    }    
                }   
            }
        }
        stage('Modify container name to upload Docker-compose to Artifactory') {
            when {
                allOf {
                    expression {"${DOCKER_VAR}"}
                }
            }   
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    script {   
                        sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                        def cmd = "docker ps --format '{{.Image}}'"
                        def cmd2 = "docker ps --format '{{.Names}}'"
                        def image = sh(returnStdout: true, script: cmd).trim()
                        def name  = sh(returnStdout: true, script: cmd2).trim()
                        sh '''$(aws ecr get-login --no-include-email)'''
                        [image.tokenize(), name.tokenize()].transpose().each { x ->
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}-"${x[1]}":${VERSION}.${BUILD_NUMBER} """
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}-"${x[1]}":latest"""
                            sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}-"${x[1]}" """
                        }
                    }
                }               
            }
        }   
        stage('Publish in Artefactory') {
            when {
                allOf {
                    expression {("${DOCKER_VAR}" == "false")}
                }
            } 
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    sh '''
                    docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                    docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:${VERSION}.${BUILD_NUMBER}
                    docker image tag evolved-5g/${NETAPP_NAME} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}:latest
                    docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME}
                    '''
                }
            }
        }
        stage('Cleaning docker images and containers') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker stop $(docker ps -q)
                    docker rmi -f $(docker images -a -q)
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

