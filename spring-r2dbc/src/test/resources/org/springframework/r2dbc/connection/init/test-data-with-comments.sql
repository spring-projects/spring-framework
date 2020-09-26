-- The next comment line has no text after the '--' prefix.
--
-- The next comment line starts with a space.
 -- x, y, z...

insert into customer (id, name)
values	(1, 'Rod; Johnson'), (2, 'Adrian Collier');
-- This is also a comment.
insert into orders(id, order_date, customer_id)
values (1, '2008-01-02', 2);
insert into orders(id, order_date, customer_id) values (1, '2008-01-02', 2);
INSERT INTO persons( person_id--      
                   , name)
VALUES( 1      -- person_id
      , 'Name' --name
);--