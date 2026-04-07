# FinSight — Installation & Deployment Guide

Welcome to FinSight, an Intelligent Personal Finance Analytics Platform built with a microservices architecture. This guide provides instructions on how to run the fully containerized application on any system using Docker Compose.

## Prerequisites
- **Docker** Engine (v20+ recommended)
- **Docker Compose** installed (v2.0+)
- Ensure standard ports `5432` (PostgreSQL), `9092` (Kafka), `6379` (Redis), `8090` (API Gateway), and `5173` (Frontend) are available.

## Running the Application 🚀
The entire platform is configured to run automatically using Docker Compose. A single command provisions the required infrastructure (Kafka, ZooKeeper, PostgreSQL, Redis) and builds the backend microservices alongside the React frontend.

1. **Clone the Repository**
   ```bash
   git clone <repo-url>
   cd FinSight
   ```

2. **Start the Infrastructure and Microservices**
   ```bash
   docker-compose up -d
   ```
   *Note: This command will pull the latest optimized images directly from **Docker Hub**. This is the fastest way to get the platform running. If you want to build the images yourself from the source code, you must first run `mvn clean package -DskipTests` on your machine and then use `docker-compose up --build -d`.*

3. **Verify running containers**
   ```bash
   docker-compose ps
   ```

## Verifying Deployment
When all services show as `Up` (healthy), navigate to the configured endpoints:

* **FinSight Web UI:** `http://localhost:5175`
* **API Gateway:** `http://localhost:8090` (Handles routing to `/api/users`, `/ws/`, etc.)

## Component Overview
The composed stack provisions:
- **Frontend** (React + Vite + Nginx)
- **API Gateway** (Spring Cloud Gateway proxying microservices)
- **Core Microservices** (User, Budget, Transaction, Notification via Spring Data JPA & PostgreSQL)
- **AI/ML Layer** (Categorization & Anomaly Detection Services mapping NLP models via Python)
- **Kafka / Zookeeper** (Event streaming core for notifications)
- **Redis / PostgreSQL 16** (State layers)

## Checking Logs
If a specific service is misbehaving (e.g., waiting for Kafka connections to establish), you can isolate logs for a single microservice:
```bash
docker-compose logs -f user-service
docker-compose logs -f frontend
docker-compose logs -f notification-service
```

## Teardown
To spin down the architecture, securely power off containers, and delete the custom networks:
```bash
docker-compose down
```
If you wish to wipe the volumes (database data, kafka logs):
```bash
docker-compose down -v
```

## Continuous Integration / Jenkins
This repository includes a `Jenkinsfile` compatible with Jenkins Pipelines. 
To integrate with Jenkins:
1. Setup a multibranch pipeline job referencing this repository.
2. The pipeline handles Git Checkout, Maven Back-end packaging, NPM Front-end building, JUnit reports, and Docker Compose artifact builds automatically (`RUN_TESTS` optionally via parameters).
