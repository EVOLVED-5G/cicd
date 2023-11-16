import groovy.json.JsonOutput

String useOf5gApis = 'apis_5g'
def buildResults = [:]
buildResults['steps'] = [:]
buildResults['tests_ok'] = true
buildResults['tests_executed'] = false
buildResults[useOf5gApis] = []

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

def getHttpPort(deployment) {
    String var = deployment
    if ('kubernetes-athens'.equals(var)) {
        return '30048'
    }else {
        return '80'
    }
}

def getHttpsPort(deployment) {
    String var = deployment
    if ('kubernetes-athens'.equals(var)) {
        return '30548'
    }else {
        return '443'
    }
}

String getArtifactoryUrl(phase) {
    return 'http://artifactory.hi.inet/artifactory/misc-evolved5g/' + phase
}

String getPhase() {
    return 'Certification'
}

String getFingerprintFilename() {
    return 'fingerprint.json'
}

def getDeployReportFilename(String netappNameLower) {
    return '019-report-deploy-' + netappNameLower
}


def umaValidation(env){
    def jobBuild = build job: '/003-NETAPPS/003-Helpers/000-UMA Validation', wait: true, propagate: true,
        parameters: [
            string(name: 'VERSION', value: String.valueOf(env.VERSION_NETAPP)),
            string(name: 'GIT_NETAPP_URL', value: String.valueOf(env.GIT_NETAPP_URL)),
            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(env.GIT_NETAPP_BRANCH)),
            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(env.GIT_CICD_BRANCH)),
            string(name: 'BUILD_ID', value: String.valueOf(env.BUILD_NUMBER)),
            string(name: 'STAGE', value: String.valueOf(env.PHASE_LOWER)),
            string(name: 'DEPLOYMENT', value: String.valueOf(env.ENVIRONMENT)),
            booleanParam(name: 'REPORTING', value: String.valueOf(env.REPORTING)),
            booleanParam(name: 'SEND_DEV_MAIL', value: false)
            ]
    return jobBuild
}

def obtainNetworkAppKPIs(env){
    def jobBuild = build job: '/003-NETAPPS/003-Helpers/000-Obtain Network App KPIs', wait: true, propagate: false,
        parameters: [
            string(name: 'VERSION', value: String.valueOf(env.VERSION_NETAPP)),
            string(name: 'GIT_NETAPP_URL', value: String.valueOf(env.GIT_NETAPP_URL)),
            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(env.GIT_NETAPP_BRANCH)),
            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(env.GIT_CICD_BRANCH)),
            string(name: 'BUILD_ID', value: String.valueOf(env.BUILD_NUMBER)),
            string(name: 'STAGE', value: String.valueOf(env.PHASE_LOWER)),
            string(name: 'DEPLOYMENT', value: String.valueOf(env.ENVIRONMENT)),
            booleanParam(name: 'REPORTING', value: String.valueOf(env.REPORTING)),
            booleanParam(name: 'SEND_DEV_MAIL', value: false)
            ]
    return jobBuild
}

def staticCodeAnalysis(env) {
    def jobBuild = build job: '/003-NETAPPS/003-Helpers/001-Static Code Analysis', wait: true, propagate: true,
        parameters: [
            string(name: 'GIT_NETAPP_URL', value: String.valueOf(env.GIT_NETAPP_URL)),
            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(env.GIT_NETAPP_BRANCH)),
            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(env.GIT_CICD_BRANCH)),
            string(name: 'STAGE', value: String.valueOf(env.PHASE_LOWER)),
            string(name: 'BUILD_ID', value: String.valueOf(env.BUILD_NUMBER)),
            booleanParam(name: 'REPORTING', value: String.valueOf(env.REPORTING)),
            booleanParam(name: 'SEND_DEV_MAIL', value: false)
            ]
    return jobBuild
}

