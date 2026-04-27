create table iso_messages (
    id bigint not null auto_increment,

    correlation_ref varchar(100),
    inquiry_ref varchar(100),
    transfer_ref varchar(100),

    end_to_end_id varchar(100),
    message_id varchar(100),

    message_type varchar(50) not null,
    direction varchar(20) not null,

    plain_payload longtext,
    encrypted_payload longtext,

    security_status varchar(30) not null,
    validation_status varchar(30) not null,

    error_code varchar(50),
    error_message varchar(500),

    created_at datetime not null default current_timestamp,

    primary key (id),

    index idx_iso_messages_correlation_ref (correlation_ref),
    index idx_iso_messages_inquiry_ref (inquiry_ref),
    index idx_iso_messages_transfer_ref (transfer_ref),
    index idx_iso_messages_end_to_end_id (end_to_end_id),
    index idx_iso_messages_message_id (message_id),
    index idx_iso_messages_message_type (message_type),
    index idx_iso_messages_direction (direction)
);