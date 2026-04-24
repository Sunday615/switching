alter table outbox_events
add column updated_at datetime not null default current_timestamp on update current_timestamp;

update outbox_events
set updated_at = created_at
where created_at is not null;