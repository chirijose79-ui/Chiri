from datetime import datetime
from uuid import UUID

from fastapi import APIRouter, Depends, HTTPException, status
from pydantic import BaseModel, Field
from sqlalchemy.orm import Session as DbSession

from app.application.login_service import login_user
from app.api.dependencies import get_db
from app.domain.exceptions import InvalidCredentialsError


router = APIRouter(
    prefix="/auth",
    tags=["authentication"],
)


class LoginRequest(BaseModel):
    identifier: str = Field(min_length=1)
    password: str = Field(min_length=1)


class LoginResponse(BaseModel):
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
        session = login_user(
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
        session_id=session.id,
        user_id=session.user_id,
        expires_at=session.expires_at,
    )
