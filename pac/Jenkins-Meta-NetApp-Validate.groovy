pipeline {
    agent {
        node {
            label 'evol5-slave'
        }
    }

    parameters {
        string(name: 'GIT_NETAPP_URL', defaultValue: 'https://github.com/EVOLVED-5G/dummy-netapp', description: 'URL of the Github Repository')
        string(name: 'GIT_NETAPP_BRANCH', defaultValue: 'evolved5g', description: 'NETAPP branch name')
        string(name: 'GIT_CICD_BRANCH', defaultValue: 'develop', description: 'Deployment git branch name')
        booleanParam(name: 'REPORTING', defaultValue: false, description: 'Save report into artifactory')
    }

    stages {

        stage('Validation: Static Code Analysis'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/001-Static Code Analysis', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Security Scan Code'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/002-Security Scan Code', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Security Scan Secrets'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/003-Security Scan Secrets', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Get docker Image from Registry'){
            steps{
                build job: '/100-HELPERS/001-Get Docker Image', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }
        //Review Parameters
        stage('Validation: Security Scan Docker Images'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/004-Security Scan Docker Images', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Upload Docker Images'){
            steps{
                build job: '/100-HELPERS/002-Upload Docker Image', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Deploy NetApp'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/005-Deploy NetApp', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Test NetApp Networking'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/006-Test NetApp Networking', wait: true, propagate: false,
                     parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: NetApp Onboarding Sucessfull'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/007-NetApp Onboarding Successful', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Discover NetApp API from CAPIF'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/008-Discover NetApp API from CAPIF', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Discover NetApp Callback CAPIF'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/009-NetApp Callback CAPIF', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: NEF Services as SessionWithQoS'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/010-NEF Services asSessionWithQoS', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
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
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Destroy NetApp'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/013-Destroy NetApp', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: NetApp OffBoarding'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/014-NetApp OffBoarding', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: OpenSource Licenses Report'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/015-OpenSource Licenses Report', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
                                booleanParam(name: 'REPORTING', value: String.valueOf(REPORTING))]
            }
        }

        //Review Parameters
        stage('Validation: Generate Final Report'){
            steps{
                build job: '/003-NETAPPS/003-Helpers/100-Generate Final Report', wait: true, propagate: false,
                    parameters: [string(name: 'GIT_NETAPP_URL', value: String.valueOf(GIT_NETAPP_URL)),
                                string(name: 'GIT_NETAPP_BRANCH', value: String.valueOf(GIT_NETAPP_BRANCH)),
                                string(name: 'GIT_CICD_BRANCH', value: String.valueOf(GIT_CICD_BRANCH)),
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
                replyTo: "no-reply@tid.es",
                recipientProviders: [[$class: 'DevelopersRecipientProvider'], [$class: 'RequesterRecipientProvider']]
        }
    }
}
