import uuid

import pytest
from sqlalchemy import create_engine, delete, select
from sqlalchemy.orm import Session as DbSession

from app.application.refresh_token_service import (
    RefreshTokenReuseError,
    _create_refresh_token_without_commit,
    rotate_refresh_token,
)
from app.application.session_service import _create_session_without_commit
from app.config.settings import settings
from app.database.database import SessionLocal
from app.database.models.refresh_token import RefreshToken
from app.database.models.session import Session
from app.database.models.user import User
from app.security.password import hash_password


TEST_USERNAME = "__refresh_test__"
TEST_EMAIL = "__refresh_test__@invalid.local"
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
                    User.id.in_(user_ids)
                )
            )

        db.commit()


@pytest.fixture
def refresh_context():
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

    session = _create_session_without_commit(
        db=db,
        user_id=user.id,
    )

    db.flush()

    refresh_token, raw_token = _create_refresh_token_without_commit(
        db=db,
        session=session,
    )

    db.commit()

    db.refresh(session)
    db.refresh(refresh_token)

    yield db, user, session, refresh_token, raw_token

    db.close()
    cleanup()


def test_initial_refresh_token_is_active_and_hashed(refresh_context):
    db, _, session, refresh_token, raw_token = refresh_context

    assert session.status == "ACTIVE"
    assert refresh_token.status == "ACTIVE"
    assert refresh_token.session_id == session.id
    assert refresh_token.token_hash != raw_token


def test_refresh_token_rotation_revokes_old_token(refresh_context):
    db, _, session, refresh_token, raw_token = refresh_context

    old_token_id = refresh_token.id

    new_refresh_token, new_raw_token = rotate_refresh_token(
        db=db,
        token=raw_token,
    )

    old_status = db.scalar(
        select(RefreshToken.status).where(
            RefreshToken.id == old_token_id
        )
    )

    assert old_status == "REVOKED"
    assert new_refresh_token.status == "ACTIVE"
    assert new_refresh_token.session_id == session.id
    assert new_raw_token != raw_token
    assert new_refresh_token.token_hash != new_raw_token


def test_reuse_of_old_refresh_token_revokes_session(
    refresh_context,
):
    db, _, session, refresh_token, raw_token = refresh_context

    rotate_refresh_token(
        db=db,
        token=raw_token,
    )

    with pytest.raises(RefreshTokenReuseError):
        rotate_refresh_token(
            db=db,
            token=raw_token,
        )

    db.expire_all()

    refreshed_session = db.scalar(
        select(Session).where(
            Session.id == session.id
        )
    )

    assert refreshed_session is not None
    assert refreshed_session.status == "REVOKED"


def test_reuse_revokes_all_active_session_tokens(
    refresh_context,
):
    db, _, session, refresh_token, raw_token = refresh_context

    new_refresh_token, _ = rotate_refresh_token(
        db=db,
        token=raw_token,
    )

    with pytest.raises(RefreshTokenReuseError):
        rotate_refresh_token(
            db=db,
            token=raw_token,
        )

    db.expire_all()

    active_tokens = db.scalars(
        select(RefreshToken).where(
            RefreshToken.session_id == session.id,
            RefreshToken.status == "ACTIVE",
        )
    ).all()

    assert len(active_tokens) == 0

    revoked_tokens = db.scalars(
        select(RefreshToken).where(
            RefreshToken.session_id == session.id,
            RefreshToken.status == "REVOKED",
        )
    ).all()

    assert len(revoked_tokens) == 2
