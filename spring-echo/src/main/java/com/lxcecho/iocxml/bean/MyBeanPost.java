package com.lxcecho.iocxml.bean;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class MyBeanPost  implements BeanPostProcessor {

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("3 bean后置处理器，初始化之前执行");
        System.out.println(beanName+"::"+bean);
        return bean;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        System.out.println("5 bean后置处理器，初始化之后执行");
        System.out.println(beanName+"::"+bean);
        return bean;
    }
}
