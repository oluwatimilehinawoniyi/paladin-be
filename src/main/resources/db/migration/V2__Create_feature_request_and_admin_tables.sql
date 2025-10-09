-- Enable UUID extension (safe if already exists)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Create admin table
CREATE TABLE admin (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    username VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    last_login TIMESTAMP
);

CREATE INDEX idx_admin_username ON admin(username);

-- Create feature_request table
CREATE TABLE feature_request (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT NOT NULL,
    category VARCHAR(50) NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING',
    admin_response TEXT,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_feature_request_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT chk_status CHECK (
        status IN ('PENDING', 'UNDER_REVIEW', 'IN_PROGRESS', 'COMPLETED', 'REJECTED')
    ),
    CONSTRAINT chk_category CHECK (
        category IN ('AI_FEATURES', 'EMAIL_AUTOMATION', 'UI_UX', 'JOB_TRACKING',
                     'CV_MANAGEMENT', 'INTEGRATIONS', 'ANALYTICS', 'PERFORMANCE',
                     'MOBILE', 'OTHER')
    )
);

CREATE INDEX idx_feature_request_created_at ON feature_request(created_at DESC);

-- Create feature_request_vote table
CREATE TABLE feature_request_vote (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    feature_request_id UUID NOT NULL,
    user_id UUID NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),

    CONSTRAINT fk_vote_feature_request FOREIGN KEY (feature_request_id)
        REFERENCES feature_request(id) ON DELETE CASCADE,
    CONSTRAINT fk_vote_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT unique_user_vote UNIQUE (feature_request_id, user_id)
);

-- Indexes
CREATE INDEX idx_feature_request_status ON feature_request(status);
CREATE INDEX idx_feature_request_user_id ON feature_request(user_id);
CREATE INDEX idx_feature_vote_feature_id ON feature_request_vote(feature_request_id);