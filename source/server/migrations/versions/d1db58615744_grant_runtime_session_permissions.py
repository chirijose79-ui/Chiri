"""grant runtime session permissions

Revision ID: d1db58615744
Revises: 3976b8c687b3
Create Date: 2026-08-23 18:31:33.544587

"""
from typing import Sequence, Union

from alembic import op


# revision identifiers, used by Alembic.
revision: str = "d1db58615744"
down_revision: Union[str, Sequence[str], None] = "3976b8c687b3"
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """Grant minimum runtime permissions for security.session."""
    op.execute(
        "GRANT USAGE ON SCHEMA security TO chiri_backend"
    )
    op.execute(
        "GRANT SELECT, INSERT, UPDATE "
        "ON TABLE security.session TO chiri_backend"
    )


def downgrade() -> None:
    """Revoke runtime permissions for security.session."""
    op.execute(
        "REVOKE SELECT, INSERT, UPDATE "
        "ON TABLE security.session FROM chiri_backend"
    )
    op.execute(
        "REVOKE USAGE ON SCHEMA security FROM chiri_backend"
    )
