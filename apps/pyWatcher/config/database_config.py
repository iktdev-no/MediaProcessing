import os
from dataclasses import dataclass

@dataclass
class DatabaseConfig:
    address: str
    port: int
    name: str
    username: str
    password: str

    @staticmethod
    def from_env() -> "DatabaseConfig":
        return DatabaseConfig(
            address=os.environ.get("DATABASE_ADDRESS") or "192.168.2.250",
            port=int(os.environ.get("DATABASE_PORT") or "3306"),
            name=os.environ.get("DATABASE_NAME_E") or "eventsV3",
            username=os.environ.get("DATABASE_USERNAME") or "root",
            password=os.environ.get("DATABASE_PASSWORD") or "def",
        )

    def validate(self) -> None:
        if not self.address:
            raise ValueError("Database address mangler")
        if not self.name:
            raise ValueError("Database name mangler")
        if not self.username:
            raise ValueError("Database username mangler")
        # du kan legge til flere regler her
