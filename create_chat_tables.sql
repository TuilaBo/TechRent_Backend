-- Kiểm tra và tạo bảng Conversation
-- Thử với tên có quotes trước (nếu hibernate.globally_quoted_identifiers=true)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'Conversation' AND table_schema = 'public') THEN
        EXECUTE '
        CREATE TABLE "Conversation" (
            conversation_id BIGSERIAL PRIMARY KEY,
            dispute_id BIGINT,
            customer_id BIGINT NOT NULL,
            staff_id BIGINT NOT NULL,
            created_at TIMESTAMP,
            updated_at TIMESTAMP
        )';
        
        -- Thêm foreign keys nếu các bảng tồn tại
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'Dispute' AND table_schema = 'public') THEN
            EXECUTE 'ALTER TABLE "Conversation" ADD CONSTRAINT fk_conversation_dispute FOREIGN KEY (dispute_id) REFERENCES "Dispute"(dispute_id) ON DELETE SET NULL';
        END IF;
        
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE (table_name = 'Customer' OR table_name = 'customer') AND table_schema = 'public') THEN
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'Customer' AND table_schema = 'public') THEN
                EXECUTE 'ALTER TABLE "Conversation" ADD CONSTRAINT fk_conversation_customer FOREIGN KEY (customer_id) REFERENCES "Customer"(customer_id) ON DELETE CASCADE';
            ELSE
                EXECUTE 'ALTER TABLE "Conversation" ADD CONSTRAINT fk_conversation_customer FOREIGN KEY (customer_id) REFERENCES customer(customer_id) ON DELETE CASCADE';
            END IF;
        END IF;
        
        IF EXISTS (SELECT 1 FROM information_schema.tables WHERE (table_name = 'Staff' OR table_name = 'staff') AND table_schema = 'public') THEN
            IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'Staff' AND table_schema = 'public') THEN
                EXECUTE 'ALTER TABLE "Conversation" ADD CONSTRAINT fk_conversation_staff FOREIGN KEY (staff_id) REFERENCES "Staff"(staff_id) ON DELETE CASCADE';
            ELSE
                EXECUTE 'ALTER TABLE "Conversation" ADD CONSTRAINT fk_conversation_staff FOREIGN KEY (staff_id) REFERENCES staff(staff_id) ON DELETE CASCADE';
            END IF;
        END IF;
    END IF;
END $$;

-- Tạo bảng ChatMessage
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'ChatMessage' AND table_schema = 'public') THEN
        EXECUTE '
        CREATE TABLE "ChatMessage" (
            message_id BIGSERIAL PRIMARY KEY,
            conversation_id BIGINT NOT NULL,
            sender_type VARCHAR(50) NOT NULL,
            sender_id BIGINT NOT NULL,
            content TEXT NOT NULL,
            is_read BOOLEAN NOT NULL DEFAULT FALSE,
            read_at TIMESTAMP,
            sent_at TIMESTAMP NOT NULL,
            CONSTRAINT fk_chatmessage_conversation FOREIGN KEY (conversation_id) REFERENCES "Conversation"(conversation_id) ON DELETE CASCADE
        )';
    END IF;
END $$;

-- Tạo index cho performance
CREATE INDEX IF NOT EXISTS idx_conversation_customer ON "Conversation"(customer_id);
CREATE INDEX IF NOT EXISTS idx_conversation_staff ON "Conversation"(staff_id);
CREATE INDEX IF NOT EXISTS idx_chatmessage_conversation ON "ChatMessage"(conversation_id);
CREATE INDEX IF NOT EXISTS idx_chatmessage_sent_at ON "ChatMessage"(sent_at DESC);

