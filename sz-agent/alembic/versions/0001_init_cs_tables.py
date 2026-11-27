"""初始化 5 张 PostgreSQL 客服业务数据表。

修订版本 ID：0001_init_cs_tables
前置版本：
创建时间：2026-11-26 00:00:00
"""

from typing import Sequence, Union

import sqlalchemy as sa
from alembic import op
from sqlalchemy.dialects import postgresql

revision: str = "0001_init_cs_tables"
down_revision: Union[str, Sequence[str], None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    op.create_table(
        "cs_sessions",
        sa.Column("id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("user_id", sa.BigInteger(), nullable=False),
        sa.Column("title", sa.String(length=128), nullable=True),
        sa.Column(
            "status",
            sa.String(length=16),
            server_default=sa.text("'ACTIVE'"),
            nullable=False,
        ),
        sa.Column(
            "has_pending_interrupt",
            sa.Boolean(),
            server_default=sa.text("false"),
            nullable=False,
        ),
        sa.Column("last_intent", sa.String(length=32), nullable=True),
        sa.Column("last_message_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "idx_cs_sessions_user_time",
        "cs_sessions",
        ["user_id", sa.literal_column("last_message_at DESC")],
        unique=False,
    )

    op.create_table(
        "cs_messages",
        sa.Column("id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("session_id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("user_id", sa.BigInteger(), nullable=False),
        sa.Column("role", sa.String(length=16), nullable=False),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column("intent", sa.String(length=32), nullable=True),
        sa.Column(
            "cards_json",
            postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), "sqlite"),
            nullable=True,
        ),
        sa.Column(
            "citations_json",
            postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), "sqlite"),
            nullable=True,
        ),
        sa.Column(
            "interrupt_payload",
            postgresql.JSONB(astext_type=sa.Text()).with_variant(sa.JSON(), "sqlite"),
            nullable=True,
        ),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["session_id"], ["cs_sessions.id"], ondelete="CASCADE"
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "idx_cs_messages_session_time",
        "cs_messages",
        ["session_id", sa.literal_column("created_at ASC")],
        unique=False,
    )

    op.create_table(
        "cs_tickets",
        sa.Column("id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("ticket_no", sa.String(length=32), nullable=False),
        sa.Column("user_id", sa.BigInteger(), nullable=False),
        sa.Column("session_id", sa.Uuid(as_uuid=True), nullable=True),
        sa.Column("category", sa.String(length=32), nullable=False),
        sa.Column("title", sa.String(length=128), nullable=False),
        sa.Column("summary", sa.Text(), nullable=True),
        sa.Column("related_order_no", sa.String(length=64), nullable=True),
        sa.Column(
            "status",
            sa.String(length=16),
            server_default=sa.text("'OPEN'"),
            nullable=False,
        ),
        sa.Column("last_reply_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column("closed_at", sa.DateTime(timezone=True), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["session_id"], ["cs_sessions.id"], ondelete="SET NULL"
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("ticket_no", name="uq_cs_tickets_ticket_no"),
    )
    op.create_index(
        "idx_cs_tickets_user",
        "cs_tickets",
        ["user_id", sa.literal_column("created_at DESC")],
        unique=False,
    )
    op.create_index(
        "idx_cs_tickets_status",
        "cs_tickets",
        ["status", sa.literal_column("updated_at DESC")],
        unique=False,
    )

    op.create_table(
        "cs_ticket_replies",
        sa.Column("id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("ticket_id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("sender_type", sa.String(length=16), nullable=False),
        sa.Column("sender_id", sa.BigInteger(), nullable=False),
        sa.Column("sender_name", sa.String(length=64), nullable=True),
        sa.Column("content", sa.Text(), nullable=False),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.ForeignKeyConstraint(
            ["ticket_id"], ["cs_tickets.id"], ondelete="CASCADE"
        ),
        sa.PrimaryKeyConstraint("id"),
    )
    op.create_index(
        "idx_cs_ticket_replies_ticket",
        "cs_ticket_replies",
        ["ticket_id", sa.literal_column("created_at ASC")],
        unique=False,
    )

    op.create_table(
        "kb_documents",
        sa.Column("id", sa.Uuid(as_uuid=True), nullable=False),
        sa.Column("title", sa.String(length=256), nullable=False),
        sa.Column("category", sa.String(length=32), nullable=False),
        sa.Column("file_name", sa.String(length=256), nullable=True),
        sa.Column("file_path", sa.String(length=512), nullable=True),
        sa.Column("file_sha256", sa.String(length=64), nullable=True),
        sa.Column("file_size", sa.BigInteger(), nullable=True),
        sa.Column("mineru_mode", sa.String(length=32), nullable=True),
        sa.Column(
            "status",
            sa.String(length=16),
            server_default=sa.text("'PENDING'"),
            nullable=False,
        ),
        sa.Column(
            "chunk_count",
            sa.Integer(),
            server_default=sa.text("0"),
            nullable=False,
        ),
        sa.Column("error_msg", sa.Text(), nullable=True),
        sa.Column("uploaded_by", sa.BigInteger(), nullable=True),
        sa.Column(
            "created_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.Column(
            "updated_at",
            sa.DateTime(timezone=True),
            server_default=sa.func.now(),
            nullable=False,
        ),
        sa.PrimaryKeyConstraint("id"),
        sa.UniqueConstraint("file_sha256", name="uk_kb_doc_sha256"),
    )
    op.create_index("idx_kb_doc_status", "kb_documents", ["status"], unique=False)


def downgrade() -> None:
    op.drop_index("idx_kb_doc_status", table_name="kb_documents")
    op.drop_table("kb_documents")

    op.drop_index("idx_cs_ticket_replies_ticket", table_name="cs_ticket_replies")
    op.drop_table("cs_ticket_replies")

    op.drop_index("idx_cs_tickets_status", table_name="cs_tickets")
    op.drop_index("idx_cs_tickets_user", table_name="cs_tickets")
    op.drop_table("cs_tickets")

    op.drop_index("idx_cs_messages_session_time", table_name="cs_messages")
    op.drop_table("cs_messages")

    op.drop_index("idx_cs_sessions_user_time", table_name="cs_sessions")
    op.drop_table("cs_sessions")
