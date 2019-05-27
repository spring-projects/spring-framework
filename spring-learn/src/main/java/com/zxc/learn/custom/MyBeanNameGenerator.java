package com.zxc.learn.custom;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.AnnotationBeanNameGenerator;

/**
 * 上士闻道，勤而行之；中士闻道，若存若亡；下士闻道，大笑之。
 *
 * @Description:自定义spring bean name生成器
 * @Author: simon
 * @Create: 2019-05-27 22:24
 * @Version: 1.0.0
 * 上士闻道，勤而行之；中士闻道，若存若亡；下士闻道，大笑之。
 **/
public class MyBeanNameGenerator extends AnnotationBeanNameGenerator {

	@Override
	public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {
		return "zxc-" + super.generateBeanName(definition, registry);
	}
}
