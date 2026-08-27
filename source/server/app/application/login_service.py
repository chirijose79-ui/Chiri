import uuid

from sqlalchemy import or_, select
from sqlalchemy.orm import Session as DbSession

from app.application.refresh_token_service import (
    _create_refresh_token_without_commit,
)
from app.application.session_service import (
    _create_session_without_commit,
)
from app.database.models.user import User
from app.domain.exceptions import InvalidCredentialsError
from app.security.jwt import create_access_token
from app.security.password import verify_password


def login_user(
    db: DbSession,
    identifier: str,
    password: str,
):
    statement = select(User).where(
        or_(
            User.username == identifier,
            User.email == identifier,
        )
    )

    user = db.scalar(statement)

    if user is None:
        raise InvalidCredentialsError

    if user.status != "ACTIVE":
        raise InvalidCredentialsError

    if not verify_password(password, user.password_hash):
        raise InvalidCredentialsError

    # ---------------------------------------------------------
    # ATOMIC LOGIN TRANSACTION
    # ---------------------------------------------------------
    #
    # Session and refresh token are created without committing.
    # Both are persisted together by the single commit below.
    #

    session = _create_session_without_commit(
        db=db,
        user_id=uuid.UUID(str(user.id)),
    )

    # Persist the session INSERT inside the current transaction
    # before creating the dependent refresh token.
    db.flush()

    refresh_token, raw_refresh_token = (
        _create_refresh_token_without_commit(
            db=db,
            session=session,
        )
    )

    access_token = create_access_token(
        user_id=session.user_id,
        session_id=session.id,
    )

    db.commit()

    db.refresh(session)
    db.refresh(refresh_token)

    return (
        session,
        access_token,
        refresh_token,
        raw_refresh_token,
    )
