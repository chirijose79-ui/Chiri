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


TEST_USERNAME = "__music_api_test__"
TEST_EMAIL = "__music_api_test__@invalid.local"
TEST_PASSWORD = "TestPassword-2026!"

PLAYER_ID = "up2024ca64"


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


class FakeMusicAssistantClient:
    def __init__(self) -> None:
        self.closed = False

    def search(self, query: str) -> dict:
        assert query == "scientist"

        return {
            "tracks": [
                {
                    "item_id": "221",
                    "name": "The Scientist",
                    "uri": "library://track/221",
                    "duration": 309,
                    "artists": [
                        {
                            "name": "Coldplay",
                        }
                    ],
                    "album": {
                        "name": "A Rush of Blood to the Head",
                    },
                }
            ]
        }

    def get_players(self) -> list[dict]:
        return [
            {
                "id": "up2024ca64",
                "name": "Decodificador multimedia Xiaomi",
                "available": True,
            },
            {
                "id": "up6d1b691c",
                "name": "Samsung Q60AA 50 TV chiri",
                "available": True,
            },
        ]


    def get_session_id(self, player_id: str) -> str | None:
        assert player_id == PLAYER_ID
        return "session-test-123"


    def get_now_playing(self, player_id: str) -> dict | None:
        assert player_id == PLAYER_ID

        return {
            "player_id": PLAYER_ID,
            "state": "playing",
            "track": {
                "id": "221",
                "title": "The Scientist",
                "artist": "Coldplay",
                "album": "A Rush of Blood to the Head",
                "duration": 309,
                "uri": "library://track/221",
                "queue_item_id": "queue-item-1",
            },
            "elapsed": 12,
        }

    def get_queue(self, player_id: str) -> list[dict]:
        assert player_id == PLAYER_ID

        return [
            {
                "id": "queue-item-1",
                "title": "The Scientist",
                "artist": "Coldplay",
                "album": "A Rush of Blood to the Head",
                "duration": 309,
            }
        ]

    def play(self, player_id: str, uri: str):
        assert player_id == PLAYER_ID
        assert uri == "library://track/221"

    def pause(self, player_id: str):
        assert player_id == PLAYER_ID

    def next(self, player_id: str):
        assert player_id == PLAYER_ID

    def previous(self, player_id: str):
        assert player_id == PLAYER_ID

    def close(self) -> None:
        self.closed = True


@pytest.fixture
def mock_music_assistant(monkeypatch):
    monkeypatch.setattr(
        "app.application.music_service.MusicAssistantClient",
        FakeMusicAssistantClient,
    )


def test_music_search(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.get(
        "/music/search?q=scientist",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    data = response.json()

    assert data == {
        "items": [
            {
                "id": "221",
                "title": "The Scientist",
                "artist": "Coldplay",
                "album": "A Rush of Blood to the Head",
                "duration": 309,
                "uri": "library://track/221",
            }
        ]
    }


def test_music_players(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.get(
        "/music/players",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200
    assert response.json() == {
        "items": [
            {
                "id": "up2024ca64",
                "name": "Decodificador multimedia Xiaomi",
                "available": True,
            },
            {
                "id": "up6d1b691c",
                "name": "Samsung Q60AA 50 TV chiri",
                "available": True,
            },
        ],
    }


def test_music_now_playing(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.get(
        f"/music/now-playing?player_id={PLAYER_ID}",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    assert response.json() == {
        "player_id": PLAYER_ID,
        "state": "playing",
        "track": {
            "id": "221",
            "title": "The Scientist",
            "artist": "Coldplay",
            "album": "A Rush of Blood to the Head",
            "duration": 309,
            "uri": "library://track/221",
            "stream_url": "http://192.168.1.88:8097/flow/session-test-123/up2024ca64/queue-item-1/up2024ca64.flac",
        },
        "elapsed": 12,
    }


def test_music_queue(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.get(
        f"/music/queue?player_id={PLAYER_ID}",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
    )

    assert response.status_code == 200

    assert response.json() == {
        "player_id": PLAYER_ID,
        "items": [
            {
                "id": "queue-item-1",
                "title": "The Scientist",
                "artist": "Coldplay",
                "album": "A Rush of Blood to the Head",
                "duration": 309,
            }
        ],
    }


def test_music_play(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.post(
        "/music/play",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
        json={
            "player_id": PLAYER_ID,
            "uri": "library://track/221",
        },
    )

    assert response.status_code == 200
    assert response.json() == {"success": True}


def test_music_pause(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.post(
        "/music/pause",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
        json={
            "player_id": PLAYER_ID,
        },
    )

    assert response.status_code == 200
    assert response.json() == {"success": True}


def test_music_next(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.post(
        "/music/next",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
        json={
            "player_id": PLAYER_ID,
        },
    )

    assert response.status_code == 200
    assert response.json() == {"success": True}


def test_music_previous(test_user, mock_music_assistant):
    access_token = login_test_user()

    response = client.post(
        "/music/previous",
        headers={
            "Authorization": f"Bearer {access_token}",
        },
        json={
            "player_id": PLAYER_ID,
        },
    )

    assert response.status_code == 200
    assert response.json() == {"success": True}


def test_music_without_access_token():
    cleanup()

    response = client.get(
        "/music/search?q=scientist",
    )

    assert response.status_code == 401
    assert response.json()["detail"] == "Not authenticated"


def test_music_rejects_malformed_jwt():
    cleanup()

    response = client.get(
        "/music/search?q=scientist",
        headers={
            "Authorization": "Bearer invalid-token",
        },
    )

    assert response.status_code == 401
