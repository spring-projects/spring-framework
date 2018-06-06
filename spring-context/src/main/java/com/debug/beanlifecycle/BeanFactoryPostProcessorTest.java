package com.debug.beanlifecycle;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

/**
 * @author: Shawn Chen
 * @date: 2018/6/6
 * @description:自定义BeanFactoryPostProcessor实现类
 */
public class BeanFactoryPostProcessorTest implements BeanFactoryPostProcessor
{
    @Override
    public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException
    {

        System.out.println("1.调用BeanFactoryPostProcessor的postProcessBeanFactory()方法");
        System.out.println();
        System.out.println("1.postProcessBeanFactory()方法在工厂处理器后，ApplicationContext容器初始化中refresh()中调用");
        System.out.println();
    }
}
