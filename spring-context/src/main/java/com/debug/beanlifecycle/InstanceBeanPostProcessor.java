package com.debug.beanlifecycle;


import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyValues;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;

import java.beans.PropertyDescriptor;
import java.util.Arrays;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:自定义InstantiationAwareBeanPostProcessor类
 */
public class InstanceBeanPostProcessor implements InstantiationAwareBeanPostProcessor
{
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException
    {
        System.out.println("10.调用InstantiationAwareBeanPostProcessor中的postProcessBeforeInitialization()方法, beanName = " + beanName);
        System.out.println();
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException
    {
        System.out.println("14.执行InstantiationAwareBeanPostProcessor的postProcessAfterInitialization()方法");
        System.out.println();
        return bean;
    }

    @Override
    public boolean postProcessAfterInstantiation(Object bean, String beanName) throws BeansException
    {
        System.out.println("4.调用InstantiationAwareBeanPostProcessor中的postProcessAfterInstantiation()方法");
        System.out.println();
        System.out.println("4.返回boolean,bean实例化后调用,并且返回false则不会注入属性,beanName" + beanName);
        System.out.println();
        return true;
    }


    @Override
    public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException
    {
        System.out.println("2.调用InstantiationAwareBeanPostProcessor中的postProcessBeforeInstantiation()方法");
        System.out.println();
        System.out.println("2.实例化bean之前调用,即调用bean类构造函数之前调用 " + beanClass.getName());
        System.out.println();
        return null;
    }

    @Override
    public PropertyValues postProcessPropertyValues(PropertyValues pvs, PropertyDescriptor[] pds, Object bean, String beanName) throws BeansException
    {
        System.out.println("5.调用InstantiationAwareBeanPostProcessor中的postProcessPropertyValues()方法");
        System.out.println();
        System.out.println("5.postProcessPropertyValues,在属性注入之前调用...... beanName = " + beanName + " 属性名集合 : " + Arrays.toString(pvs.getPropertyValues()));
        System.out.println();
        System.out.println("这里Bean的name属性还是null：" + ((BeanLifeCycle) bean).getName()); //这里可以看到nam的值
        System.out.println();
        return pvs;//这里要返回propertyValues,否则属性无法注入
    }
}
