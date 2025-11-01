-- Tạo bảng conversation (không có foreign keys trước)
CREATE TABLE IF NOT EXISTS conversation (
    conversation_id BIGSERIAL PRIMARY KEY,
    dispute_id BIGINT,
    customer_id BIGINT NOT NULL,
    staff_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

-- Tạo bảng chat_message
CREATE TABLE IF NOT EXISTS chat_message (
    message_id BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    sender_type VARCHAR(50) NOT NULL,
    sender_id BIGINT NOT NULL,
    content text NOT NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    read_at TIMESTAMP,
    sent_at TIMESTAMP NOT NULL
);

-- Thêm foreign key cho chat_message -> conversation
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_chatmessage_conversation'
    ) THEN
        ALTER TABLE chat_message 
        ADD CONSTRAINT fk_chatmessage_conversation 
        FOREIGN KEY (conversation_id) 
        REFERENCES conversation(conversation_id) ON DELETE CASCADE;
    END IF;
END $$;

-- Tạo index cho performance
CREATE INDEX IF NOT EXISTS idx_conversation_customer ON conversation(customer_id);
CREATE INDEX IF NOT EXISTS idx_conversation_staff ON conversation(staff_id);
CREATE INDEX IF NOT EXISTS idx_chatmessage_conversation ON chat_message(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chatmessage_sent_at ON chat_message(sent_at DESC);

