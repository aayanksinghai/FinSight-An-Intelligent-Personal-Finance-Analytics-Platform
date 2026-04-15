import pandas as pd
import math
from datetime import datetime, date
from sqlalchemy.orm import Session
from sqlalchemy import text
from typing import List, Dict, Any, Optional


# ── helpers ───────────────────────────────────────────────────────────────────

def _month_bounds(month_year: str):
    """Return (start, end) as 'YYYY-MM-DD' strings for a given 'YYYY-MM' string."""
    dt = datetime.strptime(month_year, "%Y-%m")
    start = dt.replace(day=1).date()
    # First day of next month
    if dt.month == 12:
        end = date(dt.year + 1, 1, 1)
    else:
        end = date(dt.year, dt.month + 1, 1)
    return str(start), str(end)


def _income_spend(db: Session, email: str, start: str, end: str):
    """Returns (income, spend) for a given period."""
    q = text("""
        SELECT type, COALESCE(SUM(amount), 0) as total
        FROM transaction_schema.transactions
        WHERE owner_email = :email
          AND occurred_at >= :start
          AND occurred_at < :end
        GROUP BY type
    """)
    rows = db.execute(q, {"email": email, "start": start, "end": end}).fetchall()
    income, spend = 0.0, 0.0
    for row in rows:
        if row[0] == "CREDIT":
            income = float(row[1])
        elif row[0] == "DEBIT":
            spend = float(row[1])
    return income, spend


def _category_spend(db: Session, email: str, start: str, end: str) -> Dict[str, float]:
    """Returns {category: total_spend} for the period."""
    q = text("""
        SELECT c.name as category, COALESCE(SUM(t.amount), 0) as total
        FROM transaction_schema.transactions t
        JOIN transaction_schema.categories c ON t.category_id = c.id
        WHERE t.owner_email = :email
          AND t.type = 'DEBIT'
          AND t.occurred_at >= :start
          AND t.occurred_at < :end
        GROUP BY c.name
    """)
    rows = db.execute(q, {"email": email, "start": start, "end": end}).fetchall()
    return {row[0]: float(row[1]) for row in rows}


DISCRETIONARY_CATEGORIES = {"Food & Dining", "Entertainment", "Shopping", "Food", "Dining", "Restaurants"}


def _detect_recurring(db: Session, email: str, start: str, end: str) -> float:
    """
    Estimate recurring/EMI burden as a fraction of total spend.
    Recurring = same merchant appearing in 3+ consecutive months with similar amounts.
    Returns estimated recurring spend for this period.
    """
    q = text("""
        SELECT normalized_merchant, COUNT(*) as cnt,
               MIN(occurred_at) as first_seen,
               MAX(occurred_at) as last_seen,
               AVG(amount) as avg_amount,
               STDDEV(amount) as std_amount
        FROM transaction_schema.transactions
        WHERE owner_email = :email
          AND type = 'DEBIT'
          AND occurred_at < :end
        GROUP BY normalized_merchant
        HAVING COUNT(DISTINCT DATE_TRUNC('month', occurred_at)) >= 2
           AND STDDEV(amount) / NULLIF(AVG(amount), 0) < 0.25
    """)
    rows = db.execute(q, {"email": email, "end": end}).fetchall()
    return sum(float(r[4]) for r in rows)  # sum of avg_amount per recurring merchant


def _budget_adherence(db: Session, email: str, month_year: str) -> float:
    """
    Returns % of categories within budget for the given month (0.0–1.0).
    Falls back to 1.0 (perfect adherence) if no budgets are configured.
    """
    try:
        q = text("""
            SELECT b.limit_amount, COALESCE(SUM(t.amount), 0) as spent
            FROM budget_schema.budgets b
            LEFT JOIN transaction_schema.transactions t
              ON t.owner_email = b.user_email
             AND t.category_id = b.category_id
             AND DATE_TRUNC('month', t.occurred_at) = DATE_TRUNC('month', TO_DATE(:month_year, 'YYYY-MM'))
             AND t.type = 'DEBIT'
            WHERE b.user_email = :email
              AND b.month_year = :month_year
            GROUP BY b.limit_amount
        """)
        rows = db.execute(q, {"email": email, "month_year": month_year}).fetchall()
        if not rows:
            return 1.0
        within = sum(1 for r in rows if float(r[1]) <= float(r[0]))
        return within / len(rows)
    except Exception:
        db.rollback()
        return 1.0  # Don't fail the whole score if budget schema isn't available


