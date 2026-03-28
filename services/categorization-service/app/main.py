from fastapi import FastAPI
from contextlib import asynccontextmanager
import logging

from .kafka_client import start_background_kafka_task, stop_background_kafka_task

logger = logging.getLogger(__name__)

@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info("Starting up ML Categorization Service")
    try:
        start_background_kafka_task()
    except Exception as e:
        logger.error(f"Failed to start Kafka consumer task: {e}")
        
    yield
    
    logger.info("Shutting down ML Categorization Service")
    stop_background_kafka_task()

app = FastAPI(
    title="Categorization ML Service",
    description="Python FastAPI service providing async categorization for transactions via Kafka.",
    lifespan=lifespan
)

@app.get("/health")
def health_check():
    return {"status": "UP", "service": "categorization-service"}

# Optional: Add direct HTTP categorization endpoint for testing or synchronous UI calls
from pydantic import BaseModel

class CategorizeRequest(BaseModel):
    description: str

@app.post("/api/categorize")
def categorize_text(req: CategorizeRequest):
    from .ml_model import guess_category, clean_merchant
    return {
        "category": guess_category(req.description),
        "merchant": clean_merchant(req.description)
    }
