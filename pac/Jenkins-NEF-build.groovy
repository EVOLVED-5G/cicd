pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_NEF_URL', defaultValue: 'https://github.com/EVOLVED-5G/NEF_emulator', description: 'URL of the NEF Github Repository')
        string(name: 'GIT_NEF_BRANCH', defaultValue: 'main', description: 'NEF branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_NEF_URL="${params.GIT_NEF_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NEF_BRANCH="${params.GIT_NEF_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = '709233559969'
        FOLDER_NAME = "NEF_emulator"
    }
    stages {
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
                }
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    mkdir $FOLDER_NAME 
                    cd $FOLDER_NAME
                    git clone --single-branch --branch $GIT_NEF_BRANCH $GIT_NEF_URL .
                    '''
                }
           }
        }
        // These stages are the indicated by Demokritos to install NEF Emulator
        stage('Create local .env file') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    make prepare-dev-env


                    EXTERNAL_NET=false
                    '''
                }
            }
        }
        stage('Build container Images') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    make build-no-cache
                    '''
                }
            }
        }
        stage('Run the containers') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    make debug-upd
                    '''
                }
            }
        }
        stage('Modify image name and upload to AWS') {   
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {               
                    script {    
                        def cmd = "docker ps --format '{{.Image}}'"
                        def cmd2 = "docker ps --format '{{.Names}}'"
                        def image = sh(returnStdout: true, script: cmd).trim()
                        def name  = sh(returnStdout: true, script: cmd2).trim()
                        sh '''$(aws ecr get-login --no-include-email)'''
                        [image.tokenize(), name.tokenize()].transpose().each { x ->
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${x[1]}-${VERSION}"""
                            sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${x[1]}-latest"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${x[1]}-${VERSION}"""
                            sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${x[1]}-latest"""
                        }
                    }
                }
            }
        }

        stage('Modify container name to upload Docker-compose to Artifactory') { 
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
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/"${x[1]}":${VERSION}.${BUILD_NUMBER} """
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/"${x[1]}":latest"""
                            sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/"${x[1]}" """
                        }
                    }
                }               
            }
        }   
    }
    post {
        always {
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a -f --volumes
            sudo rm -rf $WORKSPACE/$FOLDER_NAME/
            '''
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

