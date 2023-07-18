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
    // String var = deployment
    // if ('openshift'.equals(var)) {
    //     return 'evol5-openshift'
    // }else if ('kubernetes-athens'.equals(var)) {
    //     return 'evol5-athens'
    // }else {
    //     return 'evol5-slave'
    // }
    return 'evol5-slave'
}

def getReportFilename(String netappNameLower) {
    return '000-report-platform-assesment-' + netappNameLower
}

String getArtifactoryUrl(phase) {
    return 'http://artifactory.hi.inet/artifactory/misc-evolved5g/' + phase
}


String getHost(String url) {
    URI uri = new URI(url);
    String host = uri.getHost();
    return host
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
        string(name: 'ELCM_URL', defaultValue: 'http://10.11.23.220:5551', description: 'URL to ELCM')
        string(name: 'ANALYTICS_URL', defaultValue: 'http://10.11.23.220:5003', description: 'URL to Analytics')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
    }

    environment {
        GIT_NETAPP_URL = "${params.GIT_NETAPP_URL}"
        GIT_CICD_BRANCH = "${params.GIT_CICD_BRANCH}"
        GIT_NETAPP_BRANCH = "${params.GIT_NETAPP_BRANCH}"
        VERSION = "${params.VERSION}"
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        STAGE = "${params.STAGE}"
        PATH_DOCKER = getPath("${params.STAGE}")
        PATH_AWS = getPathAWS("${params.STAGE}")
        CHECKPORTS_PATH = 'utils/checkports'
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        DOCKER_PATH = '/usr/src/app'
        ARTIFACTORY_IMAGE_URL = getArtifactoryUrl("${env.PATH_DOCKER}")
        ARTIFACTORY_URL = 'http://artifactory.hi.inet/artifactory/misc-evolved5g/validation'
        REPORT_FILENAME = getReportFilename(NETAPP_NAME_LOWER)
        PDF_GENERATOR_IMAGE_NAME = 'dockerhub.hi.inet/evolved-5g/evolved-pdf-generator'
        PDF_GENERATOR_VERSION = 'latest'
        ELCM_URL = "${params.ELCM_URL}"
        ANALYTICS_URL = "${params.ANALYTICS_URL}"
        ELCM_HOST = getHost(ELCM_URL)
        ANALYTICS_HOST = getHost(ANALYTICS_URL)
    }

    stages {
        stage('Check the connectivity with UMA ECLM') {
            steps {
                dir ("${env.WORKSPACE}") {
                    sh '''
                    ping -c 3 ${ELCM_HOST}
                    ping -c 3 ${ANALYTICS_HOST}
                    '''
                }
            }
        }

        stage('Execute the Experiment in the platform') {
            steps {
                dir ("${env.WORKSPACE}/utils/platform_assesment/") {
                    sh '''
                    pip3 install -r requirements.txt
                    python3 platform_assesment.py ${ELCM_HOST} ${ANALYTICS_HOST} ${REPORT_FILENAME}
                    '''
                }
            }
        }
    }
}