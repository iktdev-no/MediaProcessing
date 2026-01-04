from enum import Enum

class TaskStatus(Enum):
    PENDING = "Pending"
    IN_PROGRESS = "InProgress"
    COMPLETED = "Completed"
    FAILED = "Failed"

class MediaType(Enum):
    MOVIE = "Movie"
    SERIE = "Serie"