# ── main scoring function ──────────────────────────────────────────────────────

def compute_stress_score(db: Session, email: str, month_year: str, include_trend: bool = True) -> Dict[str, Any]:
    """
    Computes a financial stress score (0–100, where 100 = maximum stress).
    Returns composite score, component breakdown, 6-month trend, and explanation.
    """
    start, end = _month_bounds(month_year)
    income, spend = _income_spend(db, email, start, end)

    # ── Component 1: Spend-to-income ratio (40% weight, higher = more stress) ──
    if income > 0:
        spend_ratio = min(spend / income, 2.0)  # cap at 200%
    else:
        spend_ratio = 1.0  # no income recorded = high stress proxy
    spend_score = min(spend_ratio * 50, 100)  # 0–100

    # ── Component 2: Savings rate (20% weight, higher savings = lower stress) ──
    if income > 0:
        savings_rate = max(0.0, 1.0 - (spend / income))
    else:
        savings_rate = 0.0
    # Low savings = high stress: inverse
    savings_score = max(0, 100 - (savings_rate * 100))

    # ── Component 3: EMI/recurring burden (15% weight) ────────────────────────
    recurring_spend = _detect_recurring(db, email, start, end)
    if spend > 0:
        recurring_ratio = min(recurring_spend / spend, 1.0)
    else:
        recurring_ratio = 0.0
    recurring_score = recurring_ratio * 100

    # ── Component 4: Discretionary spend growth (15% weight) ──────────────────
    dt = datetime.strptime(month_year, "%Y-%m")
    if dt.month == 1:
        prev_month = f"{dt.year - 1}-12"
    else:
        prev_month = f"{dt.year}-{dt.month - 1:02d}"

    prev_start, prev_end = _month_bounds(prev_month)
    curr_disc = sum(v for k, v in _category_spend(db, email, start, end).items()
                    if any(d.lower() in k.lower() for d in ["food", "dining", "entertainment", "shopping"]))
    prev_disc = sum(v for k, v in _category_spend(db, email, prev_start, prev_end).items()
                    if any(d.lower() in k.lower() for d in ["food", "dining", "entertainment", "shopping"]))

    if prev_disc > 0:
        disc_growth = (curr_disc - prev_disc) / prev_disc  # e.g. +0.20 = 20% growth
    else:
        disc_growth = 0.0
    disc_score = max(0, min(100, 50 + disc_growth * 100))  # neutral at 50

    # ── Component 5: Budget adherence rate (10% weight) ───────────────────────
    adherence = _budget_adherence(db, email, month_year)
    adherence_score = (1.0 - adherence) * 100  # 0% adherence = 100 stress

    # Fetch dynamic weights from admin_config table
    # Default fallback values
    weights = {
        'stress.weight.spend': 0.40,
        'stress.weight.savings': 0.20,
        'stress.weight.recurring': 0.15,
        'stress.weight.discretionary': 0.15,
        'stress.weight.adherence': 0.10
    }
    try:
        q_weights = text("SELECT config_key, config_value FROM admin_config WHERE config_key LIKE 'stress.weight.%'")
        rows = db.execute(q_weights).fetchall()
        for r in rows:
            weights[r[0]] = float(r[1])
    except Exception:
        db.rollback()
        pass

    # Normalize weights just in case they don't sum up exactly to 1.0 due to admin inputs
    total_w = sum(weights.values())
    if total_w == 0: total_w = 1.0

    # ── Composite score ────────────────────────────────────────────────────────
    composite = (
        spend_score     * (weights['stress.weight.spend'] / total_w) +
        savings_score   * (weights['stress.weight.savings'] / total_w) +
        recurring_score * (weights['stress.weight.recurring'] / total_w) +
        disc_score      * (weights['stress.weight.discretionary'] / total_w) +
        adherence_score * (weights['stress.weight.adherence'] / total_w)
    )
    composite = round(min(max(composite, 0), 100), 1)

    # ── 6-month trend ──────────────────────────────────────────────────────────
    trend = []
    if include_trend:
        for i in range(5, -1, -1):
            yr, mo = dt.year, dt.month - i
            while mo <= 0:
                mo += 12
                yr -= 1
            m_str = f"{yr}-{mo:02d}"
            try:
                hist = compute_stress_score(db, email, m_str, include_trend=False)
                trend.append({"month": m_str, "score": hist["score"]})
            except Exception:
                db.rollback()
                trend.append({"month": m_str, "score": None})

    # ── Plain-language explanation ─────────────────────────────────────────────
    explanation = _build_explanation(
        composite, spend_ratio, savings_rate, recurring_ratio, disc_growth, adherence,
        income, spend, recurring_spend, curr_disc, prev_disc
    )

    return {
        "month": month_year,
        "score": composite,
        "label": _score_label(composite),
        "explanation": explanation,
        "trend": trend,
        "components": {
            "spendToIncomeRatio": round(spend_ratio, 3),
            "savingsRate": round(savings_rate, 3),
            "recurringBurden": round(recurring_ratio, 3),
            "discretionaryGrowth": round(disc_growth, 3),
            "budgetAdherence": round(adherence, 3)
        }
    }


