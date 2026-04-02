from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
from app.services.anomaly_service import anomaly_service

router = APIRouter()

class FeedbackRequest(BaseModel):
    transaction_id: str
    user_email: str
    label: str  # 'expected' or 'suspicious'

@router.get("/health")
def health_check():
    return {"status": "ok", "service": "anomaly-detection-service"}

@router.post("/feedback")
def submit_feedback(request: FeedbackRequest):
    if request.label not in ["expected", "suspicious"]:
        raise HTTPException(status_code=400, detail="Invalid label. Must be 'expected' or 'suspicious'")
    
    anomaly_service.process_feedback(
        transaction_id=request.transaction_id,
        user_id=request.user_email,
        label=request.label
    )
    return {"message": "Feedback received successfully"}