def capifValidation(env) {
     def jobBuild = build job: '003-NETAPPS/003-Helpers/021-CAPIF Validation Tests', wait: true, propagate: false,
        parameters: [
            string(name: 'BRANCH_NAME', value: String.valueOf(env.CAPIF_TESTS_BRANCH)),
            booleanParam(name: 'RUN_LOCAL_CAPIF', value: false),
            string(name: 'CAPIF_HOSTNAME', value: String.valueOf(env.HOSTNAME_CAPIF)),
            string(name: 'CAPIF_PORT', value: String.valueOf(env.CAPIF_PORT)),
            string(name: 'CAPIF_TLS_PORT', value: String.valueOf(env.CAPIF_TLS_PORT)),
            string(name: 'DEPLOYMENT', value: String.valueOf(env.ENVIRONMENT)),
            string(name: 'BUILD_ID', value: String.valueOf(env.BUILD_NUMBER)),
            string(name: 'STAGE', value: String.valueOf(env.PHASE_LOWER)),
            string(name: 'VERSION', value: String.valueOf(env.VERSION_NETAPP)),
            string(name: 'GIT_NETAPP_URL', value: String.valueOf(env.GIT_NETAPP_URL)),
            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(env.GIT_CICD_BRANCH))
            ]
    return jobBuild
}

def nefValidation(env) {
    def jobBuild = build job: '003-NETAPPS/003-Helpers/022-NEF Validation Tests', wait: true, propagate: false,
        parameters: [
            string(name: 'CAPIF_HOST', value: String.valueOf(env.HOSTNAME_CAPIF)),
            string(name: 'CAPIF_HTTP_PORT', value: String.valueOf(env.CAPIF_PORT)),
            string(name: 'CAPIF_HTTPS_PORT', value: String.valueOf(env.CAPIF_TLS_PORT)),
            string(name: 'NEF_API_HOSTNAME', value: String.valueOf(env.NEF_API_HOSTNAME)),
            string(name: 'DEPLOYMENT', value: String.valueOf(env.ENVIRONMENT)),
            string(name: 'BUILD_ID', value: String.valueOf(env.BUILD_NUMBER)),
            string(name: 'STAGE', value: String.valueOf(env.PHASE_LOWER)),
            string(name: 'VERSION', value: String.valueOf(env.VERSION_NETAPP)),
            string(name: 'GIT_NETAPP_URL', value: String.valueOf(env.GIT_NETAPP_URL)),
            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(env.GIT_CICD_BRANCH))]
    return jobBuild
}

def step_platform_assessment = 'platform-assessment'
def step_static_code_analysis = 'source-code-static-analysis'
def step_security_scan_code = 'source-code-security-analysis'
def step_security_scan_secrets = 'source-code-secrets-leakage'
def step_build = 'network-app-build-and-port-check'
def step_network_app_kpis = 'network-app-kpis'
def step_security_scan_docker_images = 'image-security-analysis'
def step_deploy_capif_nef_netapp = 'deploy-network-app'
def step_use_of_5g_apis = 'use-of-5g-apis'
def step_nef_services_apis = 'nef-services-apis'
def step_destroy_network_app = 'destroy-netapp'
def step_destroy_nef = 'destroy-nef'
def step_destroy_capif = 'destroy-capif'
def step_open_source_licenses_report = 'open-source-licenses-report'

def initial_status = 'SKIPPED'
def not_report = 'NOT_REPORT'
def aborted = false