def _score_label(score: float) -> str:
    if score < 30:
        return "Healthy"
    elif score < 55:
        return "Moderate"
    elif score < 75:
        return "Elevated"
    else:
        return "High"


def _build_explanation(score, spend_ratio, savings_rate, recurring_ratio,
                       disc_growth, adherence, income, spend, recurring_spend,
                       curr_disc, prev_disc) -> str:
    parts = []
    label = _score_label(score)

    if label == "Healthy":
        parts.append("Your finances are in great shape this month! 🎉")
    elif label == "Moderate":
        parts.append("Your finances look mostly stable, with a few things to keep an eye on.")
    elif label == "Elevated":
        parts.append("There are some financial pressures this month worth addressing.")
    else:
        parts.append("Your financial stress is high this month — let's break down why.")

    # Spend-to-income
    if spend_ratio > 0.9:
        parts.append(f"You spent {spend_ratio * 100:.0f}% of your income, which leaves little room for savings.")
    elif spend_ratio > 0.7:
        parts.append(f"You spent {spend_ratio * 100:.0f}% of your income — reasonable but could be tighter.")
    else:
        parts.append(f"You spent only {spend_ratio * 100:.0f}% of your income — well managed! ✅")

    # Savings
    if savings_rate < 0.05:
        parts.append("Your savings this month are very low, which increases vulnerability to unexpected expenses.")
    elif savings_rate > 0.2:
        parts.append(f"You saved {savings_rate * 100:.0f}% of your income — excellent! 💪")

    # Recurring
    if recurring_ratio > 0.5:
        parts.append(f"About {recurring_ratio * 100:.0f}% of your spending is locked in recurring or EMI payments, limiting flexibility.")
    elif recurring_ratio > 0.3:
        parts.append(f"Recurring payments make up {recurring_ratio * 100:.0f}% of your spending.")

    # Discretionary
    if disc_growth > 0.15:
        parts.append(f"Discretionary spending (food, entertainment, shopping) grew {disc_growth * 100:.0f}% vs last month — consider scaling back.")
    elif disc_growth < -0.1:
        parts.append(f"You cut discretionary spending by {abs(disc_growth) * 100:.0f}% vs last month — great discipline! 👍")

    # Budget adherence
    if adherence < 0.5:
        parts.append("More than half your budget categories went over limit this month.")
    elif adherence == 1.0:
        parts.append("You stayed within budget across all categories — perfect adherence! 🏆")

    return " ".join(parts)


# ── simulation ─────────────────────────────────────────────────────────────────

