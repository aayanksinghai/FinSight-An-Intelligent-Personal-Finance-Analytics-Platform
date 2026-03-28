import logging
from typing import Optional

logger = logging.getLogger(__name__)

# Basic dictionary heuristic for Phase 4 MVP
# Later, this will be swapped for an embedding-based semantic search
# or a zero-shot LLM categorization approach.
CATEGORY_RULES = {
    "Housing": ["rent", "mortgage", "housing", "home", "maintenance", "homedepot", "ikea"],
    "Transportation": ["uber", "lyft", "gas", "shell", "chevron", "transport", "transit", "train", "metro"],
    "Food & Dining": ["zomato", "swiggy", "starbucks", "mcdonalds", "restaurant", "cafe", "coffee", "bistro", "pizza"],
    "Shopping": ["amazon", "flipkart", "myntra", "walmart", "target", "nike", "clothing"],
    "Utilities": ["electric", "water", "internet", "comcast", "verizon", "att", "phone", "mobile", "utility"],
    "Health & Fitness": ["gym", "fitness", "pharmacy", "cvs", "walgreens", "doctor", "hospital"],
    "Entertainment": ["netflix", "spotify", "movie", "ticketmaster", "cinema", "hulu", "prime"],
    "Travel": ["airbnb", "hotel", "flight", "airline", "delta", "booking", "expedia"],
}

def guess_category(description: str) -> str:
    """
    Very basic heuristic approach to map a raw description to a known category.
    Returns 'Uncategorized' if no match is found.
    """
    if not description:
        return "Uncategorized"
        
    desc_lower = description.lower()
    
    # 1. Rule-based search
    for category, keywords in CATEGORY_RULES.items():
        if any(kw in desc_lower for kw in keywords):
            return category
            
    # 2. Simple fallback: if 'restaurant' in description, Food & Dining, etc.
    return "Uncategorized"

def clean_merchant(description: str) -> Optional[str]:
    """
    Attempts to extract a clean merchant name from a raw bank description.
    For MVP, we just take the first two words if it's long, or return as is.
    """
    if not description:
        return None
        
    parts = description.split()
    if len(parts) > 3:
        return f"{parts[0]} {parts[1]}"
    
    return description
