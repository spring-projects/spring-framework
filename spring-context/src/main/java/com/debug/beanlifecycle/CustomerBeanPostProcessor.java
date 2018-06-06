package com.debug.beanlifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:自定义BeanPostProcessor实现类
 */
public class CustomerBeanPostProcessor implements BeanPostProcessor
{

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        System.out.println("9.调用BeanPostProcessor中的postProcessBeforeInitialization()方法, beanName= " + beanName);
        System.out.println();
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        System.out.println("13.执行BeanPostProcessor的postProcessAfterInitialization()方法,beanName=" + beanName);
        System.out.println();
        return bean;
    }


}
