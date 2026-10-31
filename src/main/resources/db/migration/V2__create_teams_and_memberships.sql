CREATE TABLE teams (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       name VARCHAR(100) NOT NULL,
                       description VARCHAR(500),
                       created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE team_memberships (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                                  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                                  role VARCHAR(30) NOT NULL CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER')),
                                  joined_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT uk_team_membership_team_user UNIQUE (team_id, user_id)
);

CREATE INDEX idx_team_memberships_team_id ON team_memberships(team_id);
CREATE INDEX idx_team_memberships_user_id ON team_memberships(user_id);