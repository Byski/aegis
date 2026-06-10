create table app_user (
    id            uuid         primary key,
    username      varchar(64)  not null unique,
    password_hash varchar(255) not null,
    role          varchar(16)  not null,
    created_at    timestamptz  not null
);

create table link (
    id         uuid          primary key,
    code       varchar(64)   not null unique,
    long_url   varchar(2048) not null,
    owner_id   uuid          not null,
    created_at timestamptz   not null,
    expires_at timestamptz,
    constraint fk_link_owner foreign key (owner_id) references app_user (id)
);

create index idx_link_owner on link (owner_id);
