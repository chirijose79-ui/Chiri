from collections.abc import Generator

from sqlalchemy.orm import Session as DbSession

from app.database.database import SessionLocal


def get_db() -> Generator[DbSession, None, None]:
    db = SessionLocal()

    try:
        yield db
    finally:
        db.close()
