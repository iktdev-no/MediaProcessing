from datetime import datetime, timezone

def utc_now():
    """Returnerer nåværende tid i UTC som en timezone-aware datetime."""
    return datetime.now(timezone.utc)

def parse_mysql_ts(value):
    if value is None:
        return None
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S.%f").replace(tzinfo=timezone.utc)

