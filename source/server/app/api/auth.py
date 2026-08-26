from datetime import datetime
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session as DbSession

from app.api.dependencies import get_current_user, get_db
from app.database.models.user import User
from app.application.login_service import login_user
from app.domain.exceptions import InvalidCredentialsError


router = APIRouter(
    prefix="/auth",
    tags=["authentication"],
)


class LoginRequest(BaseModel):
    identifier: str = Field(min_length=1)
    password: str = Field(min_length=1)


class LoginResponse(BaseModel):
    access_token: str
    token_type: str
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
        session, access_token = login_user(
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
