import uuid


def coerce_uuid(value: uuid.UUID | str) -> uuid.UUID | None:
    """将 UUID 或字符串转换为 UUID 对象，格式非法时返回 None。"""
    if isinstance(value, uuid.UUID):
        return value
    try:
        return uuid.UUID(str(value))
    except (ValueError, TypeError):
        return None
