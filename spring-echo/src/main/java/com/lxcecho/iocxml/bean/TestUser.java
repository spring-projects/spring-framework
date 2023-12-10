package com.lxcecho.iocxml.bean;

import com.lxcecho.iocxml.bean.User;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class TestUser {

    public static void main(String[] args) {
        ApplicationContext context = new
                ClassPathXmlApplicationContext("bean.xml");
        //1 根据id获取bean
        User user1 = (User)context.getBean("user1");
        System.out.println("1 根据id获取bean: "+user1);

        //2 根据类型获取bean
//        User user2 = context.getBean(User.class);
//        System.out.println("2 根据类型获取bean: "+user2);

        //3 根据id和类型获取bean
//        User user3 = context.getBean("user", User.class);
//        System.out.println("3 根据id和类型获取bean: "+user3);
    }
}
