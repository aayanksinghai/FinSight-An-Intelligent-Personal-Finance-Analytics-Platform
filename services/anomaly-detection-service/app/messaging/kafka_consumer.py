import json
import logging
import threading
from kafka import KafkaConsumer
from app.core.config import settings
from app.services.anomaly_service import anomaly_service
from app.messaging.kafka_producer import anomaly_producer

logger = logging.getLogger(__name__)

class AnomalyKafkaConsumer:
    def __init__(self):
        self.consumer = None
        self.running = False

    def start(self):
        self.running = True
        thread = threading.Thread(target=self._consume_loop, daemon=True)
        thread.start()

    def stop(self):
        self.running = False
        if self.consumer:
            self.consumer.close()

    def _consume_loop(self):
        try:
            self.consumer = KafkaConsumer(
                settings.KAFKA_TOPIC_INGESTED,
                bootstrap_servers=settings.KAFKA_BROKERS,
                group_id=settings.KAFKA_GROUP_ID,
                value_deserializer=lambda m: json.loads(m.decode('utf-8')) if m else None,
                auto_offset_reset='latest'
            )
            logger.info(f"Kafka consumer started on topic {settings.KAFKA_TOPIC_INGESTED}")
            
            for message in self.consumer:
                if not self.running:
                    break
                
                transaction = message.value
                if not transaction:
                    continue
                    
                transaction_id = transaction.get("id") or transaction.get("jobId", "unknown")
                owner_email = transaction.get("ownerEmail", "unknown")
                
                # Perform scoring
                score_result = anomaly_service.score_transaction(transaction)
                
                # If anomalous, send alert
                if score_result.get("is_anomalous"):
                    logger.warning(f"Anomaly detected for tx {transaction_id}. Score: {score_result['anomaly_score']}")
                    anomaly_producer.send_alert(
                        transaction_id=transaction_id,
                        owner_email=owner_email,
                        anomaly_details=score_result
                    )
                    
        except Exception as e:
            logger.error(f"Kafka consumer error: {e}")

anomaly_consumer = AnomalyKafkaConsumer()
