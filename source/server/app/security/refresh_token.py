import hashlib
import secrets


REFRESH_TOKEN_BYTES = 32


def generate_refresh_token() -> str:
    """
    Generate a cryptographically secure refresh token.

    The returned value is the only usable token.
    It must never be persisted directly in the database.
    """
    return secrets.token_urlsafe(REFRESH_TOKEN_BYTES)


def hash_refresh_token(token: str) -> str:
    """
    Return a deterministic SHA-256 hash of a refresh token.

    Only this derived value should be persisted.
    """
    return hashlib.sha256(
        token.encode("utf-8")
    ).hexdigest()
