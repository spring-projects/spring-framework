package com.debug.beanlifecycletest;

import com.debug.beanlifecycle.BeanLifeCycle;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:
 */
public class LifeCycleTest
{

    @Test
    public void testBeanLifeCycle()
    {

        System.out.println("Bean生命周期：");
        System.out.println("Spring容器初始化");
        System.out.println("=====================================");

        ApplicationContext context = new ClassPathXmlApplicationContext("com/debug/config/beanlifecycle.xml");

        System.out.println("Spring容器初始化完毕");
        System.out.println("=====================================");

        System.out.println("从容器中获取Bean");
        System.out.println();

        BeanLifeCycle beanLifeCycle = (BeanLifeCycle) context.getBean("beanlifecycle");

        System.out.println("15.初始化成功，并且属性注入成功：beanLifyCycle Name=" + beanLifeCycle.getName());
        System.out.println("=====================================");

        ((ClassPathXmlApplicationContext) context).close();
        System.out.println("=====================================");
        System.out.println("Spring容器关闭，Bean生命周期结束！");

    }

}
