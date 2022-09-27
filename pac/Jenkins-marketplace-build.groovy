pipeline {
    agent { node {label 'evol5-slave'}  }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_MARKET_URL', defaultValue: 'https://github.com/EVOLVED-5G/marketplace', description: 'URL of the NEF Github Repository')
        string(name: 'GIT_MARKET_BRANCH', defaultValue: 'main', description: 'Marketplace branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
    }

    environment {
        GIT_MARKET_URL="${params.GIT_MARKET_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_MARKET_BRANCH="${params.GIT_MARKET_BRANCH}"
        VERSION="${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = '709233559969'
        FOLDER_NAME = "marketplace"
    }
    stages {
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(2)
                }
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    mkdir $FOLDER_NAME 
                    cd $FOLDER_NAME
                    git clone --single-branch --branch $GIT_MARKET_BRANCH $GIT_MARKET_URL .
                    '''
                }
           }
        }
        // These stages are the indicated by Demokritos to install NEF Emulator
        stage('Create local .env file') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    cp .env.example .env
                    '''
                }
            }
        }
        stage('Configure container Images') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                      cp .env.example .env
                      uid=$(id `whoami`  | cut -d " " -f1 | cut -d "=" -f2 | cut -d "(" -f1)
                      gid=$(id `whoami`  | cut -d " " -f2 | cut -d "=" -f2 | cut -d "(" -f1)
                      sudo sed -i "s,DOCKER_USER_ID=1000,DOCKER_USER_ID=$uid,g" .env 
                      sudo sed -i "s,DOCKER_GROUP_ID=1000,DOCKER_GROUP_ID=$gid,g" .env
                      sudo sed -i "s,DB_CONNECTION=mysql,DB_CONNECTION=mysql,g" .env 
                      sudo sed -i "s,DB_HOST=127.0.0.1,DB_HOST=db,g" .env
                      sudo sed -i "s,DB_PORT=3306,DB_PORT=3306,g" .env 
                      sudo sed -i "s,DB_DATABASE=,DB_DATABASE=evolved5g_db,g" .env
                      sudo sed -i "s,DB_USERNAME=,DB_USERNAME=admin,g" .env 
                      sudo sed -i "s,DB_PASSWORD=,DB_PASSWORD=secret,g" .env                         
                    '''
                }
            }
        }
        stage('Run the containers') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    make build
                    make composer_install
                    make npm_install
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
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/marketplace/"${x[1]}":${VERSION}"""
                            sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/marketplace/"${x[1]}":latest"""
                            sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/marketplace/"${x[1]}" """
                        }
                    }
                }               
            }
        }   
        stage('Cleaning docker images and containers') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker stop $(docker ps -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/
                    '''
                }
            }
        }
    }
// post {
//     cleanup{
//         /* clean up our workspace */
//         deleteDir()
//         /* clean up tmp directory */
//         dir("${env.workspace}@tmp") {
//             deleteDir()
//         }
//         /* clean up script directory */
//         dir("${env.workspace}@script") {
//             deleteDir()
//         }
//     }
// }
}

