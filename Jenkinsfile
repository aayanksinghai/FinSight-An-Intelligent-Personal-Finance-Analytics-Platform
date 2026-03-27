pipeline {
    agent any

    parameters {
        booleanParam(name: 'RUN_TESTS', defaultValue: false, description: 'Run Maven tests after packaging')
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    environment {
        MAVEN_OPTS = '-Dmaven.repo.local=.m2/repository'
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
                sh 'git --no-pager log -1 --oneline | cat'
            }
        }

        stage('Build') {
            steps {
                sh 'mvn -B -ntp clean package -DskipTests'
            }
        }

        stage('Test') {
            when {
                expression { return params.RUN_TESTS }
            }
            steps {
                sh 'mvn -B -ntp test'
            }
        }

        stage('Archive Artifacts') {
            steps {
                archiveArtifacts artifacts: 'services/*/target/*.jar', fingerprint: true, allowEmptyArchive: true
            }
        }
    }

    post {
        always {
            junit testResults: '**/target/surefire-reports/*.xml', allowEmptyResults: true
            junit testResults: '**/target/failsafe-reports/*.xml', allowEmptyResults: true
        }
        failure {
            echo 'Build failed. Check test reports and console logs for details.'
        }
    }
}


