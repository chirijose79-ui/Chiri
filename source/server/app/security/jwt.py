from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID, uuid4

import jwt

from app.config.settings import settings


ACCESS_TOKEN_MAX_MINUTES = 15
JWT_ALGORITHM = "RS256"


def _load_private_key() -> str:
    path = Path(settings.jwt_private_key_path)

    if not path.is_file():
        raise RuntimeError(
            f"JWT private key not found: {path}"
        )

    return path.read_text(encoding="utf-8")


def create_access_token(
    user_id: UUID,
    session_id: UUID,
) -> str:
    now = datetime.now(timezone.utc)
    expires_at = now + timedelta(minutes=ACCESS_TOKEN_MAX_MINUTES)

    payload = {
        "jti": str(uuid4()),
        "kid": settings.jwt_key_id,
        "iss": settings.jwt_issuer,
        "aud": settings.jwt_audience,
        "iat": now,
        "exp": expires_at,
        "user_id": str(user_id),
        "session_id": str(session_id),
    }

    private_key = _load_private_key()

    return jwt.encode(
        payload,
        private_key,
        algorithm=JWT_ALGORITHM,
        headers={
            "kid": settings.jwt_key_id,
        },
    )
