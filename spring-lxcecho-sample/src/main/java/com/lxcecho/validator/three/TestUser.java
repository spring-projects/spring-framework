package com.lxcecho.validator.three;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class TestUser {

    public static void main(String[] args) {
        ApplicationContext context =
                new AnnotationConfigApplicationContext(ValidationConfig.class);
        MyService service = context.getBean(MyService.class);
        User user = new User();
        user.setName("lucy");
        user.setPhone("13566754321");
        user.setMessage("test a t echo");
        service.testMethod(user);
    }
}
