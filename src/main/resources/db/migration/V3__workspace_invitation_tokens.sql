ALTER TABLE workspace_invitations ADD COLUMN token_hash VARCHAR(64);

CREATE UNIQUE INDEX idx_workspace_invitations_token_hash
    ON workspace_invitations(token_hash)
    WHERE token_hash IS NOT NULL;
