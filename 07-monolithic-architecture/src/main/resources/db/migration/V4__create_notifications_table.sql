CREATE TABLE notifications (
  id uuid NOT NULL DEFAULT uuid_generate_v4(),
  recipient_id uuid NOT NULL,
  message varchar(500) NOT NULL,
  read boolean NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL DEFAULT now(),
  CONSTRAINT pk_notifications PRIMARY KEY (id),
  CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX ix_notifications_recipient ON notifications (recipient_id);
