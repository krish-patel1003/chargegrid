CREATE TABLE user_profiles (
    id UUID PRIMARY KEY,
    keycloak_user_id VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(320) NOT NULL UNIQUE,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_user_profiles_keycloak_user_id
    ON user_profiles (keycloak_user_id);