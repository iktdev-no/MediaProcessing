from datetime import datetime, timezone

def utc_now():
    """Returnerer nåværende tid i UTC som en timezone-aware datetime."""
    return datetime.now(timezone.utc)
