-- отображение клиентов, у которых больше одного заказа
select c.first_name, c.last_name, count(o.order_date)
from customer c join "order" o on c.id = o.customer_id
group by c.first_name, c.last_name
having count(o.order_date) > 1
order by count(o.order_date) desc
;

-- три самых свежих заказа в статусе создан
select o.id, o.order_date, os.status_name
from "order" o join public.order_status os on os.id = o.status_id
where os.status_name = 'создан'
order by o.order_date desc
limit 3
;

-- 5 самых продаваемых товаров
select p.name, sum(o.quantity)
from "order" o join public.product p on o.product_id = p.id
group by p.name
order by sum(o.quantity) desc
limit 5
;

-- Ид всех клиентов с именем Наталья
select id
from customer
where first_name = 'Наталья'
;

-- все клиенты, которые заказывали товары из категории электроника
select distinct c.id, c.first_name, c.last_name
from
    customer c
    join "order" o on c.id = o.customer_id
    join product p on o.product_id = p.id
where p.category = 'электроника'
;

update "order" set order_date = now() where id = 4;
update customer set last_name = 'Жопова' where last_name = 'Попова'; -- женилась =)
update product set price = price + 1 where category = 'мебель'; -- инфляция мебели

delete from "order" where id = 0;

insert into customer(first_name, last_name, phone, email)
values ('Бомж', 'Бомж', 'нет', 'нет');

select c.id
from customer c left join "order" o on c.id = o.customer_id
where o.id is null;

-- удаляем беззаказных
delete from customer where id in (
    select c.id
    from customer c left join "order" o on c.id = o.customer_id
    where o.id is null
);

update product set quantity = quantity + 100 where id = 1