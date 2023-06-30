String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

String getPathAWS(deployment) {
    String var = deployment
    if ('verification'.equals(var)) {
        return ''
    }else if ('validation'.equals(var)) {
        return 'validation'
    }else {
        return 'certification'
    }
}

String getPath(deployment) {
    String var = deployment
    if ('verification'.equals(var)) {
        return ''
    }else if ('validation'.equals(var)) {
        return 'validation/'
    }else {
        return 'certification/'
    }
}

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

def getReportFilename(String netappNameLower) {
    return '004-report-build-' + netappNameLower
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }
    options {
        retry(1)
    }

    parameters {
        string(name: 'VERSION', defaultValue: '1.0', description: 'Version of NetworkApp')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma'])
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        VERSION = "${params.VERSION}"
        AWS_DEFAULT_REGION = 'eu-central-1'
        AWS_ACCOUNT_ID = credentials('AWS_ACCOUNT_NUMBER')
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        DOCKER_VAR = false
        PATH_DOCKER = getPath("${params.STAGE}")
        PATH_AWS = getPathAWS("${params.STAGE}")
        CHECKPORTS_PATH = 'utils/checkports'
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        DOCKER_PATH = '/usr/src/app'
        ARTIFACTORY_URL = 'http://artifactory.hi.inet/artifactory/misc-evolved5g/validation'
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
    }
    stages {
        stage('Clean workspace') {
            steps {
                catchError(buildResult: 'SUCCESS', stageResult: 'UNSTABLE') {
                    sh '''
                    docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
                    docker system prune -a -f --volumes
                    sudo rm -rf $WORKSPACE/$NETAPP_NAME_LOWER/
                    docker network create services_default
                    echo $PATH_AWS
                    '''
                }
            }
        }
        stage('Prepare pdf generator tools') {
            when {
                expression {
                    return REPORTING
                }
            }
            options {
                retry(2)
            }

            steps {
                dir("${env.WORKSPACE}") {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker_pull_cred',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                        sh '''
                        docker login --username ${USER} --password ${PASS} dockerhub.hi.inet
                        docker pull ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION}
                        '''
                }
                }
            }
        }
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(1)
            }
            steps {
                dir("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME_LOWER
                    mkdir $NETAPP_NAME_LOWER
                    cd $NETAPP_NAME_LOWER
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    '''
                }
            }
        }
        stage('Check if there is a docker-compose in the repository') {
            steps {
                script {
                    DOCKER_VAR = fileExists "${env.WORKSPACE}/${NETAPP_NAME_LOWER}/docker-compose.yml"
                    if (DOCKER_VAR == false) {
                        DOCKER_VAR = fileExists "${env.WORKSPACE}/${NETAPP_NAME_LOWER}/docker-compose.yaml"
                    }
                }
                echo "env DOCKER VAR is ${DOCKER_VAR}"
            }
        }
        //NICE TO HAVE: Makefile to encapsulate docker and docker-compose commands
        stage('Build') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir("${env.WORKSPACE}/${NETAPP_NAME_LOWER}/") {
                        sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                        sh '''
                    docker build -t ${NETAPP_NAME_LOWER} .
                    container_id=$(docker run -d -P ${NETAPP_NAME_LOWER})
                    sleep 10
                    cd ..
                    docker ps|grep ${NETAPP_NAME_LOWER} || echo "Docker exited"
                    docker ps|grep ${NETAPP_NAME_LOWER} || docker logs $container_id
                    docker ps|grep ${NETAPP_NAME_LOWER} || docker logs $container_id > ${NETAPP_NAME_LOWER}-build-runtime_error.log 2>&1
                    docker ps|grep ${NETAPP_NAME_LOWER} || echo '{"result":false}' | jq . > ${REPORT_FILENAME}.json
                    docker ps|grep ${NETAPP_NAME_LOWER} || exit 1
                    '''
                    }
                }
            }
        }
        stage('Build Docker Compose') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    dir("${env.WORKSPACE}/${NETAPP_NAME_LOWER}/") {
                        sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                        sh '''
                    docker network create demo-network
                    make run-dev || docker-compose up --build --force-recreate -d
                    '''
                    }
                }
            }
        }
        //Check Ports on running images
        stage('Check Ports of images generated') {
            steps {
                dir("${env.WORKSPACE}/") {
                    sh '''
                    pip install -r ${CHECKPORTS_PATH}/requirements.txt
                    python3 ${CHECKPORTS_PATH}/checkportscicd.py $GIT_NETAPP_BRANCH $GIT_NETAPP_URL ${NETAPP_NAME_LOWER} ${REPORT_FILENAME}.json
                    '''
                }
            }
        }
        //----
        stage('Modify image name and upload to AWS') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    retry(2) {
                        script {
                            def cmd = "docker ps --format '{{.Image}}'"
                            def cmd2 = "docker ps --format '{{.Names}}'"
                            def image = sh(returnStdout: true, script: cmd).trim()
                            def name  = sh(returnStdout: true, script: cmd2).trim()
                            sh '''$(aws ecr get-login --no-include-email)'''
                            [image.tokenize(), name.tokenize()].transpose().each { x ->
                                if (env.PATH_AWS != null) {
                                    sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                    sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${x[0]} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${x[0]} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                }
                                else {
                                    sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ docker tag ${x[0]} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                    sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${x[0]} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-${VERSION}"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${x[0]} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${x[1]}-latest"""
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Publish in AWS - Dockerfile') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([[$class: 'AmazonWebServicesCredentialsBinding', credentialsId: 'evolved5g-push', accessKeyVariable: 'AWS_ACCESS_KEY_ID', secretKeyVariable: 'AWS_SECRET_ACCESS_KEY']]) {
                    retry(2) {
                        sh '''
                        $(aws ecr get-login --no-include-email)
                        if [[ -n ${PATH_AWS} ]]
                        then
                            docker image tag ${NETAPP_NAME_LOWER} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${VERSION}
                            docker image tag ${NETAPP_NAME_LOWER} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-latest
                            docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-latest
                            docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g${env.PATH_AWS}:${NETAPP_NAME_LOWER}-latest
                        else
                            docker image tag ${NETAPP_NAME_LOWER} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${VERSION}
                            docker image tag ${NETAPP_NAME_LOWER} ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-latest
                            docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-latest
                            docker image push ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} aws_images ${AWS_ACCOUNT_ID}.dkr.ecr.${AWS_DEFAULT_REGION}.amazonaws.com/evolved5g:${NETAPP_NAME_LOWER}-latest
                        fi
                        '''
                    }
                }
            }
        }
        stage('Modify container name to upload Docker-compose to Artifactory') {
            when {
                expression {
                    return "${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(2) {
                        script {
                            sh ''' docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet '''
                            def cmd = "docker ps --format '{{.Image}}'"
                            def cmd2 = "docker ps --format '{{.Names}}'"
                            def image = sh(returnStdout: true, script: cmd).trim()
                            def name  = sh(returnStdout: true, script: cmd2).trim()
                            sh '''$(aws ecr get-login --no-include-email)'''
                            [image.tokenize(), name.tokenize()].transpose().each { x ->
                                if (env.PATH_DOCKER != null) {
                                    sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":${VERSION}"""
                                    sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":latest"""
                                    sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}" """
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json "${x[0]}" docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":${VERSION}"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json "${x[0]}" docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":latest"""
                                } else {
                                    sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":${VERSION}"""
                                    sh """ docker tag "${x[0]}" dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":latest"""
                                    sh """ docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}" """
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json "${x[0]}" docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":${VERSION}"""
                                    sh """ python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json "${x[0]}" docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}/"${NETAPP_NAME_LOWER}-${x[1]}":latest"""
                                }
                            }
                        }
                    }
                }
            }
        }
        stage('Publish in Artefactory') {
            when {
                expression {
                    return !"${DOCKER_VAR}".toBoolean()
                }
            }
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker_pull_cred', usernameVariable: 'ARTIFACTORY_USER', passwordVariable: 'ARTIFACTORY_CREDENTIALS')]) {
                    retry(2) {
                        sh '''
                        docker login --username ${ARTIFACTORY_USER} --password "${ARTIFACTORY_CREDENTIALS}" dockerhub.hi.inet
                        if [[ -n ${PATH_DOCKER} ]]
                        then
                            docker image tag ${NETAPP_NAME_LOWER} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}:${VERSION}
                            docker image tag ${NETAPP_NAME_LOWER} dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}:${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} docker_hub_images dockerhub.hi.inet/evolved-5g/${PATH_DOCKER}${NETAPP_NAME_LOWER}:latest
                        else
                            docker image tag ${NETAPP_NAME_LOWER} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}:${VERSION}
                            docker image tag ${NETAPP_NAME_LOWER} dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}:latest
                            docker image push --all-tags dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}:${VERSION}
                            python3 utils/helpers/add_image_json.py ${REPORT_FILENAME}.json ${NETAPP_NAME_LOWER} docker_hub_images dockerhub.hi.inet/evolved-5g/${NETAPP_NAME_LOWER}:latest
                        fi

                        '''
                    }
                }
            }
        }
    }
    post {
        always {
            retry(2) {
                script {
                    if ("${params.REPORTING}".toBoolean() == true) {
                        sh '''#!/bin/bash
                        if [ -f "${REPORT_FILENAME}.json" ]; then
                            echo "$FILE exists."
                        
                            # get Commit Information
                            cd $NETAPP_NAME_LOWER
                            commit=$(git rev-parse HEAD)
                            cd ..

                            urlT=https://github.com/EVOLVED-5G/$NETAPP_NAME_LOWER/wiki/Telefonica-Evolved5g-$NETAPP_NAME_LOWER
                            versionT=${VERSION}

                            python3 utils/report_generator.py --template templates/step-build.md.j2 --json ${REPORT_FILENAME}.json --output $REPORT_FILENAME.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit --version $versionT --url $urlT --name $NETAPP_NAME --logs ${NETAPP_NAME_LOWER}-build-runtime_error.log
                            docker run -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/$REPORT_FILENAME.pdf $DOCKER_PATH/$REPORT_FILENAME.md
                            declare -a files=("json" "md" "pdf")

                            for x in "${files[@]}"
                                do
                                    report_file="${REPORT_FILENAME}.$x"
                                    url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                                    curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                        --data-binary @"$report_file" \
                                        "$url"
                                done
                        else
                            echo "No report file generated"
                        fi
                        '''
                    }
                }
            }
            sh '''
            docker ps -a -q | xargs --no-run-if-empty docker stop $(docker ps -a -q)
            docker system prune -a -f --volumes
            sudo rm -rf $WORKSPACE/$NETAPP_NAME_LOWER/
            '''
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: 'jenkins-evolved5G@tid.es',
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
                }
            }
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
