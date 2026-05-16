# FinSight — Intelligent Personal Finance Analytics Platform

> A finance platform built on a microservices architecture. FinSight helps users track spending, detect anomalies, forecast expenses, and interact with their financial data through a conversational AI assistant.

[![Docker Hub](https://img.shields.io/badge/Docker%20Hub-aayanksinghai-blue?logo=docker)](https://hub.docker.com/repositories/aayanksinghai)
[![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-green?logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?logo=react)](https://react.dev/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Minikube-326CE5?logo=kubernetes)](https://minikube.sigs.k8s.io/)
[![Jenkins](https://img.shields.io/badge/CI%2FCD-Jenkins-D24939?logo=jenkins)](https://www.jenkins.io/)

---

## Table of Contents

1. [Project Description](#project-description)
2. [Scope](#scope)
3. [Architecture](#architecture)
4. [Tech Stack](#tech-stack)
5. [Microservices & Docker Images](#microservices--docker-images)
6. [Prerequisites](#prerequisites)
7. [Environment Configuration](#environment-configuration)
8. [Running Locally (Docker Compose)](#running-locally-docker-compose)
9. [Deploying via Kubernetes](#deploying-via-kubernetes)
10. [CI/CD — Jenkins Pipeline](#cicd--jenkins-pipeline)
11. [Service Endpoints](#service-endpoints)
12. [Project Structure](#project-structure)

---

## Project Description

FinSight is a full-stack, microservices-based personal finance analytics platform designed to give users actionable insights into their financial health. Users can upload bank statements (CSV/XLS/PDF), manually manage transactions, set category-level budgets, and receive real-time alerts when spending thresholds are breached.

The platform is powered by a suite of ML services that automatically **categorize** transactions using NLP, **detect anomalies** in spending patterns, and **forecast** next-month expenses with confidence intervals. An AI Chatbot (backed by Google Gemini) enables natural-language queries over a user's own financial data.

The entire platform is containerized, deployed on Kubernetes with Horizontal Pod Autoscalers (HPA), and delivered through a Jenkins-based CI/CD pipeline. Observability is provided end-to-end via the ELK Stack (Elasticsearch, Logstash, Kibana).

---

## Scope

| Domain | Features |
|--------|----------|
| **Authentication** | Email/password registration & login, Google OAuth 2.0 (PKCE), RS256 JWT tokens, Redis session management |
| **Statement Ingestion** | CSV/XLS/PDF upload, async parsing via Kafka, MongoDB metadata storage, deduplication |
| **Transaction Management** | Manual CRUD, search/filter by category/date/amount, canonical schema with Flyway migrations |
| **Budgeting** | Per-category monthly budgets, real-time utilization tracking, 80% & 100% threshold Kafka events |
| **Notifications** | Real-time WebSocket/STOMP push alerts, persistent notification inbox |
| **ML — Categorization** | NLP-based automatic transaction classification (FastAPI + scikit-learn) |
| **ML — Anomaly Detection** | Statistical anomaly scoring on spend patterns, alert publishing to Kafka (FastAPI) |
| **ML — Forecasting** | Next-month expense forecasting with confidence intervals (FastAPI + statsmodels/XGBoost) |
| **AI Chatbot** | Gemini-powered conversational assistant grounded in the user's own transaction data |
| **Admin Panel** | Model threshold configuration, retrain triggers, ingestion metrics monitoring |
| **Observability** | Centralized logging via ELK, distributed tracing, Prometheus metrics via `/actuator` |
| **Infrastructure** | Kubernetes + HPA, Jenkins CI/CD, Docker Compose (local dev) |

---

## Architecture

```
                         ┌─────────────────────────────────┐
                         │           React Frontend          │
                         │    (Vite + TypeScript + Nginx)    │
                         └───────────────┬─────────────────┘
                                         │  HTTP (NodePort :30080)
                         ┌───────────────▼─────────────────┐
                         │          API Gateway              │
                         │   (Spring Cloud Gateway :8090)    │
                         └──┬───┬───┬───┬───┬───┬───┬──────┘
                            │   │   │   │   │   │   │
              ┌─────────────┘   │   │   │   │   │   └──────────────┐
              ▼                 ▼   ▼   ▼   ▼   ▼                  ▼
        user-service    ingestion  txn  budget  notif  chat   admin-service
          :8081          :8082   :8083  :8084  :8086  :8087     :8088
              │              │             │      │
         ┌────▼────┐   ┌─────▼──────┐     │    Kafka ──► anomaly-detection
         │PostgreSQL│   │  MongoDB   │     │      │        categorization
         └─────────┘   └────────────┘     │      └──────► notification
              │                           │
           Redis                   forecasting-service
          (cache)                     :8000 (FastAPI)
              │
        ┌─────▼────────────────────────────────┐
        │             ELK Stack                 │
        │  Logstash :5044 → ES :9200 → Kibana   │
        └──────────────────────────────────────┘
```

---

## Tech Stack

### Frontend
| Technology | Version | Purpose |
|------------|---------|---------|
| React | 18 | UI framework |
| TypeScript | 5.x | Type safety |
| Vite | 5.x | Build tool & dev server |
| Tailwind CSS | 3.x | Utility-first styling |
| Recharts | 2.x | Financial charts & visualizations |
| TanStack Query | 5.x | Server state management & caching |
| Zustand | 4.x | Client state management |
| React Router | v6 | Client-side routing |
| Axios | 1.x | HTTP client |
| @react-oauth/google | — | Google OAuth PKCE flow |

### Backend (Java / Spring Boot)
| Technology | Version | Purpose |
|------------|---------|---------|
| Java | 21 | Runtime |
| Spring Boot | 3.x | Microservice framework |
| Spring Cloud Gateway | — | API gateway & routing |
| Spring Security + OAuth2 Resource Server | — | JWT authentication |
| Spring Data JPA | — | PostgreSQL ORM |
| Spring Data MongoDB | — | MongoDB ODM |
| Spring Data Redis | — | Session & token caching |
| Spring Kafka | — | Event-driven messaging |
| Spring WebFlux | — | Reactive streams (chat-service) |
| Flyway | — | DB schema migrations |
| Spring Boot Actuator | — | Health, metrics, Prometheus |

### ML Services (Python / FastAPI)
| Technology | Purpose |
|------------|---------|
| Python 3.11 + FastAPI | ML service framework |
| pandas, NumPy | Data processing |
| scikit-learn | Transaction categorization |
| XGBoost | Forecasting model |
| statsmodels | Time-series analysis |
| PyTorch | Deep learning support |
| MLflow | Model tracking & registry |
| Kafka-Python | Event consumption |

### Data & Infrastructure
| Technology | Role |
|------------|------|
| PostgreSQL 15 | Primary relational store (users, transactions, budgets) |
| MongoDB 7.0 | Document store (ingestion jobs, raw statements) |
| Redis 7 | Session cache, rate limiting |
| Apache Kafka 3.x (Confluent) | Async event bus |
| Docker / Docker Compose | Local containerization |
| Kubernetes (Minikube) | Production orchestration + HPA |
| Jenkins | CI/CD pipeline |
| ELK Stack 8.11 | Centralized logging & observability |
| Google Gemini API | AI Chatbot LLM backend |

---

## Microservices & Docker Images

All images are published to **[Docker Hub — aayanksinghai](https://hub.docker.com/repositories/aayanksinghai)**.

| Service | Port | Docker Image | Description |
|---------|------|-------------|-------------|
| `api-gateway` | 8090 | [`aayanksinghai/finsight-api-gateway`](https://hub.docker.com/r/aayanksinghai/finsight-api-gateway) | Spring Cloud Gateway — JWT enforcement & routing |
| `user-service` | 8081 | [`aayanksinghai/finsight-user-service`](https://hub.docker.com/r/aayanksinghai/finsight-user-service) | Auth: register, login, Google OAuth, JWT issuance |
| `ingestion-service` | 8082 | [`aayanksinghai/finsight-ingestion-service`](https://hub.docker.com/r/aayanksinghai/finsight-ingestion-service) | Bank statement upload & async parsing |
| `transaction-service` | 8083 | [`aayanksinghai/finsight-transaction-service`](https://hub.docker.com/r/aayanksinghai/finsight-transaction-service) | Transaction CRUD, search, deduplication |
| `budget-service` | 8084 | [`aayanksinghai/finsight-budget-service`](https://hub.docker.com/r/aayanksinghai/finsight-budget-service) | Budget management & threshold alerts |
| `notification-service` | 8086 | [`aayanksinghai/finsight-notification-service`](https://hub.docker.com/r/aayanksinghai/finsight-notification-service) | WebSocket/STOMP push notifications |
| `chat-service` | 8087 | [`aayanksinghai/finsight-chat-service`](https://hub.docker.com/r/aayanksinghai/finsight-chat-service) | Gemini-powered AI financial chatbot |
| `admin-service` | 8088 | [`aayanksinghai/finsight-admin-service`](https://hub.docker.com/r/aayanksinghai/finsight-admin-service) | Admin controls, model config & retrain |
| `categorization-service` | — | [`aayanksinghai/finsight-categorization-service`](https://hub.docker.com/r/aayanksinghai/finsight-categorization-service) | FastAPI — ML transaction categorization |
| `anomaly-detection-service` | — | [`aayanksinghai/finsight-anomaly-detection-service`](https://hub.docker.com/r/aayanksinghai/finsight-anomaly-detection-service) | FastAPI — Anomaly scoring & Kafka alerts |
| `forecasting-service` | 8000 | [`aayanksinghai/finsight-forecasting-service`](https://hub.docker.com/r/aayanksinghai/finsight-forecasting-service) | FastAPI — Expense forecasting with CI |
| `frontend` | 80 | [`aayanksinghai/finsight-frontend`](https://hub.docker.com/r/aayanksinghai/finsight-frontend) | React + Nginx SPA |

---

## Prerequisites

| Tool | Minimum Version | Notes |
|------|----------------|-------|
| Docker | 24.x | With Docker Compose v2 |
| `kubectl` | 1.28+ | For Kubernetes deployments |
| Minikube | 1.32+ | Local Kubernetes cluster |
| Java JDK | 21 | For local builds |
| Maven | 3.9+ | Multi-module build |
| Node.js | 20 LTS | For frontend builds |
| Jenkins | 2.440+ | With Docker & Pipeline plugins |

---


## Running Locally (Docker Compose)

### 1. Build all Java services and the frontend

```bash
# Build Spring Boot JARs (skip tests for speed)
mvn -B clean package spring-boot:repackage -DskipTests

# Build the React frontend
cd frontend && npm install && npm run build && cd ..
```

### 2. Start the full stack

```bash
docker-compose up -d
```

This starts all services in the correct dependency order:

```
Zookeeper → Kafka → Redis → PostgreSQL → MongoDB
         → Elasticsearch → Logstash → Kibana
         → [all microservices]
         → Frontend (Nginx)
```

### 3. Verify services are up

```bash
docker-compose ps
```

### 4. Access the application

| Service | URL |
|---------|-----|
| **Frontend** | http://localhost:5175 |
| **API Gateway** | http://localhost:8095 |
| **Kibana** | http://localhost:5601 |
| **Elasticsearch** | http://localhost:9200 |
| **Forecasting API** | http://localhost:8000/docs |
| **Admin Service** | http://localhost:8088 |

### 5. Tear down

```bash
docker-compose down -v   # -v also removes named volumes
```

---

## Deploying via Kubernetes

### Prerequisites

```bash
# Start Minikube
minikube start --memory=6144 --cpus=4

# Point Docker daemon to Minikube's registry (for local images)
eval $(minikube docker-env)
```

### Step 1 — Apply Namespace

```bash
kubectl apply -f k8s/namespace.yaml
```

### Step 2 — Apply Secrets & ConfigMap

```bash
kubectl apply -f k8s/secrets.yaml   -n finsight
kubectl apply -f k8s/configmap.yaml -n finsight
```

> ⚠️ `secrets.yaml` contains base64-encoded credentials. **Do not commit real secrets** to version control. Use [Sealed Secrets](https://github.com/bitnami-labs/sealed-secrets) or a Vault integration for production.

### Step 3 — Deploy Infrastructure

```bash
kubectl apply -f k8s/infra/postgres.yaml -n finsight
kubectl apply -f k8s/infra/mongodb.yaml  -n finsight
kubectl apply -f k8s/infra/redis.yaml    -n finsight
kubectl apply -f k8s/infra/kafka.yaml    -n finsight
```

Wait for infra pods to be ready:

```bash
kubectl wait --for=condition=ready pod -l app=postgres  -n finsight --timeout=120s
kubectl wait --for=condition=ready pod -l app=mongodb   -n finsight --timeout=120s
kubectl wait --for=condition=ready pod -l app=redis     -n finsight --timeout=60s
kubectl wait --for=condition=ready pod -l app=kafka     -n finsight --timeout=120s
```

### Step 4 — Deploy All Services

```bash
kubectl apply -f k8s/services/user-service.yaml              -n finsight
kubectl apply -f k8s/services/ingestion-service.yaml         -n finsight
kubectl apply -f k8s/services/transaction-service.yaml       -n finsight
kubectl apply -f k8s/services/budget-service.yaml            -n finsight
kubectl apply -f k8s/services/notification-service.yaml      -n finsight
kubectl apply -f k8s/services/chat-service.yaml              -n finsight
kubectl apply -f k8s/services/categorization-service.yaml    -n finsight
kubectl apply -f k8s/services/anomaly-detection-service.yaml -n finsight
kubectl apply -f k8s/services/forecasting-service.yaml       -n finsight
kubectl apply -f k8s/services/admin-service.yaml             -n finsight
kubectl apply -f k8s/services/api-gateway.yaml               -n finsight
kubectl apply -f k8s/services/frontend.yaml                  -n finsight
```

### Step 5 — Access the Application

```bash
# Get the Minikube node IP
minikube ip

# Frontend is exposed at NodePort 30080
open http://$(minikube ip):30080

# API Gateway is exposed at NodePort 30090
# Base URL: http://$(minikube ip):30090
```

### Useful Kubectl Commands

```bash
# Watch all pods in the finsight namespace
kubectl get pods -n finsight -w

# Check logs for a specific service
kubectl logs -f deployment/api-gateway -n finsight

# Describe a failing pod
kubectl describe pod <pod-name> -n finsight

# Scale a deployment manually
kubectl scale deployment transaction-service --replicas=3 -n finsight

# Force re-pull of latest Docker images
kubectl rollout restart deployment/api-gateway -n finsight

# Delete all resources in the namespace
kubectl delete namespace finsight
```

---

## CI/CD — Jenkins Pipeline

The `Jenkinsfile` at the project root defines a full CI/CD pipeline with the following stages:

```
Checkout → Build (Maven) → Build Frontend → Test → Archive Artifacts
       → Build Docker Images → Push to Docker Hub → Cleanup → Deploy to Kubernetes
```

### Stage Summary

| Stage | Description |
|-------|-------------|
| **Checkout** | Clones the repository via SCM |
| **Build** | Patches API Gateway routes for Docker DNS, then runs `mvn clean package spring-boot:repackage -DskipTests` |
| **Build Frontend** | Runs `npm install && npm run build` in the `frontend/` directory |
| **Test** | Runs `mvn test` (gated by the `RUN_TESTS` boolean parameter) |
| **Archive Artifacts** | Archives all `*.jar` files with fingerprinting |
| **Build Docker Images** | Runs `docker-compose build` for all 12 images |
| **Push to Docker Hub** | Pushes `aayanksinghai/finsight-*:latest` using the `DockerHubCred` Jenkins credential |
| **Cleanup Local Images** | Prunes dangling images to reclaim disk space |
| **Deploy to Kubernetes** | Applies k8s manifests in order (namespace → secrets → configmap → infra → services), then forces a rolling restart and waits for all deployments to succeed |

### Jenkins Setup

#### 1. Required Jenkins Plugins
- **Pipeline** (pre-installed)
- **Docker Pipeline**
- **Git**
- **Timestamper**

#### 2. Required Jenkins Credentials

| Credential ID | Type | Description |
|--------------|------|-------------|
| `DockerHubCred` | Username & Password | Docker Hub — username: `aayanksinghai` |

#### 3. Required Tools on Jenkins Agent
- Docker (with daemon socket accessible)
- `kubectl` configured with `~/.kube/config` pointing to the target cluster
- Java 21 JDK
- Maven 3.9+
- Node.js 20 LTS

#### 4. Create the Pipeline Job

```
Jenkins → New Item → Pipeline
  → Pipeline Definition: "Pipeline script from SCM"
  → SCM: Git
  → Repository URL: <your-repo-url>
  → Script Path: Jenkinsfile
```

#### 5. Trigger a Build

```bash
# Trigger via CLI (requires jenkins-cli.jar)
java -jar jenkins-cli.jar -s http://localhost:8080 build FinSight -p RUN_TESTS=true -v -f

# Or trigger with tests skipped (default)
java -jar jenkins-cli.jar -s http://localhost:8080 build FinSight -v -f
```

#### 6. Pipeline Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `MAVEN_OPTS` | `-Dmaven.repo.local=.m2/repository` | Local Maven cache |
| `KUBECONFIG` | `~/.kube/config` | Path to kubeconfig |
| `K8S_NS` | `finsight` | Target Kubernetes namespace |
| `K8S_DIR` | `k8s` | Path to k8s manifests directory |
| `RUN_TESTS` *(param)* | `false` | Boolean to enable Maven test phase |

---

## Service Endpoints

All endpoints below are accessed through the **API Gateway** at `:8090` (local) or `:30090` (Kubernetes NodePort).

| Route Prefix | Upstream Service | Description |
|-------------|-----------------|-------------|
| `/api/users/**` | user-service:8081 | Auth, profile management |
| `/api/ingestion/**` | ingestion-service:8082 | Statement upload & status |
| `/api/transactions/**` | transaction-service:8083 | Transaction CRUD & search |
| `/api/budgets/**` | budget-service:8084 | Budget management |
| `/api/notifications/**` | notification-service:8086 | Notification inbox |
| `/api/chat/**` | chat-service:8087 | AI Chatbot |
| `/api/forecasting/**` | forecasting-service:8000 | Expense forecasts |
| `/api/admin/**` | admin-service:8088 | Admin controls |

---

## Project Structure

```
FinSight/
├── Jenkinsfile                    # CI/CD pipeline definition
├── docker-compose.yml             # Full local stack (all services)
├── docker-compose-infra.yml       # Infrastructure only (DB, Kafka, ELK)
├── pom.xml                        # Parent Maven POM (multi-module)
├── .env.example                   # Environment variable template
├── inventory.ini                  # Ansible inventory (localhost)
├── deploy.yml                     # Ansible playbook (Docker Compose deploy)
│
├── k8s/                           # Kubernetes manifests
│   ├── namespace.yaml
│   ├── secrets.yaml
│   ├── configmap.yaml
│   ├── infra/
│   │   ├── postgres.yaml
│   │   ├── mongodb.yaml
│   │   ├── redis.yaml
│   │   └── kafka.yaml             # Includes Zookeeper + Kafka
│   ├── services/
│   │   ├── api-gateway.yaml       # NodePort :30090
│   │   ├── user-service.yaml
│   │   ├── ingestion-service.yaml
│   │   ├── transaction-service.yaml
│   │   ├── budget-service.yaml
│   │   ├── notification-service.yaml
│   │   ├── chat-service.yaml
│   │   ├── admin-service.yaml
│   │   ├── categorization-service.yaml
│   │   ├── anomaly-detection-service.yaml
│   │   ├── forecasting-service.yaml
│   │   └── frontend.yaml          # NodePort :30080
│   └── elk/
│
├── services/                      # Spring Boot microservices
│   ├── api-gateway-service/
│   ├── user-service/
│   ├── ingestion-service/
│   ├── transaction-service/
│   ├── budget-service/
│   ├── notification-service/
│   ├── chat-service/
│   ├── admin-service/
│   ├── categorization-service/    # Python FastAPI
│   ├── anomaly-detection-service/ # Python FastAPI
│   └── forecasting-service/       # Python FastAPI
│
├── frontend/                      # React + TypeScript SPA
│   ├── src/
│   └── vite.config.ts
│
└── elk/                           # ELK Stack configuration
    ├── logstash/
    └── kibana/
```

---

<p align="center">Built with ❤️ as part of the SPE Major Project — 2026</p>
