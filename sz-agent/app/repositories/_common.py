import uuid


def coerce_uuid(value: uuid.UUID | str) -> uuid.UUID | None:
    """Convert a UUID or string into a UUID object, returning None if malformed."""
    if isinstance(value, uuid.UUID):
        return value
    try:
        return uuid.UUID(str(value))
    except (ValueError, TypeError):
        return None
