import pandas as pd
import math
from datetime import datetime
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List, Dict, Any

def get_forecast(db: Session, email: str, month_year: str) -> List[Dict[str, Any]]:
    """
    Predicts next month's (or current month's) total spend per category based on historical transaction patterns.
    Updates the forecast at mid-month using actual spend to date plus projected remainder.
    """
    try:
        target_dt = datetime.strptime(month_year, "%Y-%m")
    except ValueError:
        return []

    # Get the current date to determine if we are forecasting the future, the past, or currently mid-month
    now = datetime.now()
    is_current_month = (target_dt.year == now.year and target_dt.month == now.month)
    
    # We need historical data. We pull up to 12 months before the target_dt.
    # Exclude the exact target month from history training to avoid data leakage if mid-month.
    query = text("""
        SELECT amount, transaction_date at time zone 'UTC' as tx_date, category 
        FROM transaction_schema.transactions 
        WHERE owner_email = :email 
        AND type = 'DEBIT' 
        AND transaction_date < :target_month_end
    """)
    # target_month_end is the 1st of the target_dt month to ensure we get history strictly before it
    target_month_start = f"{target_dt.year}-{target_dt.month:02d}-01"
    
    df = pd.read_sql(query, db.bind, params={"email": email, "target_month_end": target_month_start})
    
    if df.empty:
        return []

    df['tx_date'] = pd.to_datetime(df['tx_date'])
    df['month'] = df['tx_date'].dt.to_period('M')

    # Group by category and month
    monthly_cat = df.groupby(['category', 'month'])['amount'].sum().reset_index()

    forecasts = []
    
    # Calculate days for mid-month projection
    days_in_month = pd.Period(f"{target_dt.year}-{target_dt.month:02d}").days_in_month
    days_passed = now.day if is_current_month else (days_in_month if now > target_dt else 0)
    days_remaining = max(0, days_in_month - days_passed)
    
    # If mid-month, we need actuals so far this month
    actuals_so_far = {}
    if is_current_month:
        mid_query = text("""
            SELECT category, SUM(amount) as current_spend
            FROM transaction_schema.transactions 
            WHERE owner_email = :email 
            AND type = 'DEBIT' 
            AND transaction_date >= :month_start
            GROUP BY category
        """)
        m_df = pd.read_sql(mid_query, db.bind, params={"email": email, "month_start": target_month_start})
        for _, row in m_df.iterrows():
            actuals_so_far[row['category']] = float(row['current_spend'])

    unique_categories = monthly_cat['category'].unique()

    for cat in unique_categories:
        cat_data = monthly_cat[monthly_cat['category'] == cat].sort_values('month')
        
        # Exponential Moving Average (EMA) for base prediction
        # We use a simple EMA over the historical months
        if len(cat_data) == 0:
            continue
            
        amounts = cat_data['amount'].values
        if len(amounts) == 1:
            base_pred = amounts[0]
            std_dev = base_pred * 0.1 # Fallback std dev
        else:
            # EMA with alpha = 0.5 as a simple responsive forecaster
            s = pd.Series(amounts)
            base_pred = s.ewm(alpha=0.5, adjust=False).mean().iloc[-1]
            std_dev = s.std()
            if math.isnan(std_dev):
                std_dev = base_pred * 0.1

        # Mid-month adjustment
        actual_spend = actuals_so_far.get(cat, 0.0)
        
        if is_current_month:
            # Service must update the forecast at mid-month using actual spend to date plus projected remainder
            projected_remainder = (base_pred / days_in_month) * days_remaining if base_pred > 0 else 0
            final_pred = actual_spend + projected_remainder
            # Shrink confidence interval as the month progresses
            confidence_factor = days_remaining / days_in_month
            lower_bound = max(actual_spend, final_pred - (1.96 * std_dev * confidence_factor))
            upper_bound = final_pred + (1.96 * std_dev * confidence_factor)
            
            # If actual already exceeded upper bound
            if actual_spend > upper_bound:
                upper_bound = actual_spend * 1.05
        else:
            final_pred = base_pred
            lower_bound = max(0, final_pred - (1.96 * std_dev))
            upper_bound = final_pred + (1.96 * std_dev)

        forecasts.append({
            "category": cat,
            "predictedAmount": round(final_pred, 2),
            "lowerBound": round(lower_bound, 2),
            "upperBound": round(upper_bound, 2),
            "actualAmountToDate": round(actual_spend, 2) if is_current_month else None
        })

    # Sort so highest predictions are first
    forecasts.sort(key=lambda x: x["predictedAmount"], reverse=True)
    return forecasts

def calculate_accuracy(db: Session) -> Dict[str, Any]:
    """
    Evaluates global Model Accuracy over time across all users.
    Returns Mean Absolute Percentage Error (MAPE) or similar aggregate metric.
    """
    query = text("""
        SELECT predicted_amount, actual_amount 
        FROM forecasting_schema.forecast_accuracy_snapshots
        WHERE actual_amount > 0 AND predicted_amount > 0
    """)
    df = pd.read_sql(query, db.bind)
    
    if df.empty:
        return {"mape": 0.0, "totalSnapshots": 0, "accuracyPercentage": 100.0}
    
    # MAPE calculation
    df['error_pct'] = abs(df['predicted_amount'] - df['actual_amount']) / df['actual_amount']
    mape = df['error_pct'].mean() * 100
    
    accuracy = max(0.0, 100.0 - mape)
    return {
        "mape": round(mape, 2),
        "totalSnapshots": len(df),
        "accuracyPercentage": round(accuracy, 2)
    }
