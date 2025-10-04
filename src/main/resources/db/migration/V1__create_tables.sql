create table if not exists product (
    id bigint generated always as identity primary key,
    name text not null unique,
    description text not null,
    price integer not null check ( price >= 0 ),
    quantity integer not null check ( quantity >= 0 ),
    category text not null
);

comment on column product.category is 'какая-то произвольная категория текстом, так как не хочу делать таблицу с категориями';

create table if not exists customer (
    id bigint generated always as identity primary key,
    first_name text not null,
    last_name text not null,
    phone text not null unique,
    email text not null unique
);

create table if not exists order_status (
    id smallint generated always as identity primary key,
    status_name text not null
);

insert into order_status (status_name) values
    ('создан'),
    ('в обработке'),
    ('отправлен'),
    ('доставлен'),
    ('отменён');

create table if not exists "order" (
    id bigint generated always as identity primary key,
    product_id bigint not null references product(id),
    customer_id bigint not null references customer(id),
    order_date timestamptz not null default now(),
    quantity integer not null check ( quantity >= 0 ),
    status_id smallint not null references order_status(id)
);

comment on column "order".order_date is 'дата создания заказа';