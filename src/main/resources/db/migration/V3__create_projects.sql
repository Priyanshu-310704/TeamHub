CREATE TABLE projects (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          team_id UUID NOT NULL REFERENCES teams(id) ON DELETE CASCADE,
                          name VARCHAR(120) NOT NULL,
                          project_key VARCHAR(15) NOT NULL,
                          description TEXT,
                          status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('PLANNED', 'ACTIVE', 'COMPLETED', 'ARCHIVED')),
                          created_by UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                          created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT uk_project_team_key UNIQUE (team_id, project_key)
);

CREATE INDEX idx_projects_team_id ON projects(team_id);