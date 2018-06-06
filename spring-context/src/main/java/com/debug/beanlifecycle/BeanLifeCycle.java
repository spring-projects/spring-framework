package com.debug.beanlifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.*;
import org.springframework.context.*;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.util.Arrays;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:测试主类
 */
public class BeanLifeCycle implements InitializingBean, DisposableBean, ApplicationContextAware, ApplicationEventPublisherAware, BeanClassLoaderAware, BeanFactoryAware, BeanNameAware, EnvironmentAware, ResourceLoaderAware
{

    private String name;

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        System.out.println("6.setName(name),属性注入后调用,此时name=" + name);
        System.out.println();
        this.name = name;
    }

    public BeanLifeCycle()
    {
        System.out.println("3.调用BeanLifeCycle无参构造函数，进行实例化");
        System.out.println();
    }

    @Override
    public void afterPropertiesSet() throws Exception
    {
        //processBeforeInitialization(BeanPostProcessor)后调用
        System.out.println("11.调用InitializingBean的afterPropertiesSet()，processBeforeInitialization之后,配置的initMethod之前调用");
        System.out.println();
    }

    @Override
    public void destroy() throws Exception
    {
        System.out.println("16.执行DisposableBean接口的destroy方法");
        System.out.println();
    }


    /**
     * 通过<bean>的destroy-method属性指定的销毁方法
     *
     * @throws Exception
     */
    public void destroyMethod() throws Exception
    {
        System.out.println("17.执行配置的destroy-method()方法");
        System.out.println();
    }

    /**
     * 通过<bean>的init-method属性指定的初始化方法
     *
     * @throws Exception
     */
    public void initMethod() throws Exception
    {
        System.out.println("12.BeanLifyCycle执行配置的init-method()");
        System.out.println();
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader)
    {
        System.out.println("执行setBeanClassLoader,ClassLoader Name = " + classLoader.getClass().getName());
        System.out.println();
    }

    @Override
    public void setBeanFactory(BeanFactory beanFactory) throws BeansException
    {
        //setBeanName 后调用
        System.out.println("8.调用BeanFactoryAware的setBeanFactory()方法，setBeanName后调用，BeanLifeCycle bean singleton=" + beanFactory.isSingleton("beanlifecycle"));
        System.out.println();
    }

    @Override
    public void setBeanName(String name)
    {
        System.out.println("7.调用BeanNameAware的setBeanName()方法，设置Bean名字:: Bean Name defined in context=" + name);
        System.out.println();
    }


    @Override
    public void setEnvironment(Environment environment)
    {
        System.out.println("执行setEnvironment");
        System.out.println();
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader)
    {

        Resource resource = resourceLoader.getResource("classpath:beanlifecycle.xml");
        System.out.println("执行setResourceLoader:: Resource File Name=" + resource.getFilename());
        System.out.println();
    }

    @Override
    public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher)
    {
        System.out.println("执行setApplicationEventPublisher");
        System.out.println();
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException
    {
        System.out.println("执行setApplicationContext:: Bean Definition Names=" + Arrays.toString(applicationContext.getBeanDefinitionNames()));
        System.out.println();
    }


}
