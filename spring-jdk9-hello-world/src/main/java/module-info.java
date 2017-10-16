module hello {
    requires spring.beans;
    requires java.sql;
    requires spring.context;

    exports hello.public_module_area;
    exports hello.private_module_area;

    opens hello.public_module_area;
}