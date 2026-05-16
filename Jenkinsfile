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
        MAVEN_OPTS   = '-Dmaven.repo.local=.m2/repository'
        KUBECONFIG   = "${env.HOME}/.kube/config"
        K8S_NS       = 'finsight'
        K8S_DIR      = 'k8s'
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
                        'chat-service', 'forecasting-service', 'frontend'
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

        stage('Deploy to Kubernetes') {
            steps {
                script {
                    // 1. Namespace (must exist before anything else)
                    sh "kubectl apply -f ${K8S_DIR}/namespace.yaml"

                    // 2. Secrets & ConfigMap
                    sh "kubectl apply -f ${K8S_DIR}/secrets.yaml    -n ${K8S_NS}"
                    sh "kubectl apply -f ${K8S_DIR}/configmap.yaml  -n ${K8S_NS}"

                    // 3. Infrastructure layer  (Postgres, MongoDB, Redis, Kafka)
                    sh "kubectl apply -f ${K8S_DIR}/infra/postgres.yaml -n ${K8S_NS}"
                    sh "kubectl apply -f ${K8S_DIR}/infra/mongodb.yaml  -n ${K8S_NS}"
                    sh "kubectl apply -f ${K8S_DIR}/infra/redis.yaml    -n ${K8S_NS}"
                    sh "kubectl apply -f ${K8S_DIR}/infra/kafka.yaml    -n ${K8S_NS}"

                    // 4. Application services
                    def services = [
                        'user-service',
                        'ingestion-service',
                        'transaction-service',
                        'budget-service',
                        'notification-service',
                        'chat-service',
                        'categorization-service',
                        'anomaly-detection-service',
                        'forecasting-service',
                        'admin-service',
                        'api-gateway',
                        'frontend'
                    ]
                    services.each { svc ->
                        sh "kubectl apply -f ${K8S_DIR}/services/${svc}.yaml -n ${K8S_NS}"
                    }

                    // 5. Force pods to re-pull :latest images that were just pushed
                    services.each { svc ->
                        sh "kubectl rollout restart deployment/${svc} -n ${K8S_NS}"
                    }

                    // 6. Wait for all deployments to roll out successfully
                    services.each { svc ->
                        sh "kubectl rollout status deployment/${svc} -n ${K8S_NS} --timeout=180s"
                    }
                }
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


