from datetime import datetime, timedelta, timezone
import uuid

from sqlalchemy import select, update
from sqlalchemy.orm import Session as DbSession

from app.database.models.refresh_token import RefreshToken
from app.database.models.session import Session
from app.security.refresh_token import (
    generate_refresh_token,
    hash_refresh_token,
)


REFRESH_TOKEN_MAX_DAYS = 30


class RefreshTokenReuseError(Exception):
    """
    Raised when a previously invalidated refresh token is reused.
    """


def create_refresh_token(
    db: DbSession,
    session: Session,
) -> tuple[RefreshToken, str]:
    """
    Create a refresh token associated with an active session.

    The raw token is returned only once.
    Only its hash is persisted in the database.

    This function commits the transaction.
    """

    if session.status != "ACTIVE":
        raise ValueError(
            "Cannot create refresh token for inactive session"
        )

    now = datetime.now(timezone.utc)

    expires_at = min(
        now + timedelta(days=REFRESH_TOKEN_MAX_DAYS),
        session.expires_at,
    )

    raw_token = generate_refresh_token()
    token_hash = hash_refresh_token(raw_token)

    refresh_token = RefreshToken(
        id=uuid.uuid4(),
        session_id=session.id,
        token_hash=token_hash,
        created_at=now,
        expires_at=expires_at,
        status="ACTIVE",
    )

    db.add(refresh_token)
    db.commit()
    db.refresh(refresh_token)

    return refresh_token, raw_token


def get_refresh_token(
    db: DbSession,
    token: str,
) -> RefreshToken | None:
    """
    Find a refresh token by its protected hash.

    This function does not modify token state.
    """

    token_hash = hash_refresh_token(token)

    statement = select(RefreshToken).where(
        RefreshToken.token_hash == token_hash,
    )

    return db.scalar(statement)


def _revoke_all_session_refresh_tokens(
    db: DbSession,
    session_id: uuid.UUID,
) -> None:
    """
    Revoke all refresh tokens associated with a session.

    Used when refresh-token reuse or another invalid
    refresh-token state is detected.
    """

    statement = (
        update(RefreshToken)
        .where(
            RefreshToken.session_id == session_id,
            RefreshToken.status == "ACTIVE",
        )
        .values(status="REVOKED")
    )

    db.execute(statement)


def _get_refresh_token_for_update(
    db: DbSession,
    token: str,
) -> RefreshToken | None:
    """
    Find a refresh token and lock its row
    for the current database transaction.
    """

    token_hash = hash_refresh_token(token)

    statement = (
        select(RefreshToken)
        .where(
            RefreshToken.token_hash == token_hash,
        )
        .with_for_update()
    )

    return db.scalar(statement)


def get_active_refresh_token(
    db: DbSession,
    token: str,
) -> RefreshToken | None:
    """
    Find an active and non-expired refresh token.

    Expired ACTIVE tokens are transitioned to EXPIRED.
    """

    refresh_token = get_refresh_token(
        db=db,
        token=token,
    )

    if refresh_token is None:
        return None

    now = datetime.now(timezone.utc)

    if refresh_token.status != "ACTIVE":
        return None

    if refresh_token.expires_at <= now:
        refresh_token.status = "EXPIRED"

        db.commit()
        db.refresh(refresh_token)

        return None

    return refresh_token


def revoke_refresh_token(
    db: DbSession,
    refresh_token: RefreshToken,
) -> RefreshToken:
    """
    Revoke an active refresh token.
    """

    if refresh_token.status == "ACTIVE":
        refresh_token.status = "REVOKED"

        db.commit()
        db.refresh(refresh_token)

    return refresh_token


def _create_refresh_token_without_commit(
    db: DbSession,
    session: Session,
) -> tuple[RefreshToken, str]:
    """
    Create a refresh token without committing.

    This helper is used by rotation so that revocation
    of the old token and creation of the new token
    happen inside the same database transaction.
    """

    if session.status != "ACTIVE":
        raise ValueError(
            "Cannot create refresh token for inactive session"
        )

    now = datetime.now(timezone.utc)

    expires_at = min(
        now + timedelta(days=REFRESH_TOKEN_MAX_DAYS),
        session.expires_at,
    )

    raw_token = generate_refresh_token()
    token_hash = hash_refresh_token(raw_token)

    refresh_token = RefreshToken(
        id=uuid.uuid4(),
        session_id=session.id,
        token_hash=token_hash,
        created_at=now,
        expires_at=expires_at,
        status="ACTIVE",
    )

    db.add(refresh_token)

    return refresh_token, raw_token


def rotate_refresh_token(
    db: DbSession,
    token: str,
) -> tuple[RefreshToken, str]:
    """
    Rotate a refresh token atomically.

    A valid ACTIVE token is revoked and replaced
    by a new token.

    Reuse of a previously invalidated token is treated
    as a security event and revokes the associated session.

    The revocation of the old token and creation of the
    replacement token are committed as one transaction.
    """

    refresh_token = _get_refresh_token_for_update(
        db=db,
        token=token,
    )

    if refresh_token is None:
        raise ValueError("Invalid refresh token")

    session = db.scalar(
        select(Session).where(
            Session.id == refresh_token.session_id,
        )
    )

    if session is None:
        raise ValueError("Refresh token session not found")

    now = datetime.now(timezone.utc)

    # Previously invalidated token:
    # possible refresh-token reuse attack.
    if refresh_token.status in ("REVOKED", "EXPIRED"):
        session.status = "REVOKED"

        _revoke_all_session_refresh_tokens(
            db=db,
            session_id=session.id,
        )

        db.commit()

        raise RefreshTokenReuseError(
            "Refresh token reuse detected"
        )

    # Token is not ACTIVE for any unexpected reason.
    if refresh_token.status != "ACTIVE":
        session.status = "REVOKED"

        _revoke_all_session_refresh_tokens(
            db=db,
            session_id=session.id,
        )

        db.commit()

        raise RefreshTokenReuseError(
            "Invalid refresh token state"
        )

    # The session itself must still be valid.
    if session.status != "ACTIVE":
        refresh_token.status = "REVOKED"

        db.commit()

        raise ValueError("Session is not active")

    if session.expires_at <= now:
        session.status = "EXPIRED"
        refresh_token.status = "EXPIRED"

        db.commit()

        raise ValueError("Session expired")

    # Refresh token expiration.
    if refresh_token.expires_at <= now:
        refresh_token.status = "EXPIRED"

        db.commit()

        raise ValueError("Refresh token expired")

    # ---------------------------------------------------------
    # ATOMIC ROTATION
    # ---------------------------------------------------------
    #
    # The old token is revoked and the new token is created
    # without an intermediate commit.
    #
    # A single commit below persists both changes together.
    #

    refresh_token.status = "REVOKED"

    new_refresh_token, raw_token = (
        _create_refresh_token_without_commit(
            db=db,
            session=session,
        )
    )

    db.commit()

    db.refresh(refresh_token)
    db.refresh(new_refresh_token)

    return new_refresh_token, raw_token
