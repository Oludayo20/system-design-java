CREATE TABLE comments (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  post_id uuid NOT NULL,
  user_id uuid NOT NULL,
  body text NOT NULL,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT pk_comments PRIMARY KEY (id),
  CONSTRAINT fk_comments_post FOREIGN KEY (post_id) REFERENCES posts (id) ON DELETE CASCADE,
  CONSTRAINT fk_comments_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_comments_post ON comments (post_id);
CREATE INDEX ix_comments_user ON comments (user_id);