def simulate_stress(db: Session, email: str, month_year: str,
                    adjustments: List[Dict[str, Any]]) -> Dict[str, Any]:
    """
    Applies hypothetical adjustments and returns projected stress score and balance.
    Does NOT write to database.
    """
    start, end = _month_bounds(month_year)
    income, spend = _income_spend(db, email, start, end)
    cats = _category_spend(db, email, start, end)

    # Apply adjustments
    delta_spend = 0.0
    for adj in adjustments:
        adj_type = adj.get("type")
        if adj_type == "reduce_category":
            cat = adj.get("category", "")
            pct = float(adj.get("pct", 0)) / 100.0
            cat_spend = cats.get(cat, 0.0)
            delta_spend -= cat_spend * pct

        elif adj_type == "add_recurring":
            amount = float(adj.get("amount", 0))
            delta_spend += amount

        elif adj_type == "remove_recurring":
            amount = float(adj.get("amount", 0))
            delta_spend -= amount

    projected_spend = max(0.0, spend + delta_spend)
    projected_balance = income - projected_spend

    # Recompute key metrics
    if income > 0:
        proj_spend_ratio = min(projected_spend / income, 2.0)
        proj_savings_rate = max(0.0, 1.0 - (projected_spend / income))
    else:
        proj_spend_ratio = 1.0
        proj_savings_rate = 0.0

    # Use same recurring and discretionary, budget adherence (approximated)
    recurring_spend = _detect_recurring(db, email, start, end)
    if projected_spend > 0:
        proj_recurring_ratio = min(recurring_spend / projected_spend, 1.0)
    else:
        proj_recurring_ratio = 0.0

    adherence = _budget_adherence(db, email, month_year)
    disc_score = 50.0  # neutral — no change to disc growth from sliders in this sim

    proj_spend_score = min(proj_spend_ratio * 50, 100)
    proj_savings_score = max(0, 100 - (proj_savings_rate * 100))
    proj_recurring_score = proj_recurring_ratio * 100
    proj_adherence_score = (1.0 - adherence) * 100

    proj_composite = (
        proj_spend_score    * 0.40 +
        proj_savings_score  * 0.20 +
        proj_recurring_score * 0.15 +
        disc_score          * 0.15 +
        proj_adherence_score * 0.10
    )
    proj_composite = round(min(max(proj_composite, 0), 100), 1)

    # Current base score
    base = compute_stress_score(db, email, month_year)

    return {
        "baseScore": base["score"],
        "projectedScore": proj_composite,
        "scoreDelta": round(proj_composite - base["score"], 1),
        "baseBalance": round(income - spend, 2),
        "projectedBalance": round(projected_balance, 2),
        "balanceDelta": round(projected_balance - (income - spend), 2),
        "projectedSpend": round(projected_spend, 2),
        "adjustmentSummary": f"Applied {len(adjustments)} adjustment(s): projected spend changes by ₹{abs(delta_spend):,.0f}."
    }

# ── admin ──────────────────────────────────────────────────────────────────────

def get_stress_score_distribution(db: Session) -> Dict[str, Any]:
    """
    Computes the current month's stress score for all users and returns the distribution.
    """
    current_month = datetime.now().strftime("%Y-%m")
    
    q = text("SELECT DISTINCT owner_email FROM transaction_schema.transactions")
    users = [row[0] for row in db.execute(q).fetchall()]
    
    distribution = {"Healthy": 0, "Moderate": 0, "Elevated": 0, "High": 0}
    total_score = 0.0
    valid_users = 0
    
    for email in users:
        try:
            # We don't need historical trend for global distribution
            res = compute_stress_score(db, str(email), current_month, include_trend=False)
            val = res["score"]
            label = res["label"]
            if label in distribution:
                distribution[label] += 1
            total_score += val
            valid_users += 1
        except Exception:
            db.rollback()
            continue
            
    avg_score = round(total_score / valid_users, 1) if valid_users > 0 else 0.0
    
    return {
        "averageScore": avg_score,
        "totalUsersScored": valid_users,
        "distribution": [
            {"name": "Healthy", "value": distribution["Healthy"]},
            {"name": "Moderate", "value": distribution["Moderate"]},
            {"name": "Elevated", "value": distribution["Elevated"]},
            {"name": "High", "value": distribution["High"]}
        ]
    }
