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


TEST_USERNAME = "__home_api_test__"
TEST_EMAIL = "__home_api_test__@invalid.local"
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


def login_test_user() -> str:
    response = client.post(
        "/auth/login",
        json={
            "identifier": TEST_USERNAME,
            "password": TEST_PASSWORD,
        },
    )

    assert response.status_code == 200

    return response.json()["access_token"]


def test_home_with_valid_access_token(test_user):
    access_token = login_test_user()

    response = client.get(
        "/home",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    data = response.json()

    assert data["user"]["display_name"] == TEST_USERNAME
    assert data["home"]["status"] == "operational"


def test_home_without_access_token():
    cleanup()

    response = client.get("/home")

    assert response.status_code == 401
    assert response.json()["detail"] == "Not authenticated"


def test_home_rejects_malformed_jwt():
    cleanup()

    response = client.get(
        "/home",
        headers={
            "Authorization": "Bearer invalid-token",
        },
    )

    assert response.status_code == 401


def test_home_rejects_inactive_user_after_login(test_user):
    access_token = login_test_user()

    with DbSession(migration_engine) as db:
        db_user = db.get(User, test_user.id)

        assert db_user is not None

        db_user.status = "INACTIVE"
        db.commit()

    response = client.get(
        "/home",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 401


def test_home_response_structure(test_user):
    access_token = login_test_user()

    response = client.get(
        "/home",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    data = response.json()

    assert set(data.keys()) == {
        "user",
        "home",
        "quick_actions",
        "information",
    }

    assert set(data["user"].keys()) == {
        "display_name",
    }

    assert set(data["home"].keys()) == {
        "status",
    }

    assert set(data["information"].keys()) == {
        "connectivity",
        "server",
    }

    assert isinstance(data["quick_actions"], list)
    assert len(data["quick_actions"]) == 2

    for action in data["quick_actions"]:
        assert set(action.keys()) == {
            "id",
            "enabled",
        }


def test_home_response_values(test_user):
    access_token = login_test_user()

    response = client.get(
        "/home",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    data = response.json()

    assert data["home"]["status"] in {
        "operational",
        "attention",
        "offline",
    }

    action_ids = {
        action["id"]
        for action in data["quick_actions"]
    }

    assert action_ids == {
        "music",
        "multimedia",
    }

    for action in data["quick_actions"]:
        assert isinstance(action["enabled"], bool)

    assert data["information"]["connectivity"] in {
        "online",
        "offline",
    }

    assert data["information"]["server"] in {
        "online",
        "offline",
    }
      