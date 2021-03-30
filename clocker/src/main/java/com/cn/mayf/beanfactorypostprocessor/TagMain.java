package com.cn.mayf.beanfactorypostprocessor;

import com.cn.AppConfig;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.filter.TypeFilter;

import java.io.IOException;

/**
 * @Author mayf
 * @Date 2021/3/15 20:47
 */
public class TagMain extends Test001 {
	/**
	 * 精读spring源码，能够对spring做二次开发
	 *
	 * 1.invokeBeanFactoryPostProcessors(BeanFactory)
	 * 	1扫描Bean,
	 * 	2解析BeanDefinition,
	 * 	3将BeanDefinition放入DefaultListableBeanFactory中的beanDefinitionMap，
	 * 	以上功能由spring内置提供的BeanFactoryPostProcessor(BeanDefinitionRegistryPostProcessor)来完成
	 * 		ConfigurationClassPostProcessor
	 * 	4 修改Spring的BeanDefinition
	 * 		由spring提供(cglib代理)和程序员自己提供的BeanFactoryPostProcessor来完成
	 */
	public TagMain() {
		System.out.println(TagMain.class.getSimpleName());
	}

	public static void main(String[] args) throws IOException {
//		TagMain main = new TagMain();
//		MetadataReader reader = null;
//
//		MetadataReaderFactory factory = null;
//
//		main.test((aaa,bbb)->true);
		AnnotationConfigApplicationContext ac =
				new AnnotationConfigApplicationContext(AppConfig.class);
	}

	public void test(TypeFilter typeFilter) throws IOException {
		System.out.println("1111111111");
		System.out.println(typeFilter.match(null,null));
	}
}
