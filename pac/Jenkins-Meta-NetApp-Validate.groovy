import groovy.json.JsonOutput
def buildResults = [:]

String netappName(String url) {
    String url2 = url?:'';
    String var = url2.substring(url2.lastIndexOf("/") + 1);
    return var ;
}

pipeline {
    agent {
        node {
            label 'evol5-slave'
        }
    }
    options {
        timeout(time: 60, unit: 'MINUTES')
        retry(2)
    }
    

    
    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'ENVIRONMENT', defaultValue: 'openshift', description: 'Environment')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/validation"
    }

    stages {
        stage ('Static Code Analysis'){
            parallel {
                stage('Validation: Source Code Quality Analysis'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/001-Static Code Analysis', wait: true, propagate: false,
                                            parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]           
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Static Code Analysis' returned result: ${jobResult}"
                            buildResults['static-analysis'] = jobResult
                        }
                    }
                }
                stage('Validation: Source Code Security Analysis'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/002-Security Scan Code', wait: true, propagate: false,
                                        parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Security Scan Code Analysis' returned result: ${jobResult}"
                            buildResults['security-analysis'] = jobResult
                        }
                    }
                }
                stage('Validation: Source Code Secret Leakage'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/003-Security Scan Secrets', wait: true, propagate: false,
                                        parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Secrets Scan Code Analysis' returned result: ${jobResult}"
                            buildResults['secrets-analysis'] = jobResult
                        }
                    }
                }

                stage('Validation: OpenSource Licenses Report'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/015-OpenSource Licenses Report', wait: true, propagate: false,
                                            parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'OpenSource Licenses Report' returned result: ${jobResult}"
                            buildResults['opensource-license'] = jobResult
                        }
                    }
                }

                // FIX VERSION = 1 --> Validacion
                stage('Validation: Build validation image  Report'){
                    steps{
                        script {
                            def jobBuild = build job: '003-NETAPPS/999-ToReview/build', wait: true, propagate: false,
                                            parameters: [string(name: 'VERSION', value: '1.0'),
                                                        string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'STAGE', value: "validation") ]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Netapp' returned result: ${jobResult}"
                            buildResults['build'] = jobResult
                        }
                    }
                }

                stage('Validation: Security Scan Docker Images'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/004-Security Scan Docker Images', wait: true, propagate: false,
                                            parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        string(name: 'STAGE', value: "validation"),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Security Scan Docker Images' returned result: ${jobResult}"
                            buildResults['scan-docker-images'] = jobResult
                        }
                    }
                }
            }
        }

        stage('Validation:Deploy CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '001-CAPIF/deploy', wait: true, propagate: false,
                                   parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'HOSTNAME', value: "capif.apps.ocp-epg.hi.inet")]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy CAPIF' returned result: ${jobResult}"
                    //buildResults['deploy-capif'] = jobResult
                }
            }
        }
        stage('Validation: Validate CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '/001-CAPIF/Launch_Robot_Tests', wait: true, propagate: false,
                                   parameters: [string(name: 'BRANCH_NAME', value: "GHACKING-6064_adapt_robot_tests_new_flows"),
                                                booleanParam(name: 'RUN_LOCAL_CAPIF', value: "False"),
                                                string(name: 'CAPIF_HOSTNAME', value: "capif.apps.ocp-epg.hi.inet" )]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Validate CAPIF' returned result: ${jobResult}"
                    //buildResults['validate-capif'] = jobResult
                }
            }
        }

        stage('Validation: Deploy NEF'){
            steps{
                script {
                    def jobBuild = build job: '002-NEF/nef-deploy', wait: true, propagate: false,
                     parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'HOSTNAME', value: "nef.apps.ocp-epg.hi.inet" ),
                                booleanParam(name: 'REPORTING', value: "openshift")]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy NEF' returned result: ${jobResult}"
                    //buildResults['deploy-nef'] = jobResult
                }
            }
        }
        stage('Validation: Validate NEF'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/008-Onboard NetApp to CAPIF', wait: true, propagate: false,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Validate NEF' returned result: ${jobResult}"
                    //buildResults['validate-nef'] = jobResult
                }
            }
        }
            parameters {
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        string(name: 'APP_REPLICAS', defaultValue: '2', description: 'Number of Dummy NetApp pods to run')
        string(name: 'DUMMY_NETAPP_HOSTNAME', defaultValue: 'fogus.apps.ocp-epg.hi.inet', description: 'Netapp hostname')
        string(name: 'DEPLOYMENT_NAME', defaultValue: 'dummy-netapp', description: 'Netapp hostname')
        choice(name: "DEPLOYMENT", choices: ["openshift", "kubernetes-athens", "kubernetes-uma"])  
    }
        //HARDCODED VARIABLE IN GIT FOR THE DEMO
        stage('Validation:  Deploy NetApp'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/005-Deploy NetApp', wait: true, propagate: false,
                                parameters: [string(name: 'DEPLOYMENT_NAME', value: String.valueOf(NETAPP_NAME)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'ENVIRONMENT', value: String.valueOf(ENVIRONMENT))
                                ]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy Netapp' returned result: ${jobResult}"
                    buildResults['deploy-netapp'] = jobResult
                }
            }
        }
                
        //Review Parameters
        stage('Validation: Test NetApp Networking'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/006-Test NetApp Networking', wait: true, propagate: false,
                                   parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Networking Netapp' returned result: ${jobResult}"
                    buildResults['network-netapp'] = jobResult
                }
            }
        }

        //Review Parameters
        stage('Validation: Onboarding NetApp as Invoker to CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/008-Onboard NetApp to CAPIF', wait: true, propagate: false,
                                   parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Onboard Netapp' returned result: ${jobResult}"
                    buildResults['onboard-netapp'] = jobResult
                }
            }
        }

        //Review Parameters
        stage('Validation: Discover NEF APIs from CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/009-Discover NEF APIs', wait: true, propagate: false,
                                parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                            string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                            string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                            string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                            booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Discover NEF APIs' returned result: ${jobResult}"
                    buildResults['discover-apis'] = jobResult
                }
            }
        }

        //Review Parameters
        stage('Validation: NEF Services as SessionWithQoS'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/010-NEF Services asSessionWithQoS', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: NEF Services MonitoringEvent API'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/011-NEF Services MonitoringEvent API', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: NEF Services MonitoringEvent'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/012-NEF MonitoringEvent', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }
        
        stage('Validation: Destroy NetApp'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/013-Destroy NetApp', wait: true, propagate: false,
                                parameters: [string(name: 'DEPLOYMENT_NAME', value: String.valueOf(NETAPP_NAME)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of ' Deploy NetApp' returned result: ${jobResult}"
                    buildResults['destroy-netapp'] = jobResult
                }
            }
        }
 
        stage('Validation: Destroy NEF'){
            steps{
                script {
                    def jobBuild = build job: '002-NEF/nef-destroy', wait: true, propagate: false,
                                    parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy NEF' returned result: ${jobResult}"
                    buildResults['destroy-nef'] = jobResult
                }
            }
        }

        stage('Validation: Destroy CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '/001-CAPIF/destroy', wait: true, propagate: false,
                                   parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Destroy CAPIF' returned result: ${jobResult}"
                    buildResults['destroy-capif'] = jobResult
                }
            }
        }

        stage('Validation: Obtaining information for previous pipelines'){
            steps{
                dir ("${env.WORKSPACE}/") {
                    script {
                        writeFile file: "report-steps-${env.NETAPP_NAME_LOWER}.json", text: JsonOutput.toJson([key: [buildResults]])
                    }
                    sh '''#!/bin/bash
                    report_file="report-steps-$NETAPP_NAME_LOWER.json"
                    url="$ARTIFACTORY_URL/$NETAPP_NAME/$BUILD_ID/$report_file"

                    curl -v -f -i -X PUT -u $ARTIFACTORY_CRED \
                                --data-binary @"$report_file" \
                                "$url"
                                '''
                }
            }
        }



        //Review Parameters
        stage('Validation: Generate Final Report'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/100-Generate Final Report', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

    }
    post {
        always {
            emailext body: '''${SCRIPT, template="groovy-html.template"}''',
                mimeType: 'text/html',
                subject: "Jenkins Build ${currentBuild.currentResult}: Job ${env.JOB_NAME}",
                from: 'jenkins-evolved5G@tid.es',
                replyTo: "jenkins-evolved5G",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
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
