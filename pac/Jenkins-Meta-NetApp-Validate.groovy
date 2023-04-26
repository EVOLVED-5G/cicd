import groovy.json.JsonOutput

def buildResults = [:]
buildResults['steps'] = [:]
buildResults['tests_ok'] = true

String netappName(String url) {
    String url2 = url ?: ''
    String var = url2.substring(url2.lastIndexOf('/') + 1)
    return var
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

def getHttpPort(deployment) {
    String var = deployment
    if ('kubernetes-athens'.equals(var)) {
        return '30048'
    }else {
        return ''
    }
}

def getHttpsPort(deployment) {
    String var = deployment
    if ('kubernetes-athens'.equals(var)) {
        return '30548'
    }else {
        return ''
    }
}

def step_static_code_analysis = 'static-code-analysis'
def step_security_scan_code = 'source-code-security-analysis'
def step_security_scan_secrets = 'source-code-secrets-leakage'
def step_build = 'network-app-build-and-port-check'
def step_security_scan_docker_images = 'image-security-analysis'
def step_deploy_capif_nef_netapp = 'deploy-network-app'
def step_validate_capif = 'validate-capif'
def step_onboard_netApp_to_capif = 'network-app-onboarding-to-capif'
def step_discover_nef_apis = 'discover-apis'
// def step_nef_services_monitoringevent_api = 'nef-services-monitoringevent-api'
// def step_nef_services_monitoringevent = 'nef-services-monitoringevent'
def step_nef_services_apis = 'nef-services-apis'
def step_destroy_network_app = 'destroy-netapp'
def step_destroy_nef = 'destroy-nef'
def step_destroy_capif = 'destroy-capif'
def step_open_source_licenses_report = 'open-source-licenses-report'

def initial_status = 'PENDING'
def not_report = 'NOT_REPORT'

pipeline {
    agent { node { label getAgent("${params.ENVIRONMENT }") == 'any' ? '' : getAgent("${params.ENVIRONMENT }") } }
    options {
        timeout(time: 60, unit: 'MINUTES')
        retry(1)
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'HOSTNAME_NETAPP', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Hostname to NetworkApp')
        string(name: 'VERSION_NETAPP', defaultValue: '1.0', description: 'Version Network App')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'main', description: 'Deployment git branch name')
        string(name: 'DEPLOY_NAME', defaultValue: 'fogus', description: 'Deployment NetworkApp name')
        string(name: 'APP_REPLICAS_NETAPP', defaultValue: '1', description: 'Number of NetworkApp pods to run')
        string(name: 'HOSTNAME_CAPIF', defaultValue: 'capif.apps.ocp-epg.hi.inet', description: 'Hostname to CAPIF')
        string(name: 'VERSION_CAPIF', defaultValue: '3.0', description: 'Version CAPIF')
        string(name: 'RELEASE_CAPIF', defaultValue: 'capif', description: 'Helm Release name to CAPIF')
        string(name: 'RELEASE_NEF', defaultValue: 'nef', description: 'Helm Release name to NEF')
        string(name: 'HOSTNAME_NEF', defaultValue: 'nef.apps.ocp-epg.hi.inet', description: 'Hostname to NEF')
        choice(name: 'ENVIRONMENT', choices: ['openshift', 'kubernetes-athens', 'kubernetes-uma'])
        booleanParam(name: 'REPORTING', defaultValue: true, description: 'Save report into artifactory')
        booleanParam(name: 'SEND_DEV_MAIL', defaultValue: true, description: 'Send mail to Developers')
        string(name: 'EMAILS', defaultValue: '', description: 'Nettaps emails in order to notify final report')
    }

    environment {
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED = credentials('artifactory_credentials')
        ARTIFACTORY_URL = 'http://artifactory.hi.inet/artifactory/misc-evolved5g/validation'
        RELEASE_CAPIF = "${params.RELEASE_CAPIF}"
        HOSTNAME_CAPIF = "${params.HOSTNAME_CAPIF}"
        RELEASE_NEF = "${params.RELEASE_NEF}"
        HOSTNAME_NEF = "${params.HOSTNAME_NEF}"
        RELEASE_NAME = "${params.DEPLOY_NAME}"
        VERSION_NETAPP = "${params.VERSION_NETAPP}"
        emails = "${params.EMAILS}".trim()
        CAPIF_PORT = getHttpPort("${params.ENVIRONMENT}")
        CAPIF_TLS_PORT = getHttpsPort("${params.ENVIRONMENT}")
    }

    stages {
        stage('Initialize Local Variables') {
            steps {
                script {
                    echo "Setting local variables"
                    step_deploy_capif_nef_netapp = 'deploy-' + "${NETAPP_NAME_LOWER}" + '-network-app'

                    buildResults['steps'][step_static_code_analysis] = initial_status
                    buildResults['steps'][step_security_scan_code] = initial_status
                    buildResults['steps'][step_security_scan_secrets] = initial_status
                    buildResults['steps'][step_build] = initial_status
                    buildResults['steps'][step_security_scan_docker_images] = initial_status
                    buildResults['steps'][step_deploy_capif_nef_netapp] = initial_status
                    buildResults['steps'][step_validate_capif] = initial_status
                    buildResults['steps'][step_onboard_netApp_to_capif] = initial_status
                    // buildResults['steps'][step_discover_nef_apis] = initial_status
                    // buildResults['steps'][step_nef_services_monitoringevent_api] = initial_status
                    // buildResults['steps'][step_nef_services_monitoringevent] = initial_status
                    buildResults['steps'][step_nef_services_apis] = not_report
                    // buildResults['steps'][step_destroy_network_app] = initial_status
                    // buildResults['steps'][step_destroy_nef] = initial_status
                    // buildResults['steps'][step_destroy_capif] = initial_status
                    buildResults['steps'][step_open_source_licenses_report] = initial_status
                }
            }
        }
        stage('Validation: Static Application Securirty Test - SAST') {
            parallel {
                // stage('Validation: Static Code Analysis') {
                //     steps{
                //         retry(2) {
                //             script {
                //                 def step_name = step_static_code_analysis
                //                 buildResults['steps'][step_name] = 'FAILURE'
                //                 def jobBuild = build job: '/003-NETAPPS/003-Helpers/001-Static Code Analysis', wait: true, propagate: true,
                //                 parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                                 string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                                 string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                                 string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                                 booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                //                                 booleanParam(name: 'SEND_DEV_MAIL', value: false)]

                //                 def jobResult = jobBuild.getResult()
                //                 echo "Build of 'Static Code Analysis' returned result: ${jobResult}"
                //                 buildResults['steps'][step_name] = jobResult
                //             }
                //         }
                //     }
                // }
                stage('Validation: Source Code Security Analysis') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_security_scan_code
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/002-Security Scan Code', wait: true, propagate: true,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                                booleanParam(name: 'SEND_DEV_MAIL', value: false)]
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Security Scan Code Analysis' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Validation: Source Code Secret Leakage') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_security_scan_secrets
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/003-Security Scan Secrets', wait: true, propagate: true,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                                booleanParam(name: 'SEND_DEV_MAIL', value: false)]

                                def jobResult = jobBuild.getResult()
                                echo "Build of 'Secrets Scan Code Analysis' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                stage('Validation: OpenSource Licenses Report') {
                    steps {
                        retry(2) {
                            script {
                                def step_name = step_open_source_licenses_report
                                buildResults['steps'][step_name] = 'FAILURE'
                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/015-OpenSource_Licenses_Report', wait: true, propagate: false,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                            booleanParam(name: 'SEND_DEV_MAIL', value: false)]
                                def jobResult = jobBuild.getResult()
                                echo "Build of 'OpenSource Licenses Report' returned result: ${jobResult}"
                                buildResults['steps'][step_name] = jobResult
                            }
                        }
                    }
                }
                // stage('Validation: Build validation image Report and Security Scan Docker Images Builded') {
                //     steps {
                //         retry(2) {
                //             script {
                //                 def step_name = step_build
                //                 buildResults['steps'][step_name] = 'FAILURE'
                //                 def jobBuild = build job: '003-NETAPPS/999-ToReview/build', wait: true, propagate: true,
                //                     parameters: [string(name: 'VERSION', value: String.valueOf(VERSION_NETAPP)),
                //                                 string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                                 string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                                 string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                                 string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                                 string(name: 'STAGE', value: 'validation'),
                //                                 string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                //                                 booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                //                                 booleanParam(name: 'SEND_DEV_MAIL', value: false)]
                //                 def jobResult = jobBuild.getResult()
                //                 echo "Build of '$NETAPP_NAME' returned result: ${jobResult}"
                //                 buildResults['steps'][step_name] = jobResult
                //             }
                //         }
                //         retry(2) {
                //             script {
                //                 def step_name = step_security_scan_docker_images
                //                 buildResults['steps'][step_name] = 'FAILURE'
                //                 def jobBuild = build job: '/003-NETAPPS/003-Helpers/004-Security Scan Docker Images', wait: true, propagate: true,
                //                 parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                             string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                             string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                             string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                             string(name: 'STAGE', value: 'validation'),
                //                             string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                //                             booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                //                             booleanParam(name: 'SEND_DEV_MAIL', value: false)]
                //                 def jobResult = jobBuild.getResult()
                //                 echo "Build of 'Security Scan Docker Images' returned result: ${jobResult}"
                //                 buildResults['steps'][step_name] = jobResult
                //             }
                //         }
                //     }
                // }
            }
        }

        //10
        stage('Validation: Deploying CAPIF-NEF-NetworkApp') {
            steps {
                script {
                    def step_name = step_deploy_capif_nef_netapp
                    buildResults['steps'][step_name] = 'FAILURE'
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/019-CAPIF-NEF-NETAPP-deploy', wait: true, propagate: true,
                            parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'HOSTNAME_CAPIF', value:  String.valueOf(HOSTNAME_CAPIF)),
                                        string(name: 'VERSION_CAPIF', value: String.valueOf(VERSION_CAPIF)),
                                        string(name: 'RELEASE_NAME_CAPIF', value: String.valueOf(RELEASE_CAPIF)),
                                        string(name: 'HOSTNAME_NEF', value: String.valueOf(HOSTNAME_NEF)),
                                        string(name: 'RELEASE_NAME_NEF', value: String.valueOf(RELEASE_NEF)),
                                        string(name: 'HOSTNAME_NETAPP', value: String.valueOf(HOSTNAME_NETAPP)),
                                        string(name: 'RELEASE_NAME_NETAPP', value: String.valueOf(DEPLOY_NAME)),
                                        string(name: 'APP_REPLICAS', value: String.valueOf(APP_REPLICAS_NETAPP)),
                                        string(name: 'FOLDER_NETWORK_APP', value: String.valueOf(DEPLOY_NAME)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy CAPIF, NEF and Network App' returned result: ${jobResult}"
                    buildResults['steps'][step_name] = jobResult
                }
            }
        }

        stage('Validation: Validate CAPIF'){
           steps{
               script {
                    def step_name = step_validate_capif
                    def jobBuild = build job: '/001-CAPIF/Launch_Robot_Tests', wait: true, propagate: false,
                                    parameters: [string(name: 'BRANCH_NAME', value: "pipeline-tests"),
                                                booleanParam(name: 'RUN_LOCAL_CAPIF', value: false),
                                                string(name: 'CAPIF_HOSTNAME', value: String.valueOf(HOSTNAME_CAPIF)),
                                                string(name: 'CAPIF_PORT', value: String.valueOf(CAPIF_PORT)),
                                                string(name: 'CAPIF_TLS_PORT', value: String.valueOf(CAPIF_TLS_PORT)),
                                                string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))
                                                ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Validate CAPIF' returned result: ${jobResult}"

                    buildResults['steps'][step_name] = jobResult
                    if (jobResult == 'FAILURE') {
                        buildResults['tests_ok'] = false
                    }
               }
           }
        }
        //11
        stage('Validation: Tests to Network App') {
            options {
                timeout(time: 5, unit: 'MINUTES')
            }
            parallel {
                //12
                stage('Validation: Onboarding NetworkApp to CAPIF') {
                    steps {
                        script {
                            def step_name = step_onboard_netApp_to_capif
                            buildResults['steps'][step_name] = 'FAILURE'
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/008-Onboard NetApp to CAPIF', wait: true, propagate: true,
                                        parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Onboarding NetworkApp to CAPIF' returned result: ${jobResult}"
                            buildResults['steps'][step_name] = jobResult
                            if (jobResult == 'FAILURE') {
                                buildResults['tests_ok'] = false
                            }
                        }
                    }
                }

                //Review Parameters
                //15
//                stage('Validation: Discover NEF APIs from CAPIF') {
//                    options {
//                        timeout(time: 5, unit: 'MINUTES')
//                    }
//                    steps {
//                        script {
//                            def step_name = step_discover_nef_apis
//                            buildResults['steps'][step_name] = 'FAILURE'
//                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/009-Discover NEF APIs', wait: true, propagate: false,
//                                        parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
//                                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
//                                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
//                            def jobResult = jobBuild.getResult()
//                            echo "Build of 'Discover NEF APIs' returned result: ${jobResult}"
//                            buildResults['steps'][step_name] = jobResult
//                            if (jobResult == 'FAILURE') {
//                                buildResults['tests_ok'] = false
//                            }
//                        }
//                    }
//                }
//
                stage('Validation: NEF Services logged at CAPIF'){
                    steps {
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/020-NEF Services Check', wait: true, propagate: false,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                            string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                                            booleanParam(name: 'SEND_DEV_MAIL', value: false)]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'NEF Services logged at CAPIF' returned result: ${jobResult}"
                            if (jobResult == 'SUCCESS') {
                                sh '''#!/bin/bash
                                result_file="006-report-nef-logging.json"
                                url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$result_file"
                                curl  $url -u $ARTIFACTORY_CRED -o $result_file
                                '''
                                def nef_services_check_results = readJSON file: '006-report-nef-logging.json'
                                buildResults['steps'][step_nef_services_apis] = nef_services_check_results
                            }
                        }
                    }
                }
                //Review Parameters
                //jenkins-dummy
                //16
                //                stage('Validation: NEF Services as SessionWithQoS') {
                //                    steps {
                //                        script {
                //                                def jobBuild = build job: '/003-NETAPPS/003-Helpers/010-NEF Services asSessionWithQoS', wait: true, propagate: false,
                //                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                //                                def jobResult = jobBuild.getResult()
                //                                echo "Build of 'NEF Services as SessionWithQoS' returned result: ${jobResult}"
                //                                buildResults['steps']['nef-services-as-sessionwithqos'] = jobResult
                //                        }
                //                    }
                //                }
                //
                //Review Parameters
                //jenkins-dummy
                //17
                // stage('Validation: NEF Services MonitoringEvent API') {
                //     steps {
                //         script {
                //             def step_name = step_nef_services_monitoringevent_api
                //             buildResults['steps'][step_name] = 'FAILURE'
                //             def jobBuild = build job: '/003-NETAPPS/003-Helpers/011-NEF Services MonitoringEvent API', wait: true, propagate: false,
                //                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                //             def jobResult = jobBuild.getResult()
                //             echo "Build of 'NEF Services MonitoringEvent API' returned result: ${jobResult}"
                //             buildResults['steps'][step_name] = jobResult
                //         }
                //     }
                // }

                //Review Parameters
                //jenkins-dummy
                //18
                // stage('Validation: NEF Services MonitoringEvent') {
                //     steps {
                //         script {
                //             def step_name = step_nef_services_monitoringevent
                //             buildResults['steps'][step_name] = 'FAILURE'
                //             def jobBuild = build job: '/003-NETAPPS/003-Helpers/012-NEF MonitoringEvent', wait: true, propagate: false,
                //                 parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                //                             string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                //                             string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                //                             string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                //                             booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                //             def jobResult = jobBuild.getResult()
                //             echo "Build of 'NEF Services as SessionWithQoS' returned result: ${jobResult}"
                //             buildResults['steps'][step_name] = jobResult
                //         }
                //     }
                // }
            }
        }

        stage('Validation: Check Tests results') {
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
                    echo "Destroy Network App"
                    // def step_name = step_destroy_network_app
                    // buildResults['steps'][step_name] = 'FAILURE'
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/013-Destroy NetApp', wait: true, propagate: false,
                            parameters: [string(name: 'RELEASE_NAME', value: String.valueOf(DEPLOY_NAME)),
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                            ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of ' Deploy NetApp' returned result: ${jobResult}"
                    // buildResults['steps'][step_name] = jobResult
                }
            }
            retry(3) {
                script {
                    echo "Destroy NEF"
                    // def step_name = step_destroy_nef
                    // buildResults['steps'][step_name] = 'FAILURE'
                    def jobBuild = build job: '002-NEF/nef-destroy', wait: true, propagate: false,
                                    parameters: [
                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                        string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_NEF)),
                                        string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy NEF' returned result: ${jobResult}"
                    // buildResults['steps'][step_name] = jobResult
                }
            }
            retry(3) {
                script {
                    echo "Destroy CAPIF"
                    // def step_name = step_destroy_capif
                    // buildResults['steps'][step_name] = 'FAILURE'
                    def jobBuild = build job: '/001-CAPIF/destroy', wait: true, propagate: false,
                                    parameters: [
                                    string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                    string(name: 'RELEASE_NAME', value: String.valueOf(RELEASE_CAPIF)),
                                    string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy CAPIF' returned result: ${jobResult}"
                    // buildResults['steps'][step_name] = jobResult
                }
            }
            dir("${env.WORKSPACE}/") {
                script {
                    buildResults['environment'] = String.valueOf(ENVIRONMENT)
                    buildResults['build_number'] = String.valueOf(BUILD_NUMBER)
                    buildResults['result'] = currentBuild.currentResult
                    if (buildResults['tests_ok'] == false) {
                        buildResults['result'] = 'FAILURE'
                    }
                    buildResults['build_trigger_by'] = currentBuild.getBuildCauses()[0].shortDescription.replace('Lanzada por el usuario ','').split(' ')[0] + ' / ' + currentBuild.getBuildCauses()[0].userId
                    buildResults['total_duration'] = currentBuild.durationString.replace(' and counting', '').replace(' y contando', '')
                    writeFile file: "report-steps-${env.NETAPP_NAME_LOWER}.json", text: JsonOutput.toJson(buildResults)
                }
                sh '''#!/bin/bash
                report_file="report-steps-$NETAPP_NAME_LOWER.json"
                url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"
                curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                            --data-binary @"$report_file" \
                            "$url"
                            '''
            }
            retry(3) {
                build job: '/003-NETAPPS/003-Helpers/100-Generate Final Report', wait: true, propagate: true,
                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                            string(name: 'VERSION_NETAPP', value: String.valueOf(VERSION_NETAPP)),
                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                            string(name: 'DEPLOYMENT', value: String.valueOf(ENVIRONMENT)),
                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING)),
                            booleanParam(name: 'SEND_DEV_MAIL', value: false)]
            }

            script {
                // Nettaps emails to send the report
                if (emails?.split(' ')) {
                    dir("${WORKSPACE}/") {
                        sh '''#!/bin/bash

                        report_file="final_report.pdf"
                        url="$ARTIFACTORY_URL/$NETAPP_NAME_LOWER/$BUILD_ID/$report_file"

                        curl  $url -u $ARTIFACTORY_CRED -o final_report.pdf
                        '''
                    }
                    emails.tokenize().each() {
                        email -> emailext attachmentsPattern: '**/final_report.pdf',
                                 body: '''${SCRIPT, template="groovy-html.template"}''',
                                 mimeType: 'text/html',
                                 subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
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
