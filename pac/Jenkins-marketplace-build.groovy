def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    }else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    }else {
        return 'evol5-slave'
    }
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }")}}

    parameters {
        string(name: 'VERSION', defaultValue: '1.11', description: '')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'GIT_MARKET_URL', defaultValue: 'https://github.com/EVOLVED-5G/marketplace', description: 'URL of the NEF Github Repository')
        string(name: 'GIT_MARKET_BRANCH', defaultValue: 'main', description: 'Marketplace branch name')
        string(name: 'GIT_MARKET_BLOCKCHAIN_URL', defaultValue: 'https://github.com/EVOLVED-5G/marketplace-blockchain-integration.git', description: 'marketplace Blockchain repository.')
        string(name: 'GIT_MARKET_BLOCKCHAIN_BRANCH', defaultValue: 'main', description: 'Marketplace Blockchain branch name')
        string(name: 'GIT_MARKET_TMF620_URL', defaultValue: 'https://github.com/EVOLVED-5G/marketplace-tmf620-api.git', description: 'marketplace Tmf620 repository.')
        string(name: 'GIT_MARKET_TMF620_BRANCH', defaultValue: 'master', description: 'Marketplace TMF620 branch name')
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma'])
    }

    environment {
        GIT_MARKET_URL = "${params.GIT_MARKET_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_MARKET_BRANCH = "${params.GIT_MARKET_BRANCH}"
        VERSION = "${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = '709233559969'
        MKTP_FOLDER_NAME = 'marketplace'
        MKTP_BLOCKCHAIN_FOLDER_NAME = 'marketplace-blockchain-integration'
        MKTP_TMF620_FOLDER_NAME = 'marketplace-tmf620-integration'
    }
    stages {
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
            }
            steps {
                dir("${env.WORKSPACE}/") {
                    sh '''
                    git clone --single-branch --branch $GIT_MARKET_BRANCH $GIT_MARKET_URL $MKTP_FOLDER_NAME
                    cp -r utils/marketplace/$MKTP_FOLDER_NAME/* $MKTP_FOLDER_NAME/
                    git clone --single-branch --branch $GIT_MARKET_BLOCKCHAIN_BRANCH $GIT_MARKET_BLOCKCHAIN_URL $MKTP_BLOCKCHAIN_FOLDER_NAME
                    cp -r utils/marketplace/$MKTP_BLOCKCHAIN_FOLDER_NAME/* $MKTP_BLOCKCHAIN_FOLDER_NAME/
                    git clone --single-branch --branch $GIT_MARKET_TMF620_BRANCH $GIT_MARKET_TMF620_URL $MKTP_TMF620_FOLDER_NAME
                    cp -r utils/marketplace/$MKTP_TMF620_FOLDER_NAME/* $MKTP_TMF620_FOLDER_NAME/
                    '''
                }
            }
        }
        // These stages are the indicated by Demokritos to install NEF Emulator
        stage('Select docker-compose yaml to use') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_FOLDER_NAME}/") {
                    sh'''
                    mv docker-compose-immutable.yml docker-compose.yml
                    '''
                }
            }
        }
        stage('Configure container Images') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_FOLDER_NAME}/") {
                    sh'''
                        cp .env.example .env
                        uid=$(id `whoami`  | cut -d " " -f1 | cut -d "=" -f2 | cut -d "(" -f1)
                        gid=$(id `whoami`  | cut -d " " -f2 | cut -d "=" -f2 | cut -d "(" -f1)
                        sed -i "s,^[ ]*APP_ENV=.*,APP_ENV=production,g" .env
                        sed -i "s,^[ ]*APP_DEBUG=.*,APP_DEBUG=true,g" .env
                        sed -i "s,^[ ]*APP_KEY=.*,APP_KEY=base64:vAaqm+X4+cBPWSabYrcJMPR3yZZw2ljgLjuTN7d4/J8=,g" .env
                        sed -i "s,^[ ]*APP_URL=.*,APP_URL=https://marketplace-pro.evolved-5g.eu,g" .env
                        sed -i "s,^[ ]*MAIL_HOST=.*,MAIL_HOST=smtp.office365.com,g" .env
                        sed -i "s,^[ ]*MAIL_MAILER=.*,MAIL_MAILER=smtp,g" .env
                        sed -i "s,^[ ]*MAIL_PORT=.*,MAIL_PORT=587,g" .env
                        sed -i "s,^[ ]*MAIL_USERNAME=.*,MAIL_USERNAME=evolved5g-marketplace@maggioli.gr,g" .env
                        sed -i "s,^[ ]*MAIL_PASSWORD=.*,MAIL_PASSWORD=Sor42968,g" .env
                        sed -i "s,^[ ]*MAIL_ENCRYPTION=.*,MAIL_ENCRYPTION=tls,g" .env
                        sed -i "s,^[ ]*MAIL_FROM_ADDRESS=.*,MAIL_FROM_ADDRESS=evolved5g-marketplace@maggioli.gr,g" .env
                        sed -i "s,^[ ]*DEFAULT_ADMIN_USER_PASSWORD_FOR_SEED=.*,DEFAULT_ADMIN_USER_PASSWORD_FOR_SEED=test123,g" .env
                        sed -i "s,^[ ]*DOCKER_USER_ID=.*,DOCKER_USER_ID=$uid,g" .env
                        sed -i "s,^[ ]*DOCKER_GROUP_ID=.*,DOCKER_GROUP_ID=$gid,g" .env
                        sed -i "s,^[ ]*DB_CONNECTION=.*,DB_CONNECTION=mysql,g" .env
                        sed -i "s,^[ ]*DB_HOST=.*,DB_HOST=evolved5g-pilot-marketplace-db,g" .env
                        sed -i "s,^[ ]*DB_PORT=.*,DB_PORT=3306,g" .env
                        sed -i "s,^[ ]*DB_DATABASE=.*,DB_DATABASE=evolved5g_db,g" .env
                        sed -i "s,^[ ]*DB_USERNAME=.*,DB_USERNAME=admin,g" .env
                        sed -i "s,^[ ]*DB_PASSWORD=.*,DB_PASSWORD=secret,g" .env
                        sed -i "s,^[ ]*CRYPTO_SENDER_ADDRESS=.*,CRYPTO_SENDER_ADDRESS=3c4e0e4985fB45D648428a8af4D0A4dFef7A8744,g" .env
                        sed -i "s,^[ ]*CRYPTO_RECEIVER_ADDRESS=.*,CRYPTO_RECEIVER_ADDRESS=3c4e0e4985fB45D648428a8af4D0A4dFef7A8744,g" .env
                        sed -i "s,^[ ]*CRYPTO_WALLET_PRIVATE_KEY=.*,CRYPTO_WALLET_PRIVATE_KEY=9491fa81f7d491e0f6cef6bd7ab1cabe460e29b488874a7137d39de042d29b3f,g" .env
                        sed -i "s,^[ ]*CRYPTO_NETWORK=.*,CRYPTO_NETWORK=goerli,g" .env
                        sed -i "s,^[ ]*FORUM_URL=.*,FORUM_URL=http://evolved5g-marketplace-forum.evolved-5g.gr/,g" .env
                        sed -i "s,^[ ]*CRYPTO_INFURA_PROJECT_ID=.*,CRYPTO_INFURA_PROJECT_ID=48e5260693384e9aa0ea22976749ddf7,g" .env
                        sed -i "s,^[ ]*CRYPTO_SENDER_BASE_URL=.*,CRYPTO_SENDER_BASE_URL=http://evolved5g-blockchain-sender:8000/,g" .env
                        sed -i "s,^[ ]*TM_FORUM_API_BASE_URL=.*,TM_FORUM_API_BASE_URL=http://evolved5g-pilot-tmf-api-container:8080/tmf-api/,g" .env

                        cat .env
                    '''
                }
            }
        }
        stage('Run the containers') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_FOLDER_NAME}/") {
                    sh'''
                    make build
                    '''
                }
            }
        }
        stage('Leave some time avoid docker pull limit at blockchain sender build') {
            steps {
                sleep(time: 5, unit: 'MINUTES')
            }
        }
        stage('build blockchain service') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_BLOCKCHAIN_FOLDER_NAME}/") {
                    sh'''
                    docker-compose up --build -d
                    '''
                }
            }
        }
        stage('Leave some time avoid docker pull limit at tmf620 build') {
            steps {
                sleep(time: 5, unit: 'MINUTES')
            }
        }
        stage('build tmf620 service') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_TMF620_FOLDER_NAME}/") {
                    sh'''
                    docker-compose up --build -d
                    '''
                }
            }
        }
        stage('Run post commands') {
            steps {
                dir("${env.WORKSPACE}/${MKTP_FOLDER_NAME}/") {
                    sh'''
                    docker exec -i evolved5g-pilot-marketplace-laravel bash -c "composer install ; composer dump-autoload ; php artisan key:generate ; php artisan migrate ; php artisan db:seed; php artisan storage:link"
                    docker exec -i evolved5g-pilot-marketplace-laravel bash -c "npm install; npm run production"
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
            dir("${env.WORKSPACE}/${MKTP_TMF620_FOLDER_NAME}/") {
                sh'''
                docker-compose down --volumes
                '''
            }
            dir("${env.WORKSPACE}/${MKTP_BLOCKCHAIN_FOLDER_NAME}/") {
                sh'''
                docker-compose down --volumes
                '''
            }
            dir("${env.WORKSPACE}/${MKTP_FOLDER_NAME}/") {
                sh'''
                docker-compose down --volumes
                '''
            }
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a -f --volumes
            '''
        }
        cleanup {
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

