import uuid

import pytest
from fastapi.testclient import TestClient
from sqlalchemy import create_engine, delete, select
from sqlalchemy.orm import Session as DbSession

from app.config.settings import settings
from app.database.database import SessionLocal
from app.database.models.refresh_token import RefreshToken
from app.database.models.session import Session
from app.database.models.user import User
from app.main import app
from app.security.password import hash_password


TEST_USERNAME = "__api_test__"
TEST_EMAIL = "__api_test__@invalid.local"
TEST_PASSWORD = "TestPassword-2026!"


migration_engine = create_engine(
    settings.migration_database_url,
)

client = TestClient(app)


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

    yield user

    db.close()
    cleanup()


def test_health():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_login_success(test_user):
    response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert response.status_code == 200

    data = response.json()

    assert data["access_token"]
    assert data["token_type"] == "Bearer"
    assert data["refresh_token"]
    assert data["session_id"]
    assert data["user_id"] == str(test_user.id)
    assert data["expires_at"]


def test_me_with_valid_access_token(test_user):
    login_response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert login_response.status_code == 200

    access_token = login_response.json()["access_token"]

    response = client.get(
        "/auth/me",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    assert response.json() == {
        "user_id": str(test_user.id),
        "username": TEST_USERNAME,
        "email": TEST_EMAIL,
    }


def test_me_without_access_token():
    response = client.get("/auth/me")

    assert response.status_code == 401
    assert response.json()["detail"] == "Not authenticated"


def test_logout_revokes_session(test_user):
    login_response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert login_response.status_code == 200

    data = login_response.json()

    access_token = data["access_token"]
    session_id = data["session_id"]

    logout_response = client.post(
        "/auth/logout",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert logout_response.status_code == 200
    assert logout_response.json() == {
        "message": "Logged out successfully",
    }

    with DbSession(migration_engine) as db:
        session = db.scalar(
            select(Session).where(
                Session.id == uuid.UUID(session_id)
            )
        )

        assert session is not None
        assert session.status == "REVOKED"


def test_me_rejected_after_logout(test_user):
    login_response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert login_response.status_code == 200

    access_token = login_response.json()["access_token"]

    logout_response = client.post(
        "/auth/logout",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert logout_response.status_code == 200

    me_response = client.get(
        "/auth/me",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert me_response.status_code == 401
    assert me_response.json()["detail"] == "Session is not active"

def test_refresh_rejected_after_logout_and_token_revoked(test_user):
    login_response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert login_response.status_code == 200

    data = login_response.json()

    access_token = data["access_token"]
    refresh_token = data["refresh_token"]
    session_id = data["session_id"]

    logout_response = client.post(
        "/auth/logout",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert logout_response.status_code == 200

    refresh_response = client.post(
        "/auth/refresh",
        json={
            "refresh_token": refresh_token,
        },
    )

    assert refresh_response.status_code == 401
    assert refresh_response.json()["detail"] == "Invalid refresh token"

    with DbSession(migration_engine) as db:
        db_session = db.scalar(
            select(Session).where(
                Session.id == uuid.UUID(session_id)
            )
        )

        assert db_session is not None
        assert db_session.status == "REVOKED"

        db_refresh_token = db.scalar(
            select(RefreshToken).where(
                RefreshToken.session_id == db_session.id
            )
        )

        assert db_refresh_token is not None
        assert db_refresh_token.status == "REVOKED"
