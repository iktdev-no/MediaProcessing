from datetime import datetime, timezone

def utc_now():
    """
    Returnerer en UTC-basert LocalDateTime uten Z eller offset,
    med nanosekund-lignende presisjon (mikrosekunder + padding).
    """
    dt = datetime.now(timezone.utc).replace(tzinfo=None)
    return dt.strftime("%Y-%m-%dT%H:%M:%S.") + f"{dt.microsecond:06d}000"

