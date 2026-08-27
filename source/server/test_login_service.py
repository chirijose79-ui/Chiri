import uuid

import pytest
from sqlalchemy import create_engine, delete, select
from sqlalchemy.orm import Session as DbSession

from app.application.login_service import login_user
from app.config.settings import settings
from app.database.database import SessionLocal
from app.database.models.refresh_token import RefreshToken
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
            session_ids = db.scalars(
                select(Session.id).where(
                    Session.user_id.in_(user_ids)
                )
            ).all()

            if session_ids:
                db.execute(
                    delete(RefreshToken).where(
                        RefreshToken.session_id.in_(session_ids)
                    )
                )

                db.execute(
                    delete(Session).where(
                        Session.id.in_(session_ids)
                    )
                )

        db.execute(
            delete(User).where(
                User.username == TEST_USERNAME
            )
        )

        db.commit()


@pytest.fixture
def test_user():
    cleanup()

    db = SessionLocal()

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

    yield db, user

    db.close()
    cleanup()


def test_login_success(test_user):
    db, user = test_user

    session, access_token, refresh_token, raw_refresh_token = login_user(
        db=db,
        identifier=TEST_USERNAME,
        password=TEST_PASSWORD,
    )

    assert user.status == "ACTIVE"
    assert session.status == "ACTIVE"
    assert session.user_id == user.id

    assert access_token
    assert refresh_token.status == "ACTIVE"
    assert raw_refresh_token
    assert refresh_token.token_hash != raw_refresh_token


def test_login_wrong_password_rejected(test_user):
    db, _ = test_user

    with pytest.raises(InvalidCredentialsError):
        login_user(
            db=db,
            identifier=TEST_USERNAME,
            password="WrongPassword-2026!",
        )


def test_login_unknown_user_rejected(test_user):
    db, _ = test_user

    with pytest.raises(InvalidCredentialsError):
        login_user(
            db=db,
            identifier="__does_not_exist__",
            password=TEST_PASSWORD,
        )


def test_login_inactive_user_rejected(test_user):
    db, user = test_user

    user.status = "INACTIVE"
    db.commit()

    with pytest.raises(InvalidCredentialsError):
        login_user(
            db=db,
            identifier=TEST_USERNAME,
            password=TEST_PASSWORD,
        )
