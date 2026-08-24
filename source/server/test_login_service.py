import uuid

from sqlalchemy import create_engine, delete, select
from sqlalchemy.orm import Session as DbSession

from app.application.login_service import login_user
from app.config.settings import settings
from app.database.database import SessionLocal
from app.database.models.session import Session
from app.database.models.user import User
from app.domain.exceptions import InvalidCredentialsError
from app.security.password import hash_password


TEST_USERNAME = "__login_test__"
TEST_EMAIL = "__login_test__@invalid.local"
TEST_PASSWORD = "TestPassword-2026!"


migration_engine = create_engine(
    settings.migration_database_url,
)


def cleanup() -> None:
    with DbSession(migration_engine) as db:
        user_ids = db.scalars(
            select(User.id).where(
                User.username == TEST_USERNAME
            )
        ).all()

        if user_ids:
            db.execute(
                delete(Session).where(
                    Session.user_id.in_(user_ids)
                )
            )

        db.execute(
            delete(User).where(
                User.username == TEST_USERNAME
            )
        )

        db.commit()


db = SessionLocal()

try:
    cleanup()

    user = User(
        id=uuid.uuid4(),
        username=TEST_USERNAME,
        email=TEST_EMAIL,
        password_hash=hash_password(TEST_PASSWORD),
        status="ACTIVE",
    )

    db.add(user)
    db.commit()
    db.refresh(user)

    session = login_user(
        db=db,
        identifier=TEST_USERNAME,
        password=TEST_PASSWORD,
    )

    print("LOGIN_CORRECT")
    print("user_status=", user.status)
    print("session_status=", session.status)
    print("session_user_id=", session.user_id)
    print("session_user_matches=", session.user_id == user.id)

    try:
        login_user(
            db=db,
            identifier=TEST_USERNAME,
            password="WrongPassword-2026!",
        )
    except InvalidCredentialsError:
        print("WRONG_PASSWORD_REJECTED=True")
    else:
        print("WRONG_PASSWORD_REJECTED=False")

    try:
        login_user(
            db=db,
            identifier="__does_not_exist__",
            password=TEST_PASSWORD,
        )
    except InvalidCredentialsError:
        print("UNKNOWN_USER_REJECTED=True")
    else:
        print("UNKNOWN_USER_REJECTED=False")

    user.status = "INACTIVE"
    db.commit()

    try:
        login_user(
            db=db,
            identifier=TEST_USERNAME,
            password=TEST_PASSWORD,
        )
    except InvalidCredentialsError:
        print("INACTIVE_USER_REJECTED=True")
    else:
        print("INACTIVE_USER_REJECTED=False")

finally:
    db.close()
    cleanup()
    migration_engine.dispose()