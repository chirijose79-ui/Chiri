from collections.abc import Generator
from uuid import UUID

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.orm import Session as DbSession

from app.application.session_service import get_active_session
from app.database.database import SessionLocal
from app.database.models.user import User
from app.security.jwt import (
    InvalidAccessTokenError,
    decode_access_token,
)


bearer_scheme = HTTPBearer(
    auto_error=False,
)


def get_db() -> Generator[DbSession, None, None]:
    db = SessionLocal()

    try:
        yield db
    finally:
        db.close()


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(
        bearer_scheme
    ),
    db: DbSession = Depends(get_db),
) -> User:
    if credentials is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Not authenticated",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    if credentials.scheme.lower() != "bearer":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid authentication scheme",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    try:
        payload = decode_access_token(
            credentials.credentials
        )
    except InvalidAccessTokenError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid access token",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    try:
        user_id = UUID(payload["user_id"])
        session_id = UUID(payload["session_id"])
    except (ValueError, TypeError, KeyError):
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid access token",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    session = get_active_session(
        db=db,
        session_id=session_id,
    )

    if session is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Session is not active",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    if session.user_id != user_id:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid access token session",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    user = db.scalar(
        select(User).where(
            User.id == user_id,
        )
    )

    if user is None or user.status != "ACTIVE":
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="User is not active",
            headers={
                "WWW-Authenticate": "Bearer",
            },
        )

    return user
