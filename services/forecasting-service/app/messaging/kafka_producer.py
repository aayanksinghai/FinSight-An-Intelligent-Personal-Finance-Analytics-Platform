import json
import logging
import os
from kafka import KafkaProducer

logger = logging.getLogger(__name__)

class ForecastingKafkaProducer:
    def __init__(self):
        self.producer = None
        self.bootstrap_servers = os.getenv("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
        self._init_producer()

    def _init_producer(self):
        try:
            self.producer = KafkaProducer(
                bootstrap_servers=self.bootstrap_servers,
                value_serializer=lambda v: json.dumps(v).encode('utf-8'),
                key_serializer=lambda k: k.encode('utf-8') if k else None,
                retries=3
            )
            logger.info(f"Forecasting Kafka producer initialized for {self.bootstrap_servers}")
        except Exception as e:
            logger.error(f"Failed to initialize Kafka producer: {e}")

    def send_stress_score_alert(self, owner_email: str, new_score: float, previous_score: float):
        if not self.producer:
            return

        topic = "stress.score.alerts"
        payload = {
            "ownerEmail": owner_email,
            "newScore": new_score,
            "previousScore": previous_score,
            "type": "STRESS_SCORE_CHANGE"
        }

        try:
            self.producer.send(topic, key=owner_email, value=payload)
            self.producer.flush()
            logger.info(f"Published stress score alert for {owner_email}")
        except Exception as e:
            logger.error(f"Error producing Kafka message to {topic}: {e}")

    def send_forecast_update_alert(self, owner_email: str, month_year: str):
        if not self.producer:
            return

        topic = "forecast.alerts"
        payload = {
            "ownerEmail": owner_email,
            "monthYear": month_year,
            "type": "FORECAST_UPDATE"
        }

        try:
            self.producer.send(topic, key=owner_email, value=payload)
            self.producer.flush()
            logger.info(f"Published forecast update alert for {owner_email}")
        except Exception as e:
            logger.error(f"Error producing Kafka message to {topic}: {e}")

# Global instance
kafka_producer = ForecastingKafkaProducer()
