# FinSight - Intelligent Personal Finance Analytics Platform

FinSight is a microservices-based personal finance analytics platform with Spring Boot services, Python ML services, and a React frontend.

## Current Initialization Status

This repository is initialized with a multi-module Maven setup for the required Spring Boot microservices.

### Backend Maven modules created

- `services/api-gateway-service`
- `services/user-service`
- `services/ingestion-service`
- `services/transaction-service`
- `services/budget-service`
- `services/notification-service`
- `services/admin-service`
- `services/chat-service`

Each service includes:
- Independent `pom.xml`
- Spring Boot application entrypoint
- `application.yml` with service name, dedicated port, and health/prometheus exposure

## Required Tech Stack (from requirements)

### Frontend
- React 18 + TypeScript
- Vite
- Tailwind CSS
- Recharts
- TanStack Query
- Zustand
- React Router v6
- Axios
- `@react-oauth/google`

### Backend (Java)
- Java 21
- Spring Boot 3.x
- Spring Cloud Gateway
- Spring Security + OAuth2
- Spring Data JPA
- Spring Kafka
- Flyway
- WebFlux (chat-service)

### ML Services (Python)
- Python 3.11 + FastAPI
- pandas, scikit-learn, XGBoost, PyTorch, statsmodels, scipy
- MLflow

### Data and Infra
- PostgreSQL 16
- MongoDB
- Redis 7
- Kafka 3.x
- Docker / Docker Compose
- Kubernetes + HPA
- Jenkins
- Ansible
- Vault
- ELK + Prometheus + Grafana

## Feature-Slice Build Plan (very small, test-first increments)

Complete each slice, test manually, then move forward.

1. **Platform baseline**
   - Parent Maven + all Spring modules bootable
   - `/actuator/health` reachable per service
2. **Gateway routing baseline**
   - Route `/api/users/**` to `user-service`
   - Baseline JWT enforcement path in gateway
3. **User registration (email/password only)**
   - Register endpoint + validation + password hashing
4. **User login + JWT issue**
   - Login endpoint with RS256 token generation
5. **Google OAuth login bridge**
   - `id_token` verification + account link/create
6. **Statement upload job creation**
   - `ingestion-service` accepts CSV/XLS/PDF and stores ingestion job metadata
7. **Canonical transaction persistence**
   - `transaction-service` stores parsed transactions in canonical schema
8. **Manual transaction CRUD**
   - Add/edit/delete/filter transactions
9. **Transaction deduplication**
   - Date + amount + description hash checks
10. **Budget CRUD and usage progress**
    - Category budget set/get with utilization
11. **Budget 80% and 100% events**
    - Publish budget alerts to Kafka
12. **Notification inbox baseline**
    - Consume alert topics and persist notifications
13. **Dashboard aggregate API**
    - Monthly spend/income/savings + category summary
14. **Categorization ML integration**
    - `transaction-service` calls categorization FastAPI
15. **Anomaly ML integration**
    - ingestion event -> anomaly score -> alert
16. **Forecast integration**
    - next-month category forecast + confidence interval
17. **Stress score integration**
    - monthly composite score + trend endpoint
18. **What-if simulator orchestration**
    - simulate endpoint (no persistent side effects)
19. **Chat orchestration baseline**
    - intent routing + grounded response composition
20. **Admin operational controls**
    - model threshold config + retrain trigger + ingestion metrics

## What to implement next

Next recommended implementation slice:

- Add profile update and account deletion endpoints with role-aware authorization, then introduce JWT refresh/logout flow.

## Quick local auth smoke test

Current dev bootstrap login credentials:
- Email: `demo@finsight.local`
- Password: `Passw0rd!123`

```bash
mvn -q -pl services/user-service spring-boot:run
mvn -q -pl services/api-gateway-service spring-boot:run -Dspring-boot.run.arguments=--server.port=8090
curl -sS -X POST "http://localhost:8090/api/users/auth/register" -H "Content-Type: application/json" -d '{"email":"new-user@finsight.local","password":"StrongP@ss1"}'
curl -sS -X POST "http://localhost:8090/api/users/auth/login" -H "Content-Type: application/json" -d '{"email":"new-user@finsight.local","password":"StrongP@ss1"}'
```

## Build

```bash
mvn -DskipTests package
```

## Notes

- No additional documentation files are created; all planning and setup notes are kept in this `README.md`.
- Python ML services and frontend are intentionally not implemented yet in this step; they will be initialized in upcoming slices.
