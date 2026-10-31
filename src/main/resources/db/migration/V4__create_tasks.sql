CREATE TABLE tasks (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       project_id UUID NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
                       task_key VARCHAR(30) NOT NULL,
                       title VARCHAR(200) NOT NULL,
                       description TEXT,
                       status VARCHAR(30) NOT NULL DEFAULT 'TODO' CHECK (status IN ('TODO', 'IN_PROGRESS', 'IN_REVIEW', 'DONE')),
                       priority VARCHAR(30) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
                       assignee_id UUID REFERENCES users(id) ON DELETE SET NULL,
                       reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
                       due_date DATE,
                       position INTEGER NOT NULL DEFAULT 0,
                       created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT uk_tasks_project_task_key UNIQUE (project_id, task_key)
);

CREATE INDEX idx_tasks_project_id ON tasks(project_id);
CREATE INDEX idx_tasks_assignee_id ON tasks(assignee_id);
CREATE INDEX idx_tasks_status ON tasks(status);