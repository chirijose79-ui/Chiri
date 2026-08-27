"""grant runtime refresh token permissions

Revision ID: 42d545a5ced2
Revises: ade06ea3f721
Create Date: 2026-08-27
"""

from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = "42d545a5ced2"
down_revision: Union[str, Sequence[str], None] = "ade06ea3f721"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Grant minimum runtime permissions."""
    op.execute(
        """
        GRANT SELECT, INSERT, UPDATE
        ON TABLE security.refresh_token
        TO chiri_backend
        """
    )


def downgrade() -> None:
    """Revoke runtime permissions."""
    op.execute(
        """
        REVOKE SELECT, INSERT, UPDATE
        ON TABLE security.refresh_token
        FROM chiri_backend
        """
    )
