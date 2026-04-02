import json
import logging
from kafka import KafkaProducer
from app.core.config import settings

logger = logging.getLogger(__name__)

class AnomalyKafkaProducer:
    def __init__(self):
        self.producer = None
        self._init_producer()

    def _init_producer(self):
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=settings.KAFKA_BROKERS,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                retries=3
            )
            logger.info("Kafka producer initialized.")
        except Exception as e:
            logger.error(f"Failed to initialize Kafka producer: {e}")

    def send_alert(self, transaction_id: str, owner_email: str, anomaly_details: dict):
        if not self.producer:
            logger.warning("Kafka producer not healthy, ignoring send.")
            return

        topic = settings.KAFKA_TOPIC_ALERTS
        payload = {
            "transactionId": transaction_id,
            "ownerEmail": owner_email,
            "isAnomalous": True,
            "anomalyScore": anomaly_details.get("anomaly_score", 0.0),
            "explanation": anomaly_details.get("explanation", "")
        }

        try:
            self.producer.send(
                topic,
                key=owner_email,
                value=payload
            )
            self.producer.flush()
            logger.info(f"Published anomaly alert to {topic} for transaction {transaction_id}")
        except Exception as e:
            logger.error(f"Error producing Kafka message to {topic}: {e}")

anomaly_producer = AnomalyKafkaProducer()
