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
                sh '''
                    # Patch API Gateway routes to use Docker service names instead of localhost
                    sed -i "s|http://localhost:8081|http://user-service:8081|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8082|http://ingestion-service:8082|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8083|http://transaction-service:8083|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8084|http://budget-service:8084|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8086|http://notification-service:8086|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8087|http://chat-service:8087|g" services/api-gateway-service/src/main/resources/application.yml
                    sed -i "s|http://localhost:8000|http://forecasting-service:8000|g" services/api-gateway-service/src/main/resources/application.yml
                    
                    # Run the build
                    mvn -B -ntp clean package spring-boot:repackage -DskipTests
                '''
            }
        }

        stage('Build Frontend') {
            steps {
                dir('frontend') {
                    sh 'npm install'
                    sh 'npm run build'
                }
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

        stage('Build Docker Images') {
            steps {
                sh 'docker-compose build'
            }
        }

        stage('Push Docker Images to Docker Hub') {
            steps {
                script {
                    def services = [
                        'api-gateway', 'user-service', 'transaction-service', 
                        'budget-service', 'ingestion-service', 'notification-service', 
                        'admin-service', 'categorization-service', 'anomaly-detection-service', 
                        'chat-service', 'frontend'
                    ]
                    docker.withRegistry('', 'DockerHubCred') {
                        services.each { service ->
                            def imageName = "aayanksinghai/finsight-${service}"
                            // Push only the latest tag
                            sh "docker push ${imageName}:latest"
                        }
                    }
                }
            }
        }

        stage('Cleanup Local Images') {
            steps {
                sh 'docker image prune -f'
            }
        }

        stage('Deploy with Ansible') {
            steps {
                sh 'ansible-playbook -i inventory.ini deploy.yml'
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


