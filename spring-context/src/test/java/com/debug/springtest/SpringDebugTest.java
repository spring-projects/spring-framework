package com.debug.springtest;


import com.debug.spring.User;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:spring源码简单测试
 */
public class SpringDebugTest
{
    @Test
    public void test()
    {
        ApplicationContext context = new ClassPathXmlApplicationContext("com/debug/config/User.xml");

        User user = (User) context.getBean("user");

        System.out.println(user.getClass().getName());

        System.out.println("name属性:" + user.getName());
        System.out.println("gender属性:" + user.getGender());

    }
}
