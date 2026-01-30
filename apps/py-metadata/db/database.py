from ctypes import Union
from config.database_config import DatabaseConfig
from utils.logger import logger
import mysql.connector
from mysql.connector import Error
from utils.backoff import wait_with_backoff

class Database:
    def __init__(self, config: DatabaseConfig):
        self.config = config
        self.conn = None

    def connect(self):
        """Koble til DB med backoff."""
        self.config.validate()
        while True:
            try:
                self.conn = mysql.connector.connect(
                    host=self.config.address,
                    user=self.config.username,
                    password=self.config.password,
                    database=self.config.name,
                    autocommit=True
                )
                if self.conn.is_connected(): 
                    logger.info("✅ Tilkoblet til databasen")
                    return
            except Error as e:
                logger.error(f"❌ DB-tilkobling feilet: {e}")
                for _ in wait_with_backoff():
                    try:
                        self.conn = mysql.connector.connect(
                            host=self.config.address,
                            user=self.config.username,
                            password=self.config.password,
                            database=self.config.name,
                            autocommit=True
                        )
                        if self.conn.is_connected():
                            logger.info("✅ Tilkoblet til databasen")
                            return
                    except Error:
                        continue

    def validate(self):
        """Sjekk at tilkoblingen er aktiv."""
        if not self.conn or not self.conn.is_connected():
            logger.warning("⚠️ Tilkobling mistet, prøver igjen...")
            self.connect()

    def query(self, sql: str, params=None):
        """Kjør en spørring med validering."""
        self.validate()
        cursor = self.conn.cursor(dictionary=True)
        cursor.execute(sql, params or ())
        return cursor.fetchall()

    def ping(self):
        try:
            self.validate()
            cursor = self.conn.cursor()
            cursor.execute("SELECT 1")
            cursor.fetchone()
            return True
        except Exception as e:
            logger.error(f"Ping failed: {e}")
            return False

