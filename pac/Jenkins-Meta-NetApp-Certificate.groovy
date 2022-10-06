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
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    environment {
        NETAPP_NAME = netappName("${params.GIT_NETAPP_URL}")
        NETAPP_NAME_LOWER = NETAPP_NAME.toLowerCase()
        ARTIFACTORY_CRED=credentials('artifactory_credentials')
        ARTIFACTORY_URL="http://artifactory.hi.inet/artifactory/misc-evolved5g/certification"
    }

    stages {
        stage ('Static Code Analysis'){
            parallel {
                stage('Certification: Static Code Analysis'){
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
                stage('Certification: Security Scan Code'){
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
                stage('Certification: Security Scan Secrets'){
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
                        //Review Parameters -- do we need to get the image from registry ?
                stage('Certification: Get docker Image from Registry'){
                    steps{
                        script {
                            def jobBuild = build job: '/100-HELPERS/001-Get Docker Image', wait: true, propagate: false,
                                            parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Get docker Image from Registry' returned result: ${jobResult}"
                            buildResults['docker-images'] = jobResult
                        }
                    }
                }
        //Review Parameters 
                stage('Certification: Security Scan Docker Images'){
                    steps{
                        script {
                            def jobBuild = build job: '/003-NETAPPS/003-Helpers/004-Security Scan Docker Images', wait: true, propagate: false,
                                            parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                                        string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                                        string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                        string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                                        booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
                            def jobResult = jobBuild.getResult()
                            echo "Build of 'Security Scan Docker Images' returned result: ${jobResult}"
                            buildResults['scan-docker-images'] = jobResult
                        }
                    }
                }
            }
        }


        stage('Certification: Deploy CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '001-CAPIF/deploy', wait: true, propagate: false,
                                   parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'HOSTNAME', value: "nginx.apps.ocp-epg.hi.inet")]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy CAPIF' returned result: ${jobResult}"
                    buildResults['deploy-capif'] = jobResult
                }
            }
        }

        // Validate CAPIF
        stage('Certification: Validate CAPIF'){
            steps{
                script {
                    def jobBuild = build job: '/001-CAPIF/Launch_Robot_Tests', wait: true, propagate: false,
                                   parameters: [string(name: 'BRANCH_NAME', value: "CAPIF_aef_demo"),
                                                booleanParam(name: 'RUN_LOCAL_CAPIF', value: "False"),
                                                string(name: 'CAPIF_HOSTNAME', value: "nginx.apps.ocp-epg.hi.inet" )]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Validate CAPIF' returned result: ${jobResult}"
                    buildResults['validate-capif'] = jobResult
                }
            }
        }

        stage('Certification: Deploy NEF'){
            steps{
                script {
                    def jobBuild = build job: '002-NEF/nef-deploy', wait: true, propagate: false,
                     parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'HOSTNAME', value: "nef.apps.ocp-epg.hi.inet" ),
                                booleanParam(name: 'REPORTING', value: "openshift")]
                    def jobResult = jobBuild.getResult()
                    echo "Build of 'Deploy NEF' returned result: ${jobResult}"
                    buildResults['deploy-nef'] = jobResult
                }
            }
        }

        //HARDCODED VARIABLE IN GIT FOR THE DEMO
        stage('Certification:  Deploy NetApp'){
            steps{
                script {
                    def jobBuild = build job: '003-NETAPPS/999-ToReview/deploy', wait: true, propagate: false,
                                    parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                                string(name: 'APP_REPLICAS', value: "2"),
                                                string(name: 'DUMMY_NETAPP_HOSTNAME', value: "fogus.apps.ocp-epg.hi.inet")]
                    def jobResult = jobBuild.getResult()
                    echo "Build of ' Deploy NetApp'' returned result: ${jobResult}"
                    buildResults['deploy-netapp'] = jobResult
                }
            }
        }

        //Review Parameters
        stage('Certification: Test NetApp Networking'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/006-Test NetApp Networking', wait: true, propagate: false,
                     parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }
        
        //Review Parameters
        stage('Certification: NetApp Onboarding Sucessfull'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/007-NetApp Onboarding Successful', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Certification: Discover NetApp API from CAPIF'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/008-Discover NetApp API from CAPIF', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Certification: Discover NetApp Callback CAPIF'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/009-NetApp Callback CAPIF', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Certification: NEF Services as SessionWithQoS'){
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
        stage('Certification: NEF Services MonitoringEvent API'){
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
        stage('Certification: NEF Services MonitoringEvent'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/012-NEF MonitoringEvent', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                string(name: 'BUILD_ID', value: String.valueOf(BUILD_NUMBER)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }
        
        //Review Parameters
        stage('Certification: Scale replica NetApp '){
            steps{
                build job: '/003-NETAPPS/003-Helpers/016-Scale Netapp', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        stage('Certification: Shrink replicaSet NetApp '){
            steps{
                build job: '/003-NETAPPS/003-Helpers/017-Shrink Netapp', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Certification: NetApp OffBoarding'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/014-NetApp OffBoarding', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        
        stage('Certification: Destroy NetApp'){
            steps{
                script {
                    def jobBuild = build job: '/003-NETAPPS/003-Helpers/013-Destroy NetApp', wait: true, propagate: false,
                                    parameters: [string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH))]
                    def jobResult = jobBuild.getResult()
                    echo "Build of ' Deploy NetApp' returned result: ${jobResult}"
                    buildResults['destroy-netapp'] = jobResult
                }
            }
        }
 
        stage('Certification: Destroy NEF'){
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

        stage('Certification: Destroy CAPIF'){
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


        stage('Certification: OpenSource Licenses Report'){
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

        stage('Certification: Obtaining information for previous pipelines'){
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
        stage('Certification: Generate Final Report'){
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
