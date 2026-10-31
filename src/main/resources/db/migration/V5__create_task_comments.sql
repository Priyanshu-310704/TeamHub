CREATE TABLE task_comments (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
                               author_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                               content TEXT NOT NULL,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_task_comments_task_id ON task_comments(task_id);