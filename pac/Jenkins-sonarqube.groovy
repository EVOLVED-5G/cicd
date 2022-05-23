/*
NTH: Create library with helper methods
*/
String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent { node {label 'evol5-openshift'}  }

    parameters {
        //string(name: 'VERSION', defaultValue: '1.0', description: '')
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        GIT_NETAPP_URL="${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH="${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH="${params.GIT_NETAPP_BRANCH}"
        SCANNERHOME = tool 'Sonar Scanner 5';
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}").toLowerCase()
        SQ_TOKEN=credentials('SONARQUBE_TOKEN')
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {
        stage('Get the code!') {
            steps {
                dir ("${env.WORKSPACE}/") {
                    sh '''
                    rm -rf $NETAPP_NAME
                    mkdir $NETAPP_NAME
                    cd $NETAPP_NAME
                    git clone --single-branch --branch $GIT_NETAPP_BRANCH $GIT_NETAPP_URL .
                    '''
                }
           }
        }

        //TODO: Create a project for each NETAPP
        stage('SonarQube Analysis and Wait for Quality Gate') {
            steps {
                 dir ("${WORKSPACE}/") {
                    withSonarQubeEnv('Evol5-SonarQube') {
                        sh '''
                            ${SCANNERHOME}/bin/sonar-scanner -X \
                                -Dsonar.projectKey=Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH} \
                                -Dsonar.projectBaseDir=${WORKSPACE}/${NETAPP_NAME}/src/ \
                                -Dsonar.host.url=http://195.235.92.134:9000  \
                                -Dsonar.login=${SQ_TOKEN} \
                                -Dsonar.projectName=Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH} \
                                -Dsonar.language=python \
                                -Dsonar.sourceEncoding=UTF-8 \
                        '''
                    }
                    script {
                        def qg = waitForQualityGate()
                        if (qg.status != 'OK') {
                            error "Pipeline aborted due to quality gate failure: ${qg.status}"
                            //unstable("There are Checkstyle issues")
                        }
                    }
                }
            }
        }

        stage('Get SonarQube Report') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''
                    sleep 25
                    curl -u $SQ_TOKEN -X GET -H 'Accept: application/json' http://195.235.92.134:9000/api/qualitygates/project_status\\?projectKey\\=Evolved5g-${NETAPP_NAME}-${GIT_NETAPP_BRANCH} > report-sq-${NETAPP_NAME}.json
                    '''
                    script {
                        def json = readJSON file:'report.json'
                        echo "${json.projectStatus.status}"
                    }
                }
            }
        }

        stage('Upload report to Artifactory') {
            when {
                expression {
                    return REPORTING;
                }
            }
            steps {
                 dir ("${WORKSPACE}/") {
                    sh '''
                    report_file='report-sq-${NETAPP_NAME}.json'
                    url="$ARTIFACTORY_URL//$report_file"

                    curl -v -f -i -X PUT -u "$ARTIFACTORY_CRED" \
                        --data-binary @"$report_file" \
                        "$url"
                    '''
                }
            }
        }

    }
    post {
        always {
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                to: "a.molina@telefonica.com",
                replyTo: "no-reply@tid.es",
                recipientProviders: [[$class: 'CulpritsRecipientProvider']]
        }
    }
}

