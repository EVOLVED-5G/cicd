String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
}

def getAgent(deployment) {
    String var = deployment
    if ('openshift'.equals(var)) {
        return 'evol5-openshift'
    } else if ('kubernetes-athens'.equals(var)) {
        return 'evol5-athens'
    } else if ('kubernetes-cosmote'.equals(var)) {
        return 'evol5-cosmote'
    } else {
        return 'evol5-slave'
    }
}

def getReportFilename(String netappNameLower) {
    return '030-report-licenses-repo-' + netappNameLower
}

pipeline {
    agent { node { label getAgent("${params.DEPLOYMENT }") == 'any' ? '' : getAgent("${params.DEPLOYMENT }") } }

    options {
        timeout(time: 30, unit: 'MINUTES')
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'BUILD_ID', defaultValue: '', description: 'value to identify each execution')
        choice(name: 'STAGE', choices: ['verification', 'validation', 'certification'])
        choice(name: 'DEPLOYMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma', 'kubernetes-cosmote'])
        booleanParam(name: 'REPORTING', defaultValue: true, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        PASSWORD_ARTIFACTORY = credentials('artifactory_credentials')
        STAGE = "${params.STAGE}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        TOKEN = credentials('github_token_cred')
        TOKEN_EVOLVED = credentials('github_token_evolved5g')
        TOKEN_TRIVY = credentials('token_trivy')
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        ARTIFACTORY_URL = "http://artifactory.hi.inet/artifactory/misc-evolved5g/${params.STAGE}"
        DOCKER_PATH = '/usr/src/app'
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
    }

    stages {
        stage('Prepare pdf generator tools') {
            when {
                expression {
                    return REPORTING
                }
            }
            options {
                retry(3)
            }

            steps {
                dir("${env.WORKSPACE}") {
                    withCredentials([usernamePassword(
                    credentialsId: 'docker_pull_cred',
                    usernameVariable: 'USER',
                    passwordVariable: 'PASS'
                )]) {
                        script {
                            try {
                                sh '''
                                docker login --username ${USER} --password ${PASS} dockerhub.hi.inet
                                docker pull ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION}
                                '''
                    } catch (Exception e) {
                                sleep(time:60, unit:'SECONDS')
                                throw e
                            }
                        }
                }
                }
            }
        }
        stage('Get the code!') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(3)
            }
            steps {
                sh '''
                rm -rf ${NETAPP_NAME}
                mkdir ${NETAPP_NAME}
                cd ${NETAPP_NAME}
                git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                '''
            }
        }
        stage('Prepare License Check') {
            options {
                    timeout(time: 10, unit: 'MINUTES')
                    retry(3)
            }
            steps {
                sh '''
                pip3 install licensecheck
                '''
            }
        }

        stage('Vulnerability scan and license checking') {
            options {
                    timeout(time: 60, unit: 'MINUTES')
                    retry(3)
            }
            steps {
                sh '''#!/bin/bash
                cd "${WORKSPACE}/${NETAPP_NAME}"
                FILES=$(find . -name "requirements*.txt")

                using="requirements"
                echo ${#FILES[@]}
                for req in $FILES
                do
                    using+=:$req
                done
                echo $using

                remove=0
                if [ -f pyproject.toml ]; then
                    echo "pyproject.toml file exists"
                else
                    echo "pyproject.toml not exist, generating one for license checking"
                    cp ${WORKSPACE}/utils/licensecheck/pyproject.toml ./
                    remove=1
                fi

                python3 -m licensecheck --using $using --format json --file compliance_${NETAPP_NAME}_"$GIT_COMMIT".report.json

                if [ $remove -eq 1 ];then
                rm pyproject.toml
                fi
                '''
            }
        }

        stage('Upload report to Artifactory') {
            when {
                expression {
                    return REPORTING
                }
            }
            options {
                timeout(time: 10, unit: 'MINUTES')
                retry(3)
            }
            steps {
                dir("${WORKSPACE}/") {
                    sh '''#!/bin/bash

                        # get Commit Information
                        cd $NETAPP_NAME
                        commit=$(git rev-parse HEAD)
                        cd ..

                        python3 utils/report_licensecheck_generator.py --template templates/${STAGE}/step-licensecheck.md.j2 --json ${WORKSPACE}/${NETAPP_NAME}/compliance_${NETAPP_NAME}_"$GIT_COMMIT".report.json --output ${REPORT_FILENAME}.md --repo ${GIT_NETAPP_URL} --branch ${GIT_NETAPP_BRANCH} --commit $commit
                        docker run --rm -v "$WORKSPACE":$DOCKER_PATH ${PDF_GENERATOR_IMAGE_NAME}:${PDF_GENERATOR_VERSION} markdown-pdf -f A4 -b 1cm -s $DOCKER_PATH/utils/docker_generate_pdf/style.css -o $DOCKER_PATH/${REPORT_FILENAME}.pdf $DOCKER_PATH/${REPORT_FILENAME}.md
                        declare -a files=("md" "pdf")

                        for x in "${files[@]}"
                            do
                                report_file="${REPORT_FILENAME}.$x"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                    --data-binary @"$report_file" \
                                    "$url"
                            done
                    '''
                }
            }
        }
    }
    post {
        always {
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Evolved 5G - ${NETAPP_NAME} - Compliance Analysis Result ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
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
