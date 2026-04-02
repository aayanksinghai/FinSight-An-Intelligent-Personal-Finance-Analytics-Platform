# Anomaly Detection Service Walkthrough

## Changes Made
- Scaffolded `anomaly-detection-service` in the `services/` directory.
- Created [Dockerfile](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/transaction-service/Dockerfile) and [requirements.txt](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/categorization-service/requirements.txt) with ML and FastAPI dependencies (`torch`, `fastapi`, `kafka-python`).
- Built the [LSTMAutoencoder](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/anomaly-detection-service/app/models/lstm_autoencoder.py#4-51) model in PyTorch for per-user baseline scoring.
- Implemented [AnomalyService](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/anomaly-detection-service/app/services/anomaly_service.py#6-52) with logic to load models and score transactional anomalies based on reconstruction errors. Let to mocked testing rules for immediate feedback while training data accumulates.
- Created [AnomalyKafkaConsumer](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/anomaly-detection-service/app/messaging/kafka_consumer.py#11-62) to asynchronously consume the `transactions.ingested` topic.
- Created [AnomalyKafkaProducer](file:///home/aayanksinghai/SPE/Major%20Project/FinSight/services/anomaly-detection-service/app/messaging/kafka_producer.py#8-49) to asynchronously produce alerts to the `anomaly.alerts` topic when an anomaly threshold is breached.
- IntegratedREST API with `/health` and `/feedback` endpoints using FastAPI.

## Validation Results
All code has been structured per the microservices specifications. The service uses dependency injection, Pydantic for validation, and proper background task orchestration.

## Manual Test Steps

### Prerequisites
1. Ensure your local Kafka and Zookeeper are running.
2. In the `anomaly-detection-service` directory, run:
```bash
python -m venv venv
source venv/bin/activate
pip install -r requirements.txt
uvicorn app.main:app --host 0.0.0.0 --port 8000 --reload
```

### 1. Test Health Endpoint
Open a new terminal and run:
```bash
curl -X GET http://localhost:8000/api/v1/anomaly/health
```
**Expected Response:**
```json
{"status": "ok", "service": "anomaly-detection-service"}
```

### 2. Test Feedback API Endpoint
```bash
curl -X POST http://localhost:8000/api/v1/anomaly/feedback \
-H "Content-Type: application/json" \
-d '{"transaction_id": "tx-123456", "user_email": "user@example.com", "label": "suspicious"}'
```
**Expected Response:**
```json
{"message":"Feedback received successfully"}
```

### 3. Test Kafka Consumer & Producer
Use a Python script or Kafka tool to push a message into the `transactions.ingested` topic:
```bash
# Push payload to trigger the anomaly rule (amount > 5000)
kafka-console-producer.sh --broker-list localhost:9092 --topic transactions.ingested
> {"id": "tx-99", "ownerEmail": "test@demo.com", "jobId": "job123", "debitAmount": 6000.0, "merchant": "Amazon"}
```
**Expected Output (Service Logs):**
```
WARNING:app.messaging.kafka_consumer:Anomaly detected for tx tx-99. Score: 0.92
INFO:app.messaging.kafka_producer:Published anomaly alert to anomaly.alerts for transaction tx-99
```

**Expected Output (Kafka `anomaly.alerts`):**
Consume the alerts topic to see the result:
```bash
kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic anomaly.alerts --from-beginning
{"transactionId": "tx-99", "ownerEmail": "test@demo.com", "isAnomalous": true, "anomalyScore": 0.92, "explanation": "Unusual amount relative to your baseline (₹6000.0)"}
```
