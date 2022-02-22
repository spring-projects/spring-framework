create function insert_emp_view() returns trigger as $$
    insert into emp_audit values('D', user, old.*);
$$ LANGUAGE plpgsql;

create function update_emp_view() returns trigger as $$
    update emp_audit set operation = 'U', stamp = now(), salary = new.salary where empname = old.empname;
$$ LANGUAGE plpgsql;
