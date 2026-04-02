import logging
from fastapi import FastAPI
from app.api import endpoints
from app.core.config import settings
from app.messaging.kafka_consumer import anomaly_consumer

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title=settings.PROJECT_NAME)

app.include_router(endpoints.router, prefix=settings.API_V1_STR + "/anomaly")

@app.on_event("startup")
def startup_event():
    logger.info("Starting up Anomaly Detection Service...")
    anomaly_consumer.start()

@app.on_event("shutdown")
def shutdown_event():
    logger.info("Shutting down Anomaly Detection Service...")
    anomaly_consumer.stop()