pipeline {
    agent { node { label getAgent("${params.ENVIRONMENT }") == 'any' ? '' : getAgent("${params.ENVIRONMENT }") } }
    options {
        timeout(time: 120, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-network-application', description: '(Required) URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'network-app.apps.ocp-epg.hi.inet', description: 'Hostname to NetworkApp')
        string(name: 'VERSION_NETAPP', defaultValue: '4.0', description: 'Version Network App')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS_NETAPP', defaultValue: '1', description: 'Number of NetworkApp pods to run')
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'VERSION_CAPIF', defaultValue: '3.1.2', description: 'Version CAPIF')
        string(name: 'CAPIF_TESTS_BRANCH', defaultValue: 'develop', description: 'Branch to use for CAPIF tests')
        string(name: 'RELEASE_CAPIF', defaultValue: 'capif', description: 'Helm Release name to CAPIF')
        string(name: 'RELEASE_NEF', defaultValue: 'nef', description: 'Helm Release name to NEF')
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        string(name: 'RELEASE_TSN', defaultValue: 'tsn', description: 'Helm Release name to TSN')
        string(name: 'HOSTNAME_TSN', defaultValue: 'tsn.apps.ocp-epg.hi.inet', description: 'Hostname to TSN')
        choice(name: 'ENVIRONMENT', choices: ['kubernetes-uma', 'kubernetes-athens', 'kubernetes-cosmote', 'openshift'])
        booleanParam(name: 'REPORTING', defaultValue: true, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: false, description: 'Send mail to Developers')
        booleanParam(name: 'OVERWRITE_FINGERPRINT', defaultValue: false, description: 'Overwrite artifactory fingerprint if present')
        string(name: 'EMAILS', defaultValue: '', description: 'Nettaps emails in order to notify final report')
    }

    environment {
        PHASE = getPhase()
        PHASE_LOWER = PHASE.toLowerCase()
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        ARTIFACTORY_URL = getArtifactoryUrl("${env.PHASE_LOWER}")
        RELEASE_CAPIF = "${params.RELEASE_CAPIF}"
        HOSTNAME_CAPIF = "${params.HOSTNAME_CAPIF}"
        RELEASE_NEF = "${params.RELEASE_NEF}"
        HOSTNAME_NEF = "${params.HOSTNAME_NEF}"
        RELEASE_TSN = "${params.RELEASE_TSN}"
        HOSTNAME_TSN = "${params.HOSTNAME_TSN}"
        VERSION_NETAPP = "${params.VERSION_NETAPP}"
        emails = "${params.EMAILS}".trim()
        CAPIF_PORT = getHttpPort("${params.ENVIRONMENT}")
        CAPIF_TLS_PORT = getHttpsPort("${params.ENVIRONMENT}")
        FINGERPRINT_FILENAME = getFingerprintFilename()
        DEPLOY_REPORT_FILENAME = getDeployReportFilename(NETAPP_NAME_LOWER)
        NEF_API_HOSTNAME = "https://${params.HOSTNAME_NEF}:${CAPIF_TLS_PORT}"
        OVERWRITE_FINGERPRINT = "${params.OVERWRITE_FINGERPRINT}"
    }

    stages {
        stage('Initialize Local Variables') {
            steps {
                script {
                    echo 'Setting local variables'
                    step_deploy_capif_nef_netapp = 'deploy-' + "${NETAPP_NAME_LOWER}" + '-network-app'
                    buildResults['steps'][step_platform_assessment] = initial_status
                    buildResults['steps'][step_static_code_analysis] = initial_status
                    buildResults['steps'][step_security_scan_code] = initial_status
                    buildResults['steps'][step_security_scan_secrets] = initial_status
                    buildResults['steps'][step_build] = initial_status
                    buildResults['steps'][step_security_scan_docker_images] = initial_status
                    buildResults['steps'][step_deploy_capif_nef_netapp] = initial_status
                    buildResults['steps'][step_use_of_5g_apis] = initial_status
                    buildResults['steps'][step_network_app_kpis] = initial_status
                    buildResults['steps'][step_nef_services_apis] = not_report
                    buildResults['steps'][step_open_source_licenses_report] = initial_status
                }
            }
        }
        stage('Certification: Performance Assesment at platform') {
            steps {
                retry(2) {
                    script {
                        def step_name = step_platform_assessment
                        buildResults['steps'][step_name] = 'FAILURE'
                        def jobBuild = umaValidation(env)
                        def jobResult = jobBuild.getResult()
                        echo "Build of 'Performance Assesment at platform' returned result: ${jobResult}"
                        buildResults['steps'][step_name] = jobResult
                    }
                }
            }
        }
        stage('Certification: Static Application Securirty Test - SAST') {
            parallel {
                stage('Certification: Static Code Analysis') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_static_code_analysis
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = staticCodeAnalysis(env)
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Static Code Analysis' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Certification: Source Code Security Analysis') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_security_scan_code
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/002-Security Scan Code', wait: true, propagate: true,
                                    parameters: [
                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                        string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                        booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                        ]
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Security Scan Code Analysis' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Certification: Source Code Secret Leakage') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_security_scan_secrets
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/003-Security Scan Secrets', wait: true, propagate: true,
                                    parameters: [
                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                        string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                        booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                        ]

                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Secrets Scan Code Analysis' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Certification: OpenSource Licenses Report') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_open_source_licenses_report
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/015-OpenSource_Licenses_Report', wait: true, propagate: false,
                                    parameters: [
                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                        string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                        booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                        ]
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'OpenSource Licenses Report' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Certification: Build validation image Report and Security Scan Docker Images Builded') {
                    steps {
                        retry(5) {
                            script {
                                def step_name = step_build
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '003-NETAPPS/003-Helpers/004-Build Network App', wait: true, propagate: true,
                                    parameters: [
                                        string(name: 'VERSION', value: String.valueOf(VERSION_NETAPP)),
                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                        string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                        booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                        ]
                                def jobResult = jobBuild.getResult()
                                echo "Build of '$NETAPP_NAME' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                                // buildResults['steps'][step_name] = 'SUCCESS'
                                // echo "BUILD NOT EXECUTED, TESTING PURPOUSE"
                            }
                        }
                        retry(2) {
                            script {
                                def step_name = step_security_scan_docker_images
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/004-Security Scan Docker Images', wait: true, propagate: true,
                                    parameters: [
                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                        string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                        booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                        ]
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Security Scan Docker Images' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
            }
        }

        //10
        stage('Certification: Deploying CAPIF-NEF-TSN-NetworkApp') {
            steps {
                script {
                    def step_name = step_deploy_capif_nef_netapp
                    buildResults['steps'][step_name] = 'FAILURE'
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/019-CAPIF-NEF-NETAPP-deploy', wait: true, propagate: true,
                        parameters: [
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                            string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                            string(name: 'HOSTNAME_CAPIF', value:  String.valueOf(HOSTNAME_CAPIF)),
                            string(name: 'VERSION_CAPIF', value: String.valueOf(VERSION_CAPIF)),
                            string(name: 'RELEASE_NAME_CAPIF', value: String.valueOf(RELEASE_CAPIF)),
                            string(name: 'HOSTNAME_NEF', value: String.valueOf(HOSTNAME_NEF)),
                            string(name: 'RELEASE_NAME_NEF', value: String.valueOf(RELEASE_NEF)),
                            string(name: 'HOSTNAME_TSN', value: String.valueOf(HOSTNAME_TSN)),
                            string(name: 'RELEASE_NAME_TSN', value: String.valueOf(RELEASE_TSN)),
                            string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                            string(name: 'HOSTNAME_NETAPP', value: String.valueOf(HOSTNAME_NETAPP)),
                            string(name: 'RELEASE_NAME_NETAPP', value: String.valueOf(NETAPP_NAME_LOWER)),
                            string(name: 'APP_REPLICAS', value: String.valueOf(APP_REPLICAS_NETAPP)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy CAPIF, NEF and Network App' returned result: ${jobResult}"
                    buildResults['steps'][step_name] = jobResult
                    if (jobResult == 'SUCCESS') {
                        sh '''#!/bin/bash
                        result_file="${DEPLOY_REPORT_FILENAME}.json"
                        url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$result_file"
                        echo "Result File: $result_file"
                        echo "Destination URL: $url"
                        curl  -f $url -u $ARTIFACTORY_CRED -o $result_file || echo "No result obtained"
                        '''
                        def fileName = "${DEPLOY_REPORT_FILENAME}" + '.json'
                        if (fileExists(fileName)) {
                            def deploy_results = readJSON file: fileName
                            buildResults['deploy_kpi'] = deploy_results['deploy_kpi']
                        }
                    }
                }
            }
        }
        stage('Leave some time to perform needed operations by all components') {
            steps {
                sleep(time: 1, unit: 'MINUTES')
                script {
                    buildResults['tests_executed'] = true
                }
            }
        }
        //11
        stage('Certification: Tests to Network App') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            parallel {
                //12
                stage('Certification: Onboarding NetworkApp to CAPIF') {
                    steps {
                        script {
                            buildResults[useOf5gApis][0] = [:]
                            buildResults[useOf5gApis][0]['name'] = 'Onboarding NetworkApp to CAPIF'
                            buildResults[useOf5gApis][0]['value'] = 'FAILURE'
                            def initial_test_ok = buildResults['tests_ok']
                            buildResults['tests_ok'] = false

                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/008-Onboard NetApp to CAPIF', wait: true, propagate: true,
                                parameters: [
                                    string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                                    ]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Onboarding NetworkApp to CAPIF' returned result: ${jobResult}"
                            buildResults[useOf5gApis][0]['value'] = jobResult
                            buildResults['tests_ok'] = initial_test_ok
                            if (jobResult == 'FAILURE') {
                                buildResults['tests_ok'] = false
                            }
                        }
                    }
                }
                stage('Certification: Discover NEF APIs from CAPIF') {
                    options {
                        timeout(time: 5, unit: 'MINUTES')
                    }
                    steps {
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/009-Discover NEF APIs', wait: true, propagate: false,
                                        parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Discover NEF APIs' returned result: ${jobResult}"
                            buildResults[useOf5gApis][1]=[:]
                            buildResults[useOf5gApis][1]['name'] = 'Discover NEF APIs from CAPIF'
                            buildResults[useOf5gApis][1]['value'] = jobResult
                            if (jobResult == 'FAILURE') {
                                buildResults['tests_ok'] = false
                            }
                        }
                    }
                }
                stage('Certification: NEF Services logged at CAPIF') {
                    steps {
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/020-NEF Services Check', wait: true, propagate: false,
                                parameters: [
                                    string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                    string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                    string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                    string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                    booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                    booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                    ]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'NEF Services logged at CAPIF' returned result: ${jobResult}"
                            if (jobResult == 'SUCCESS') {
                                sh '''#!/bin/bash
                                result_file="060-report-nef-logging.json"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$result_file"
                                curl  -f $url -u $ARTIFACTORY_CRED -o $result_file || echo "No result obtained"
                                '''
                                def fileName = '060-report-nef-logging.json'
                                if (fileExists(fileName)) {
                                    def nef_services_check_results = readJSON file: fileName
                                    buildResults[useOf5gApis][2] = [:]
                                    buildResults[useOf5gApis][2]['name'] = 'NEF Services logged at CAPIF'
                                    buildResults[useOf5gApis][2]['value'] = nef_services_check_results
                                }
                            }
                        }
                    }
                }
                stage('Certification: TSN Services logged at CAPIF') {
                    steps {
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/020-TSN Services Check', wait: true, propagate: false,
                                parameters: [
                                    string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                    string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                    string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                    string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                    booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                    booleanParam(name: 'SEND_DEV_MAIL', value: false)
                                    ]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'TSN Services logged at CAPIF' returned result: ${jobResult}"
                            if (jobResult == 'SUCCESS') {
                                sh '''#!/bin/bash
                                result_file="060-report-tsn-logging.json"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$result_file"
                                curl  -f $url -u $ARTIFACTORY_CRED -o $result_file || echo "No result obtained"
                                '''
                                def fileName = '060-report-tsn-logging.json'
                                if (fileExists(fileName)) {
                                    def tsn_services_check_results = readJSON file: fileName
                                    buildResults[useOf5gApis][3] = [:]
                                    buildResults[useOf5gApis][3]['name'] = 'TSN Services logged at CAPIF'
                                    buildResults[useOf5gApis][3]['value'] = tsn_services_check_results
                                }
                            }
                        }
                    }
                }
            }
        }

        stage('Certification: Validate CAPIF') {
            steps {
                retry(3) {
                    script {
                        def jobBuild = capifValidation(env)
                        def jobResult = jobBuild.getResult()
                        echo "Build of 'Validate CAPIF' returned result: ${jobResult}"
                        if (jobResult == 'FAILURE') {
                            buildResults['tests_ok'] = false
                            currentBuild.result = 'ABORTED'
                            aborted = true
                            error('CAPIF is not working properly, abort pipeline')
                        }
                    }
                }
            }
        }
        stage('Certification: Validate NEF') {
            steps {
                retry(3) {
                    script {
                        def jobBuild = nefValidation(env)
                        def jobResult = jobBuild.getResult()
                        echo "Build of 'Validate NEF' returned result: ${jobResult}"
                        if (jobResult == 'FAILURE') {
                            buildResults['tests_ok'] = false
                            currentBuild.result = 'ABORTED'
                            aborted = true
                            error('NEF is not working properly, abort pipeline')
                        }
                    }
                }
            }
        }

        stage('Certification: Network App KPIs') {
            steps {
                retry(2) {
                    script {
                        def step_name = step_network_app_kpis
                        buildResults['steps'][step_name] = 'FAILURE'
                        def jobBuild = obtainNetworkAppKPIs(env)
                        def jobResult = jobBuild.getResult()
                        echo "Build of 'Network App KPIs' returned result: ${jobResult}"
                        buildResults['steps'][step_name] = jobResult
                    }
                }
            }
        }

        stage('Certification: Check Tests results') {
            steps {
                script {
                    if (buildResults['tests_ok'] == false) {
                        error(message: 'One or More tests FAILS, please check summary')
                    }
                }
            }
        }
    }

    post {
        always {
            retry(3) {
                script {
                    echo 'Destroy Network App'
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/013-Destroy NetApp', wait: true, propagate: false,
                        parameters: [
                            string(name: 'RELEASE_NAME', value: String.valueOf(NETAPP_NAME_LOWER)),
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of ' Deploy NetApp' returned result: ${jobResult}"
                }
            }
            retry(3) {
                script {
                    echo 'Destroy TSN'
                    def jobBuild = build job: '005-TSN-FrontEnd/destroy', wait: true, propagate: false,
                        parameters: [
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_TSN)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy TSN' returned result: ${jobResult}"
                }
            }
            retry(3) {
                script {
                    echo 'Destroy NEF'
                    def jobBuild = build job: '002-NEF/nef-destroy', wait: true, propagate: false,
                        parameters: [
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_NEF)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy NEF' returned result: ${jobResult}"
                }
            }
            retry(3) {
                script {
                    echo 'Destroy CAPIF'
                    def jobBuild = build job: '/001-CAPIF/destroy', wait: true, propagate: false,
                        parameters: [
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy CAPIF' returned result: ${jobResult}"
                }
            }
            dir("${env.WORKSPACE}/") {
                script {
                    buildResults['environment'] = String.valueOf(ENVIRONMENT)
                    buildResults['build_number'] = String.valueOf(BUILD_NUMBER)
                    buildResults['result'] = currentBuild.currentResult
                    if (buildResults['tests_ok'] == false) {
                        buildResults['result'] = 'FAILURE'
                        if (buildResults['tests_executed']) {
                            buildResults['steps'][step_use_of_5g_apis] = 'FAILURE'
                        }
                    } else {
                        if (buildResults['tests_executed']) {
                            buildResults['steps'][step_use_of_5g_apis] = 'SUCCESS'
                            sh '''#!/bin/bash

                            if [[ $OVERWRITE_FINGERPRINT == false ]]
                            then
                                echo "try to recover fingerprint from artifactory"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$VERSION_NETAPP/$FINGERPRINT_FILENAME"
                                curl  -f $url -u $ARTIFACTORY_CRED -o $FINGERPRINT_FILENAME || echo "No result obtained"
                            fi

                            if [[ -f $FINGERPRINT_FILENAME ]]
                            then
                                echo "Fingerprint is present on artifactory for this netapp ($NETAPP_NAME) and version ($VERSION_NETAPP)"
                                cat $FINGERPRINT_FILENAME
                            else
                                echo "Create Fingerpint and pushing to artifactory"
                                UUID=$(uuidgen)
                                echo $UUID
                                jq -n --arg CERTIFICATION_ID $UUID --arg VERSION $VERSION_NETAPP -f ./utils/fingerprint/fp_template.json > $FINGERPRINT_FILENAME

                                cat $FINGERPRINT_FILENAME

                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$VERSION_NETAPP/$FINGERPRINT_FILENAME"
                                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                --data-binary @"$FINGERPRINT_FILENAME" \
                                "$url"

                                url="$ARTIFACTORY_URL/$NETAPP_NAME/$VERSION_NETAPP/$FINGERPRINT_FILENAME"
                                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                --data-binary @"$FINGERPRINT_FILENAME" \
                                "$url"
                            fi
                            '''
                        }
                    }

                    def fileName = getFingerprintFilename()
                    if (fileExists(fileName)) {
                        def fingerprint = readJSON file: fileName
                        buildResults['fingerprint'] = fingerprint
                    }

                    buildResults['build_trigger_by'] = currentBuild.getBuildCauses()[0].shortDescription.replace('Lanzada por el usuario ', '').split(' ')[0] + ' / ' + currentBuild.getBuildCauses()[0].userId
                    buildResults['total_duration'] = currentBuild.durationString.replace(' and counting', '').replace(' y contando', '')
                    writeFile file: "report-steps-${env.NETAPP_NAME_LOWER}.json", text: JsonOutput.toJson(buildResults)
                    
                    sh '''#!/bin/bash
                    report_file="report-steps-$NETAPP_NAME_LOWER.json"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"
                    curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                    --data-binary @"$report_file" \
                    "$url"
                    '''
                }
            }
            retry(3) {
                script {
                    if (aborted == false) {
                        build job: '/003-NETAPPS/003-Helpers/100-Generate Final Report', wait: true, propagate: true,
                        parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                            string(name: 'VERSION_NETAPP', value: String.valueOf(VERSION_NETAPP)),
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                            string(name: 'STAGE', value: String.valueOf(PHASE_LOWER)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                            booleanParam(name: 'SEND_DEV_MAIL', value: false)
                            ]
                    }
                }
            }

            script {
                // Nettaps emails to send the report
                if (emails?.split(' ')) {
                    // if (aborted == false) {
                    dir("${WORKSPACE}/") {
                        sh '''#!/bin/bash

                        response=$(curl -s "http://artifactory.hi.inet/ui/api/v1/ui/nativeBrowser/misc-evolved5g/$PHASE_LOWER/$NETAPP_NAME_LOWER/$BUILD_ID/attachments" -u $ARTIFACTORY_CRED | jq ".children[].name" | tr -d '"' )
                        artifacts=($response)

                        mkdir attachments

                        for x in "${artifacts[@]}"
                        do
                            url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/attachments/$x"
                            curl -u $ARTIFACTORY_CRED $url -o attachments/$x
                        done
                        '''
                    }
                    emails.tokenize().each() {
                        email -> emailext attachmentsPattern: '**/attachments/**',
                                body: '''${SCRIPT, template="groovy-html.template"}''',
                                mimeType: 'text/html',
                                subject: "Jenkins ${env.NETAPP_NAME} Certification Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                                from: 'jenkins-evolved5G@tid.es',
                                replyTo: 'jenkins-evolved5G',
                                to: email
                    }
                }
            }
            script {
                if ("${params.SEND_DEV_MAIL}".toBoolean() == true) {
                    sh 'echo "Send mail to all developers"'
                    emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                    mimeType: 'text/html',
                    subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                    from: 'jenkins-evolved5G@tid.es',
                    replyTo: 'jenkins-evolved5G',
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
