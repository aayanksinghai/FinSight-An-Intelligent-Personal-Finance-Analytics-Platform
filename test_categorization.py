import json
import uuid
import time
from confluent_kafka import Producer, Consumer

KAFKA_BROKER = "localhost:9092"
print("Setting up Kafka clients...")

# Publish to created
producer = Producer({"bootstrap.servers": KAFKA_BROKER})
txn_id = str(uuid.uuid4())
payload = {
    "id": txn_id,
    "ownerEmail": "test@test.com",
    "rawDescription": "UPI/Swiggy/Food Order/upi",
    "merchant": "Swiggy",
    "amount": 420.0,
    "type": "DEBIT",
    "occurredAt": "2026-03-29T10:00:00Z"
}
producer.produce("transactions.created", key=txn_id.encode('utf-8'), value=json.dumps(payload).encode('utf-8'))
producer.flush()
print(f"Published test transaction {txn_id} to transactions.created")

# Wait a moment for ML service to process
time.sleep(2)

# Consume from categorized
consumer = Consumer({
    "bootstrap.servers": KAFKA_BROKER,
    "group.id": "test-group-" + str(uuid.uuid4()),
    "auto.offset.reset": "earliest"
})
consumer.subscribe(["transactions.categorized"])

print("Waiting for response on transactions.categorized...")
start_time = time.time()
found = False
while time.time() - start_time < 10:
    msg = consumer.poll(1.0)
    if msg is None: continue
    if msg.error(): continue
    
    val = json.loads(msg.value().decode("utf-8"))
    if val.get("transactionId") == txn_id:
        print("SUCCESS! Received categorized event:")
        print(json.dumps(val, indent=2))
        found = True
        break
        
if not found:
    print("FAILED: No response received on transactions.categorized within 10 seconds.")
    
consumer.close()
