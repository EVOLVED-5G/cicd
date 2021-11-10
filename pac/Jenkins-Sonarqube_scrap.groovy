pipeline {
    agent { node {label 'evol5-slave2'}  }


    stages {
        stage('Scrap Sonarqube') {
            steps {
                sh '''
                curl -u admin:admin "http://evol5-sonarqube.hi.inet:9000/api/measures/component?additionalFields=period%2Cmetrics&component=Evolved5g-master&metricKeys=alert_status%2Cquality_gate_details%2Cbugs%2Cnew_bugs%2Creliability_rating%2Cnew_reliability_rating%2Cvulnerabilities%2Cnew_vulnerabilities%2Csecurity_rating%2Cnew_security_rating%2Csecurity_hotspots%2Cnew_security_hotspots%2Csecurity_hotspots_reviewed%2Cnew_security_hotspots_reviewed%2Csecurity_review_rating%2Cnew_security_review_rating%2Ccode_smells%2Cnew_code_smells%2Csqale_rating%2Cnew_maintainability_rating%2Csqale_index%2Cnew_technical_debt%2Ccoverage%2Cnew_coverage%2Clines_to_cover%2Cnew_lines_to_cover%2Ctests%2Cduplicated_lines_density%2Cnew_duplicated_lines_density%2Cduplicated_blocks%2Cncloc%2Cncloc_language_distribution%2Cprojects%2Clines%2Cnew_lines%2Cnew_blocker_violations%2Cnew_critical_violations"
                '''
            }
        }
    }
}