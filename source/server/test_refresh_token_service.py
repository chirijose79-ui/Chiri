import uuid
from datetime import datetime, timedelta, timezone

from sqlalchemy import create_engine, delete, select
from sqlalchemy.orm import Session as DbSession

from app.application.refresh_token_service import (
    RefreshTokenReuseError,
    rotate_refresh_token,
)
from app.application.session_service import _create_session_without_commit
from app.database.database import SessionLocal
from app.database.models.refresh_token import RefreshToken
from app.database.models.session import Session
from app.database.models.user import User
from app.security.password import hash_password


TEST_USERNAME = "__refresh_test__"
TEST_EMAIL = "__refresh_test__@invalid.local"
TEST_PASSWORD = "TestPassword-2026!"


migration_engine = create_engine(
    __import__("app.config.settings", fromlist=["settings"]).settings.migration_database_url,
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


db = SessionLocal()

try:
    cleanup()

    # ---------------------------------------------------------
    # CREATE TEST USER
    # ---------------------------------------------------------

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

    # ---------------------------------------------------------
    # CREATE SESSION
    # ---------------------------------------------------------

    session = _create_session_without_commit(
        db=db,
        user_id=user.id,
    )

    db.flush()

    # ---------------------------------------------------------
    # CREATE INITIAL REFRESH TOKEN
    # ---------------------------------------------------------

    from app.application.refresh_token_service import (
        _create_refresh_token_without_commit,
    )

    refresh_token, raw_token = (
        _create_refresh_token_without_commit(
            db=db,
            session=session,
        )
    )

    db.commit()

    db.refresh(session)
    db.refresh(refresh_token)

    print("INITIAL_SESSION_ACTIVE=", session.status == "ACTIVE")
    print("INITIAL_REFRESH_TOKEN_ACTIVE=", refresh_token.status == "ACTIVE")
    print(
        "INITIAL_TOKEN_HASHED=",
        refresh_token.token_hash != raw_token,
    )

    # ---------------------------------------------------------
    # ROTATE REFRESH TOKEN
    # ---------------------------------------------------------

    old_token_id = refresh_token.id

    new_refresh_token, new_raw_token = rotate_refresh_token(
        db=db,
        token=raw_token,
    )

    print(
        "OLD_TOKEN_REVOKED=",
        db.scalar(
            select(RefreshToken.status).where(
                RefreshToken.id == old_token_id
            )
        ) == "REVOKED",
    )

    print(
        "NEW_TOKEN_ACTIVE=",
        new_refresh_token.status == "ACTIVE",
    )

    print(
        "NEW_TOKEN_DIFFERENT=",
        new_raw_token != raw_token,
    )

    print(
        "NEW_TOKEN_HASHED=",
        new_refresh_token.token_hash != new_raw_token,
    )

    # ---------------------------------------------------------
    # REUSE OLD TOKEN
    # ---------------------------------------------------------

    try:
        rotate_refresh_token(
            db=db,
            token=raw_token,
        )

    except RefreshTokenReuseError:
        print("OLD_TOKEN_REUSE_REJECTED=True")

    else:
        print("OLD_TOKEN_REUSE_REJECTED=False")

    db.expire_all()

    refreshed_session = db.scalar(
        select(Session).where(
            Session.id == session.id
        )
    )

    print(
        "SESSION_REVOKED_AFTER_REUSE=",
        refreshed_session.status == "REVOKED",
    )

    # ---------------------------------------------------------
    # VERIFY ALL SESSION TOKENS ARE REVOKED
    # ---------------------------------------------------------

    active_tokens = db.scalars(
        select(RefreshToken).where(
            RefreshToken.session_id == session.id,
            RefreshToken.status == "ACTIVE",
        )
    ).all()

    print(
        "NO_ACTIVE_TOKENS_AFTER_REUSE=",
        len(active_tokens) == 0,
    )

finally:
    db.close()
    cleanup()
    migration_engine.dispose()
    