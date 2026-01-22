from datetime import datetime, timezone

def utc_now():
    """
    Returnerer et offset-aware UTC datetime-objekt.
    Brukes for alle timestamps som skal inn i databasen.
    """
    return datetime.now(timezone.utc)


def utc_iso():
    """
    Returnerer en ISO8601-streng i UTC, f.eks. '2025-01-22T12:34:56.789012+00:00'
    Perfekt for JSON-serialisering og DB-felter som lagres som tekst.
    """
    return utc_now().isoformat()
