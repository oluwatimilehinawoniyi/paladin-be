CREATE TABLE notification (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    type VARCHAR(50) NOT NULL,
    title VARCHAR(200) NOT NULL,
    message TEXT NOT NULL,
    related_entity_id UUID NULL,
    related_entity_type VARCHAR(50) NULL,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notification_user
        FOREIGN KEY (user_id)
        REFERENCES users (id)
        ON DELETE CASCADE
);

--  Index for efficiently fetching unread notifications for a user
CREATE INDEX idx_notification_user_read
    ON notification (user_id, is_read);

--  Index for fetching user's notifications ordered by newest first
CREATE INDEX idx_notification_user_created
    ON notification (user_id, created_at DESC);

--  Index for admin or global queries by recency
CREATE INDEX idx_notification_created
    ON notification (created_at DESC);
