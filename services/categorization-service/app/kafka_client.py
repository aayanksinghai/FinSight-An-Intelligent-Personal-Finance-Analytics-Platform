import os
import json
import logging
import threading
from confluent_kafka import Consumer, Producer

from .ml_model import guess_category, clean_merchant

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

KAFKA_BROKER = os.environ.get("KAFKA_BOOTSTRAP_SERVERS", "localhost:9092")
CONSUMER_GROUP = "categorization-service-group"
TOPIC_IN = "transactions.created"
TOPIC_OUT = "transactions.categorized"

class KafkaWorker(threading.Thread):
    def __init__(self):
        super().__init__(daemon=True)
        self._running = True
        
    def stop(self):
        self._running = False
        
    def run(self):
        logger.info(f"Starting Kafka consumer thread on {KAFKA_BROKER} topic {TOPIC_IN}")
        
        consumer = Consumer({
            "bootstrap.servers": KAFKA_BROKER,
            "group.id": CONSUMER_GROUP,
            "auto.offset.reset": "earliest",
        })
        consumer.subscribe([TOPIC_IN])
        
        producer = Producer({"bootstrap.servers": KAFKA_BROKER})
        
        try:
            while self._running:
                msg = consumer.poll(1.0)
                if msg is None:
                    continue
                if msg.error():
                    logger.error(f"Kafka error: {msg.error()}")
                    continue
                
                try:
                    payload_str = msg.value().decode("utf-8")
                    payload = json.loads(payload_str)
                    
                    txn_id = payload.get("id")
                    desc = payload.get("rawDescription", "")
                    
                    logger.info(f"Categorizing transaction {txn_id}: {desc}")
                    
                    category = guess_category(desc)
                    merchant = clean_merchant(desc)
                    
                    out_payload = {
                        "transactionId": txn_id,
                        "categoryName": category,
                        "merchant": merchant,
                        "confidenceScore": 0.85
                    }
                    
                    producer.produce(
                        TOPIC_OUT, 
                        key=str(txn_id).encode('utf-8') if txn_id else b'',
                        value=json.dumps(out_payload).encode('utf-8')
                    )
                    producer.poll(0)
                    
                except Exception as e:
                    logger.error(f"Failed processing message: {e}", exc_info=True)
                    
        finally:
            consumer.close()
            producer.flush()
            logger.info("Kafka consumer thread shutdown complete.")

_worker = None

def start_background_kafka_task():
    global _worker
    if _worker is None:
        _worker = KafkaWorker()
        _worker.start()
    return _worker

def stop_background_kafka_task():
    global _worker
    if _worker:
        _worker.stop()
        _worker.join(timeout=5)
