import uuid

from sqlalchemy import or_, select
from sqlalchemy.orm import Session as DbSession

from app.application.session_service import create_session
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

    session = create_session(
        db=db,
        user_id=uuid.UUID(str(user.id)),
    )

    access_token = create_access_token(
        user_id=session.user_id,
        session_id=session.id,
    )

    return session, access_token
