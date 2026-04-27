alter table idempotency_records
add column updated_at datetime null;

update idempotency_records
set updated_at = created_at
where updated_at is null
  and created_at is not null;

alter table idempotency_records
modify column updated_at datetime not null default current_timestamp on update current_timestamp;