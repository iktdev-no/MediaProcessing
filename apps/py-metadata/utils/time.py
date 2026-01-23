from datetime import datetime, timezone

def utc_now():
    """Returnerer nåværende tid i UTC som en timezone-aware datetime."""
    return datetime.now(timezone.utc)

def parse_mysql_ts(value):
    if value is None:
        return None

    # Hvis DB-driveren allerede har gjort jobben
    if isinstance(value, datetime):
        # Sørg for at den er timezone-aware
        if value.tzinfo is None:
            return value.replace(tzinfo=timezone.utc)
        return value

    # Hvis det er en streng (MySQL-format)
    return datetime.strptime(value, "%Y-%m-%d %H:%M:%S.%f").replace(tzinfo=timezone.utc)
