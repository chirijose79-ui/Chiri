from datetime import datetime, timedelta, timezone
import uuid

from sqlalchemy import select
from sqlalchemy.orm import Session as DbSession

from app.database.models.session import Session


SESSION_MAX_DAYS = 30


def create_session(
    db: DbSession,
    user_id: uuid.UUID,
) -> Session:
    now = datetime.now(timezone.utc)

    session = Session(
        id=uuid.uuid4(),
        user_id=user_id,
        created_at=now,
        expires_at=now + timedelta(days=SESSION_MAX_DAYS),
        status="ACTIVE",
    )

    db.add(session)
    db.commit()
    db.refresh(session)

    return session


def revoke_session(
    db: DbSession,
    session: Session,
) -> Session:
    if session.status == "ACTIVE":
        session.status = "REVOKED"
        db.commit()
        db.refresh(session)

    return session


def get_active_session(
    db: DbSession,
    session_id: uuid.UUID,
) -> Session | None:
    statement = select(Session).where(
        Session.id == session_id,
    )

    session = db.scalar(statement)

    if session is None:
        return None

    now = datetime.now(timezone.utc)

    if session.status != "ACTIVE":
        return None

    if session.expires_at <= now:
        return None

    return session
