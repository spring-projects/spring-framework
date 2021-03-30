package com.cn.mayf.beanfactorypostprocessor;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathBeanDefinitionScanner;
import org.springframework.core.type.filter.TypeFilter;

/**
 * @Author mayf
 * @Date 2021/3/15 23:18
 */
public class CustomScanner extends ClassPathBeanDefinitionScanner {
	public CustomScanner(BeanDefinitionRegistry registry) {
		super(registry);
	}

	@Override
	// 不需要重写，直接可调用
	public void addIncludeFilter(TypeFilter/*AnnotationTypeFilter*/ includeFilter) {
		// 注册进自定义扫描注解
		super.addIncludeFilter(includeFilter);
	}
}
