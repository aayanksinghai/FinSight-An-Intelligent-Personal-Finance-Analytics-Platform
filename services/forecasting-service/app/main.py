from fastapi import FastAPI, Depends, HTTPException, Header
import jwt
from typing import Optional
from sqlalchemy import text
import os

from app.db.database import get_db, engine
from app.model.forecaster import get_forecast, calculate_accuracy

app = FastAPI(title="FinSight Forecasting Service")

# Create schemas if they don't exist
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
            
        payload = jwt.decode(token, pub_key, algorithms=["RS256"])
        return payload
    except jwt.ExpiredSignatureError:
        raise HTTPException(status_code=401, detail="Token has expired")
    except jwt.InvalidTokenError as e:
        raise HTTPException(status_code=401, detail=f"Invalid token: {str(e)}")

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

@app.get("/actuator/health")
def health():
    return {"status": "UP"}
