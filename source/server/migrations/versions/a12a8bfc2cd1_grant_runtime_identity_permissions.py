"""grant runtime identity permissions

Revision ID: a12a8bfc2cd1
Revises: d1db58615744
Create Date: 2026-08-23 23:19:51.945830

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = "a12a8bfc2cd1"
down_revision: Union[str, Sequence[str], None] = "d1db58615744"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Grant runtime permissions for identity.user."""
    op.execute(
        "GRANT USAGE ON SCHEMA identity TO chiri_backend"
    )

    op.execute(
        """
        GRANT SELECT, INSERT, UPDATE
        ON TABLE identity."user"
        TO chiri_backend
        """
    )


def downgrade() -> None:
    """Revoke runtime permissions for identity.user."""
    op.execute(
        """
        REVOKE SELECT, INSERT, UPDATE
        ON TABLE identity."user"
        FROM chiri_backend
        """
    )

    op.execute(
        "REVOKE USAGE ON SCHEMA identity FROM chiri_backend"
    )