-- Failed DROP can be ignored if necessary
drop table users;

-- Create the test table
create table users (last_name varchar(50) not null);
