from datetime import datetime
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy import select
from sqlalchemy.orm import Session as DbSession

from app.api.dependencies import (
    get_current_session,
    get_current_user,
    get_db,
)
from app.application.login_service import login_user
from app.application.refresh_token_service import (
    RefreshTokenReuseError,
    RefreshTokenValidationError,
    rotate_refresh_token,
)
from app.application.session_service import revoke_session
from app.database.models.session import Session
from app.database.models.user import User
from app.domain.exceptions import InvalidCredentialsError
from app.security.jwt import create_access_token


router = APIRouter(
    prefix="/auth",
    tags=["authentication"],
)


class LoginRequest(BaseModel):
    identifier: str = Field(min_length=1)
    password: str = Field(min_length=1)


class RefreshRequest(BaseModel):
    refresh_token: str = Field(min_length=1)


class LoginResponse(BaseModel):
    access_token: str
    token_type: str
    refresh_token: str
    session_id: UUID
    user_id: UUID
    expires_at: datetime


class RefreshResponse(BaseModel):
    access_token: str
    token_type: str
    refresh_token: str
    session_id: UUID
    user_id: UUID
    expires_at: datetime


@router.post(
    "/login",
    response_model=LoginResponse,
)
def login(
    payload: LoginRequest,
    db: DbSession = Depends(get_db),
) -> LoginResponse:
    try:
        (
            session,
            access_token,
            refresh_token,
            raw_refresh_token,
        ) = login_user(
            db=db,
            identifier=payload.identifier,
            password=payload.password,
        )

    except InvalidCredentialsError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid credentials",
        )

    return LoginResponse(
        access_token=access_token,
        token_type="Bearer",
        refresh_token=raw_refresh_token,
        session_id=session.id,
        user_id=session.user_id,
        expires_at=session.expires_at,
    )


@router.post(
    "/refresh",
    response_model=RefreshResponse,
)
def refresh(
    payload: RefreshRequest,
    db: DbSession = Depends(get_db),
) -> RefreshResponse:
    try:
        refresh_token, raw_refresh_token = rotate_refresh_token(
            db=db,
            token=payload.refresh_token,
        )

    except RefreshTokenReuseError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token reuse detected",
        )

    except RefreshTokenValidationError as exc:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail=str(exc),
        )

    except ValueError:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid refresh token",
        )

    session = db.scalar(
        select(Session).where(
            Session.id == refresh_token.session_id,
        )
    )

    if session is None:
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Refresh token session not found",
        )

    access_token = create_access_token(
        user_id=session.user_id,
        session_id=session.id,
    )

    return RefreshResponse(
        access_token=access_token,
        token_type="Bearer",
        refresh_token=raw_refresh_token,
        session_id=session.id,
        user_id=session.user_id,
        expires_at=session.expires_at,
    )


@router.get(
    "/me",
)
def get_me(
    current_user: User = Depends(get_current_user),
) -> dict:
    return {
        "user_id": str(current_user.id),
        "username": current_user.username,
        "email": current_user.email,
    }


@router.post(
    "/logout",
)
def logout(
    session: Session = Depends(get_current_session),
    db: DbSession = Depends(get_db),
) -> dict:
    revoke_session(
        db=db,
        session=session,
    )

    return {
        "message": "Logged out successfully",
    }
