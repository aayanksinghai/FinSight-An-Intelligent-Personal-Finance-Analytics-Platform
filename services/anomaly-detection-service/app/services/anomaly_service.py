import uuid
from typing import Dict, Any
from app.models.lstm_autoencoder import LSTMAutoencoder
from app.core.config import settings

class AnomalyService:
    def __init__(self):
        # In a real scenario, models are loaded per-user from a registry like MLflow or S3
        self.models = {}

    def score_transaction(self, transaction: Dict[str, Any]) -> Dict[str, Any]:
        """
        Scores a single transaction for anomalies.
        Returns a dictionary with result and explanation if anomalous.
        """
        user_email = transaction.get("ownerEmail", "unknown")
        
        # STUB: Simulate anomaly scoring.
        # In a real implementation:
        # 1. Fetch user's baseline model (if exists)
        # 2. Extract features (amount, time, merchant)
        # 3. Pass through LSTMAutoencoder
        # 4. Compare reconstruction error vs baseline threshold
        
        amount = float(transaction.get("debitAmount", 0.0))
        is_anomalous = False
        explanation = ""
        score = 0.0
        
        # Mock threshold based on business rules for testing
        if amount > 5000.0:
            is_anomalous = True
            score = 0.92
            explanation = f"Unusual amount relative to your baseline (₹{amount})"
        elif str(transaction.get("merchant", "")).lower() == "unknown":
            is_anomalous = True
            score = 0.88
            explanation = "Unusual merchant not seen in your transaction history"

        return {
            "is_anomalous": is_anomalous,
            "anomaly_score": score,
            "explanation": explanation
        }

    def process_feedback(self, transaction_id: str, user_id: str, label: str):
        """
        Processes 'expected' or 'suspicious' labels from user.
        """
        # Could save to DB or forward to ML-training-service Kafka topic
        pass

anomaly_service = AnomalyService()
