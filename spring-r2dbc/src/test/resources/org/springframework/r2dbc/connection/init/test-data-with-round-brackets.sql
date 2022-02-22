create rule insert_emp_view as on insert to emp_view do instead (
    insert into emp values(new.empname, new.salary);
    insert into emp_audit values('I', now(), user, new.*)
);

create rule update_emp_view as on update to emp_view do instead (
    update emp set salary = new.salary where empname = old.empname;
    update emp_audit set operation = 'U', stamp = now(), salary = new.salary where empname = old.empname
);