# db/metadata_repository.py

from typing import Optional, List
from db.database import Database
from models.metadata import Metadata, Summary
from models.enums import MediaType


def fetch_metadata_by_source_and_id(db: Database, source: str, source_id: str) -> Optional[Metadata]:
    """
    Henter metadata for en gitt (source, sourceId).
    Returnerer None hvis ikke funnet eller hvis tabellen ikke finnes.
    """
    try:
        db.validate()
        cursor = db.conn.cursor(dictionary=True)

        cursor.execute(
            """
            SELECT *
            FROM METADATA
            WHERE SOURCE = %s AND SOURCE_ID = %s
            """,
            (source, source_id)
        )

        row = cursor.fetchone()
        if not row:
            return None

        metadata_id = row["ID"]

        # Hent alternate titles
        cursor.execute(
            "SELECT TITLE FROM METADATA_TITLES WHERE METADATA_ID = %s",
            (metadata_id,)
        )
        alt_titles = [r["TITLE"] for r in cursor.fetchall()]

        # Hent summaries
        cursor.execute(
            "SELECT LANGUAGE, DESCRIPTION FROM METADATA_SUMMARIES WHERE METADATA_ID = %s",
            (metadata_id,)
        )
        summaries = [
            Summary(language=r["LANGUAGE"], summary=r["DESCRIPTION"])
            for r in cursor.fetchall()
        ]

        # Hent genres
        cursor.execute(
            "SELECT GENRE FROM METADATA_GENRES WHERE METADATA_ID = %s",
            (metadata_id,)
        )
        genres = [r["GENRE"] for r in cursor.fetchall()]

        return Metadata(
            source=row["SOURCE"],
            sourceId=row["SOURCE_ID"],
            title=row["TITLE"],
            altTitle=alt_titles,
            cover=row["COVER"],
            bannerImage=row["BANNER_IMAGE"],
            type=MediaType(row["MEDIA_TYPE"]),
            summary=summaries,
            genres=genres
        )

    except Exception as e:
        # Hvis tabellen ikke finnes, eller kolonner mangler → returner None
        if "doesn't exist" in str(e).lower() or "unknown column" in str(e).lower():
            return None

        # Andre feil → logg og returner None
        print(f"[metadata_repository] Warning: {e}")
        return None
