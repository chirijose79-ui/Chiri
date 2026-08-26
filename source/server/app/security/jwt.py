from datetime import datetime, timedelta, timezone
from pathlib import Path
from uuid import UUID, uuid4

import jwt

from app.config.settings import settings


ACCESS_TOKEN_MAX_MINUTES = 15
JWT_ALGORITHM = "RS256"


class InvalidAccessTokenError(Exception):
    pass


def _load_private_key() -> str:
    path = Path(settings.jwt_private_key_path)

    if not path.is_file():
        raise RuntimeError(
            f"JWT private key not found: {path}"
        )

    return path.read_text(encoding="utf-8")


def _load_public_key() -> str:
    private_key_path = Path(settings.jwt_private_key_path)
    public_key_path = private_key_path.with_name("public_key.pem")

    if not public_key_path.is_file():
        raise RuntimeError(
            f"JWT public key not found: {public_key_path}"
        )

    return public_key_path.read_text(encoding="utf-8")


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


def decode_access_token(
    token: str,
) -> dict:
    try:
        header = jwt.get_unverified_header(token)
    except jwt.InvalidTokenError as exc:
        raise InvalidAccessTokenError(
            "Invalid access token header"
        ) from exc

    token_kid = header.get("kid")

    if token_kid != settings.jwt_key_id:
        raise InvalidAccessTokenError(
            "Unknown access token key id"
        )

    try:
        public_key = _load_public_key()

        payload = jwt.decode(
            token,
            public_key,
            algorithms=[JWT_ALGORITHM],
            issuer=settings.jwt_issuer,
            audience=settings.jwt_audience,
            options={
                "require": [
                    "jti",
                    "kid",
                    "iss",
                    "aud",
                    "iat",
                    "exp",
                    "user_id",
                    "session_id",
                ],
                "verify_signature": True,
                "verify_exp": True,
                "verify_iat": True,
                "verify_iss": True,
                "verify_aud": True,
            },
        )

    except jwt.ExpiredSignatureError as exc:
        raise InvalidAccessTokenError(
            "Access token expired"
        ) from exc

    except jwt.InvalidIssuerError as exc:
        raise InvalidAccessTokenError(
            "Invalid access token issuer"
        ) from exc

    except jwt.InvalidAudienceError as exc:
        raise InvalidAccessTokenError(
            "Invalid access token audience"
        ) from exc

    except jwt.InvalidIssuedAtError as exc:
        raise InvalidAccessTokenError(
            "Invalid access token issued-at claim"
        ) from exc

    except jwt.InvalidTokenError as exc:
        raise InvalidAccessTokenError(
            "Invalid access token"
        ) from exc

    if payload.get("kid") != settings.jwt_key_id:
        raise InvalidAccessTokenError(
            "Invalid access token key id"
        )

    try:
        UUID(payload["user_id"])
        UUID(payload["session_id"])
    except (ValueError, TypeError, AttributeError) as exc:
        raise InvalidAccessTokenError(
            "Invalid access token identity claims"
        ) from exc

    return payload
