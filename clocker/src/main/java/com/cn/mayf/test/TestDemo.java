package com.cn.mayf.test;

import com.cn.mayf.app.AppConfig01;
import com.cn.mayf.service.IndexService;
import com.cn.mayf.service.NotBeingLoadService;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ChildBeanDefinition;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * @Author mayf
 * @Date 2021/3/9 12:57
 */
public class TestDemo {
	public static void main(String[] args) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		ac.register(AppConfig01.class);

		/**
		 * 手动把没有被扫描到的类加入容器
		 */
		AbstractBeanDefinition beanDefinition = new GenericBeanDefinition();
		beanDefinition.setBeanClass(NotBeingLoadService.class);
		ac.registerBeanDefinition("xxxx",beanDefinition);


		/*GenericBeanDefinition genericBeanDefinition = new GenericBeanDefinition();
		genericBeanDefinition.setBeanClass(BeanService.class);
		genericBeanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);*/

		/**
		 * 1.模板
		 * 2.可以被实例化的BD==>可以设置setAbstract
		 */
		RootBeanDefinition rbd = new RootBeanDefinition();
		// set方法的值
		rbd.getPropertyValues().add("name","mayf111");
		rbd.setAbstract(true);
		// 可以设置为抽象||也可以不设置为抽象---必须给一个BeanClass
		// rbd.setBeanClass(BeanService.class);
		ac.registerBeanDefinition("ss",rbd);

		/**
		 * 使用模板
		 */
		ChildBeanDefinition childBeanDefinition = new ChildBeanDefinition("ss");
		childBeanDefinition.setBeanClass(IndexService.class);
		ac.registerBeanDefinition("aaa",childBeanDefinition);

		ac.refresh();
		// ((NotBeingLoadService)ac.getBean("xxxx")).test();
		// System.out.println(ac.getBean("ss"));

		System.out.println(ac.getBean(IndexService.class));
	}
}
