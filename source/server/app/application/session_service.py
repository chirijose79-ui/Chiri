from datetime import datetime, timedelta, timezone
import uuid

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
