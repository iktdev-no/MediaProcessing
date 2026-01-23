from datetime import datetime, timezone

def utc_now():
    """
    Matcher nøyaktig formatet Kotlin/Exposed skriver til databasen:
    yyyy-MM-dd HH:mm:ss.SSSSSS (UTC)
    """
    dt = datetime.now(timezone.utc)
    return dt.strftime("%Y-%m-%d %H:%M:%S.%f")
