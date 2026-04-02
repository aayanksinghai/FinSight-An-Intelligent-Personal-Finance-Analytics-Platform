from pydantic_settings import BaseSettings

class Settings(BaseSettings):
    PROJECT_NAME: str = "Anomaly Detection Service"
    API_V1_STR: str = "/api/v1"
    
    # Kafka Configuration
    KAFKA_BROKERS: str = "localhost:9092"
    KAFKA_TOPIC_INGESTED: str = "transactions.ingested"
    KAFKA_TOPIC_ALERTS: str = "anomaly.alerts"
    KAFKA_GROUP_ID: str = "anomaly-detection-group"
    
    # ML Preferences
    ANOMALY_THRESHOLD: float = 0.85
    HISTORICAL_MONTHS_NEEDED: int = 3
    
    class Config:
        env_file = ".env"

settings = Settings()
