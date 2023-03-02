pipeline {
    agent { node {label 'evol5-openshift'}  }

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
                    retry(1)
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
                    mv docker-compose-immutable.yml docker-compose.yml
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
                      sudo sed -i "s,APP_ENV=local,APP_ENV=production,g" .env
                      sudo sed -i "s,APP_KEY=,APP_KEY=base64:vAaqm+X4+cBPWSabYrcJMPR3yZZw2ljgLjuTN7d4/J8=,g" .env
                      sudo sed -i "s,APP_URL=http://localhost,APP_URL=https://marketplace-pro.evolved-5g.eu,g" .env
                      sudo sed -i "s,MAIL_MAILER=smtp,MAIL_MAILER=smtp,g" .env
                      sudo sed -i "s,MAIL_HOST=mailhog,DOCKER_USER_ID=$uid,g" .env
                      sudo sed -i "s,MAIL_MAILER=smtp,DOCKER_USER_ID=$uid,g" .env
                      sudo sed -i "s,MAIL_PORT=1025,MAIL_PORT=587,g" .env
                      sudo sed -i "s,MAIL_USERNAME=null,MAIL_USERNAME=evolved5g-marketplace@maggioli.gr,g" .env
                      sudo sed -i "s,MAIL_PASSWORD=null,MAIL_PASSWORD=Sor42968,g" .env
                      sudo sed -i "s,MAIL_ENCRYPTION=null,MAIL_ENCRYPTION=tls,g" .env
                      sudo sed -i "s,MAIL_FROM_ADDRESS=null,MAIL_FROM_ADDRESS=evolved5g-marketplace@maggioli.gr,g" .env
                      sudo sed -i "s,DEFAULT_ADMIN_USER_PASSWORD_FOR_SEED=,DEFAULT_ADMIN_USER_PASSWORD_FOR_SEED=test123,g" .env
                      sudo sed -i "s,# NON-DOCKER Installation configuration:,MIX_APP_ENV=${APP_ENV},g" .env
                      sudo sed -i "s,DOCKER_USER_ID=1000,DOCKER_USER_ID=$uid,g" .env 
                      sudo sed -i "s,DOCKER_GROUP_ID=1000,DOCKER_GROUP_ID=$gid,g" .env
                      sudo sed -i "s,DB_CONNECTION=mysql,DB_CONNECTION=mysql,g" .env 
                      sudo sed -i "s,DB_HOST=127.0.0.1,DB_HOST=mktp-db,g" .env
                      sudo sed -i "s,DB_PORT=3306,DB_PORT=3306,g" .env 
                      sudo sed -i "s,DB_DATABASE=,DB_DATABASE=evolved5g_db,g" .env
                      sudo sed -i "s,DB_USERNAME=,DB_USERNAME=admin,g" .env 
                      sudo sed -i "s,DB_PASSWORD=,DB_PASSWORD=secret,g" .env 
                      sudo sed -i "s,CRYPTO_SENDER_ADDRESS=,CRYPTO_SENDER_ADDRESS=72d6019506866a2ca1dc6bfd0f2f65ac0953ba1e,g" .env
                      sudo sed -i "s,CRYPTO_RECEIVER_ADDRESS=,CRYPTO_RECEIVER_ADDRESS=72d6019506866a2ca1dc6bfd0f2f65ac0953ba1e,g" .env    
                      sudo sed -i "s,CRYPTO_WALLET_PRIVATE_KEY=,CRYPTO_WALLET_PRIVATE_KEY=36b134f2175656978eeb7821c19a2213d6716a6de2ea297dbf21e100175632d4,g" .env    
                      sudo sed -i "s,CRYPTO_NETWORK=,CRYPTO_NETWORK=goerli,g" .env
                      sudo sed -i "s,FORUM_URL=https://forum.evolved-5g.eu/,FORUM_URL=http://evolved5g-marketplace-forum.evolved-5g.gr/,g" .env
                      sudo sed -i "s,CRYPTO_INFURA_PROJECT_ID=,CRYPTO_INFURA_PROJECT_ID=48e5260693384e9aa0ea22976749ddf7,g" .env    
                      sudo sed -i "s,#CRYPTO_SENDER_BASE_URL=http://localhost:8000/,NETAPP_FINGERPRINT_BASE_URL=http://artifactory.hi.inet/artifactory/misc-evolved5g/certification/,g" .env    
                      sudo sed -i "s,#TM_FORUM_API_BASE_URL=http://localhost:8080/tmf-api/,MIX_NETAPP_OPEN_REPOSITORY_DOCKER_IMAGE_BASE_URL=https://dockerhub.hi.inet/evolved5g/certification/,g" .env
                    '''
                }
            }
        }
        stage('Run the containers') {
            steps {
                dir ("${env.WORKSPACE}/${FOLDER_NAME}/"){
                    sh'''
                    make build
                    docker exec -i evolved5g_pilot_marketplace_laravel bash -c "composer install ; composer dump-autoload ; php artisan key:generate ; php artisan migrate ; php artisan db:seed; php artisan storage:link"
                    docker exec -i evolved5g_pilot_marketplace_laravel bash -c "npm install; npm run prod"
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
                        sh ''' echo ${ARTIFACTORY_CREDENTIALS} | docker login --username ${ARTIFACTORY_USER} --password-stdin dockerhub.hi.inet '''
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
    }
    post {
        always {
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a -f --volumes
            sudo rm -rf $WORKSPACE
            '''
        }
        cleanup{
            /* clean up our workspace */
            // deleteDir()
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

