package com.luban.test;

import com.luban.config.Appconfig;
import com.luban.dao.UserDao;
import com.luban.dao.UserDaoImpl;
import com.luban.service.UserService;
import com.luban.service.UserServiceImpl;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

public class Test {
    public static void main(String[] args) {
//        BeanFactory beanFactory = new BeanFactory("spring.xml");
//
//        UserService service = (UserService) beanFactory.getBean("service");

        AnnotationConfigApplicationContext annotationConfigApplicationContext = new
                AnnotationConfigApplicationContext();
        annotationConfigApplicationContext.register(Appconfig.class);
//        annotationConfigApplicationContext.register(UserDaoImpl.class);
        annotationConfigApplicationContext.scan("com");
        annotationConfigApplicationContext.refresh();

        System.out.println(annotationConfigApplicationContext.getBean(UserDaoImpl.class).getClass().getName());

        // service.find();
    }
}
