from fastapi import FastAPI, Depends, HTTPException, Header
import jwt
from typing import Optional
from sqlalchemy import text
import os

from app.db.database import get_db, engine
from app.model.forecaster import get_forecast, calculate_accuracy
from app.model.stress_scorer import compute_stress_score, simulate_stress
from pydantic import BaseModel
from typing import List, Optional, Any

class SimulateAdjustment(BaseModel):
    type: str                  # "reduce_category" | "add_recurring" | "remove_recurring"
    category: Optional[str] = None
    pct: Optional[float] = None
    amount: Optional[float] = None
    label: Optional[str] = None

class SimulateRequest(BaseModel):
    monthYear: str
    adjustments: List[SimulateAdjustment]


import time

app = FastAPI(title="FinSight Forecasting Service")

# Wait for DB to be ready and create schemas
MAX_RETRIES = 10
for i in range(MAX_RETRIES):
    try:
        with engine.connect() as conn:
            conn.execute(text("CREATE SCHEMA IF NOT EXISTS forecasting_schema;"))
            conn.execute(text("""
                CREATE TABLE IF NOT EXISTS forecasting_schema.forecast_accuracy_snapshots (
                    id SERIAL PRIMARY KEY,
                    user_email VARCHAR(255) NOT NULL,
                    category VARCHAR(255) NOT NULL,
                    month_year VARCHAR(7) NOT NULL,
                    predicted_amount DECIMAL(19, 2) NOT NULL,
                    actual_amount DECIMAL(19, 2) NOT NULL,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                );
            """))
            conn.commit()
        break
    except Exception as e:
        if i == MAX_RETRIES - 1:
            raise e
        print(f"Database connection failed, retrying in 3 seconds... ({i+1}/{MAX_RETRIES})")
        time.sleep(3)

# JWT Config
JWT_PUBLIC_KEY = os.getenv("JWT_PUBLIC_KEY", "").replace("\\n", "\n")

def get_current_user(authorization: Optional[str] = Header(None)) -> dict:
    if not authorization or not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="Missing or invalid Authorization header")
    
    token = authorization.split(" ")[1]
    
    if not JWT_PUBLIC_KEY:
        # Fallback for local testing if pub key isn't passed (not recommended for production)
        try:
            payload = jwt.decode(token, options={"verify_signature": False})
            return payload
        except Exception:
            raise HTTPException(status_code=401, detail="Invalid token")

    try:
        # The key requires proper BEGIN/END PUBLIC KEY formatting
        pub_key = JWT_PUBLIC_KEY
        if "BEGIN PUBLIC KEY" not in pub_key:
            pub_key = f"-----BEGIN PUBLIC KEY-----\n{pub_key}\n-----END PUBLIC KEY-----"
            
        print(f"DEBUG: Attempting to decode token with key starting with: {pub_key[:30]}")
        payload = jwt.decode(token, pub_key, algorithms=["RS256"])
        print(f"DEBUG: Token decoded successfully. Payload: {payload}")
        return payload
    except jwt.ExpiredSignatureError:
        print("DEBUG: Token has expired")
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError as e:
        print(f"DEBUG: Invalid token error: {str(e)}")
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")
    except Exception as e:
        print(f"DEBUG: Unexpected error during token validation: {str(e)}")
        raise HTTPException(status_code=401, detail=f"Authentication error: {str(e)}")

@app.get("/api/forecast")
def fetch_forecast(monthYear: str, db=Depends(get_db), user: dict = Depends(get_current_user)):
    """
    Get category spend forecasts for a specific month.
    """
    email = user.get("preferred_username") or user.get("sub")
    if not email:
        raise HTTPException(status_code=401, detail="User email not found in token")
        
    forecasts = get_forecast(db, email, monthYear)
    return forecasts

@app.get("/api/forecast/admin/accuracy")
def fetch_accuracy(db=Depends(get_db)):
    """
    Returns global ML accuracy metrics. Should ideally check admin role, 
    but leaving open for internal gateway routing.
    """
    return calculate_accuracy(db)


@app.get("/api/stress-score")
def get_stress_score(monthYear: str, db=Depends(get_db), user: dict = Depends(get_current_user)):
    """
    Compute the financial stress score (0–100) for the given month.
    Returns composite score, 6-month trend, component breakdown, and AI explanation.
    """
    email = user.get("preferred_username") or user.get("sub")
    if not email:
        raise HTTPException(status_code=401, detail="User email not found in token")

    try:
        result = compute_stress_score(db, email, monthYear)
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Stress score computation failed: {str(e)}")


@app.post("/api/stress-score/simulate")
def simulate_stress_score(request: SimulateRequest, db=Depends(get_db), user: dict = Depends(get_current_user)):
    """
    Simulates the impact of hypothetical financial adjustments on the stress score and balance.
    DOES NOT write to the database — read-only projection.
    """
    email = user.get("preferred_username") or user.get("sub")
    if not email:
        raise HTTPException(status_code=401, detail="User email not found in token")

    try:
        adjustments = [a.dict() for a in request.adjustments]
        result = simulate_stress(db, email, request.monthYear, adjustments)
        return result
    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Simulation failed: {str(e)}")


@app.get("/api/stress-score/admin/distribution")
def fetch_stress_score_distribution(db=Depends(get_db)):
    """
    Returns the average stress score and grouping distribution across all users.
    """
    try:
        from app.model.stress_scorer import get_stress_score_distribution
        return get_stress_score_distribution(db)
    except Exception as e:
        raise HTTPException(status_code=500, detail=f"Failed to fetch distribution: {str(e)}")

@app.post("/api/admin/models/retrain/{modelName}")
def retrain_model(modelName: str, db=Depends(get_db)):
    """
    Triggers a manual model retraining pipeline.
    """
    supported_models = ["forecasting", "stress_score", "anomaly_detection", "categorization"]
    if modelName not in supported_models:
        raise HTTPException(status_code=400, detail=f"Unsupported model: {modelName}")
        
    # In a real system, this would trigger an asynchronous Kafka event or Airflow DAG
    # Here we mock a successful trigger.
    return {
        "status": "success",
        "message": f"Model '{modelName}' retraining pipeline successfully triggered",
        "startedAt": time.strftime("%Y-%m-%dT%H:%M:%SZ", time.gmtime())
    }

@app.get("/actuator/health")
def health():
    return {"status": "UP"}
