import threading
from ctypes import Union
from config.database_config import DatabaseConfig
from utils.logger import logger
import mysql.connector
from mysql.connector import Error
from utils.backoff import wait_with_backoff

class Database:
    def __init__(self, config: DatabaseConfig):
        self.config = config
        self._local = threading.local()

    def _create_connection(self):
        """Oppretter en ny tilkobling med backoff."""
        self.config.validate()
        while True:
            try:
                conn = mysql.connector.connect(
                    host=self.config.address,
                    user=self.config.username,
                    password=self.config.password,
                    database=self.config.name,
                    autocommit=True
                )
                if conn.is_connected(): 
                    logger.info(f"✅ Tilkoblet til databasen (Tråd: {threading.get_ident()})")
                    return conn
            except Error as e:
                logger.error(f"❌ DB-tilkobling feilet for tråd {threading.get_ident()}: {e}")
                for _ in wait_with_backoff():
                    try:
                        conn = mysql.connector.connect(
                            host=self.config.address,
                            user=self.config.username,
                            password=self.config.password,
                            database=self.config.name,
                            autocommit=True
                        )
                        if conn.is_connected():
                            logger.info(f"✅ Tilkoblet til databasen (Tråd: {threading.get_ident()})")
                            return conn
                    except Error:
                        continue

    def connect(self):
        """Koble til DB for gjeldende tråd."""
        self._local.conn = self._create_connection()

    def validate(self):
        """Sjekk at tilkoblingen for gjeldende tråd er aktiv."""
        if not hasattr(self._local, "conn") or not self._local.conn or not self._local.conn.is_connected():
            logger.warning(f"⚠️ Tilkobling mistet for tråd {threading.get_ident()}, prøver igjen...")
            self.connect()

    @property
    def conn(self):
        """Gir trådsikker tilgang til gjeldende tråds tilkobling."""
        self.validate()
        return self._local.conn

    def query(self, sql: str, params=None):
        """Kjør en spørring med validering."""
        cursor = self.conn.cursor(dictionary=True)
        cursor.execute(sql, params or ())
        return cursor.fetchall()

    def ping(self):
        try:
            cursor = self.conn.cursor()
            cursor.execute("SELECT 1")
            cursor.fetchone()
            return True
        except Exception as e:
            logger.error(f"Ping failed: {e}")
            return False