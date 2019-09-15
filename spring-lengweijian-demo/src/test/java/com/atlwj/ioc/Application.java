package com.atlwj.ioc;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Application {

	public static void main(String[] args) {
		AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(Application.class);
		ConfigurableListableBeanFactory beanFactory = (ConfigurableListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
		//注册可被注入实例
		beanFactory.registerResolvableDependency(Person.class, new StudentObjectFactory());
		//添加BeanDefinition
		RootBeanDefinition objectXDef = new RootBeanDefinition(ObjectX.class);
		objectXDef.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		applicationContext.registerBeanDefinition("objectX", objectXDef);
		//输出com.example.lcc.basic.spring.factory.Application$Person@4f51b3e0
		System.out.println(applicationContext.getBean("objectX", ObjectX.class).person);

		//输出com.example.lcc.basic.spring.factory.Application$Man@282003e1
		System.out.println(applicationContext.getBean("myFactoryBean"));
		//true 多次获取都是同一个对象
		System.out.println(applicationContext.getBean("myFactoryBean") == applicationContext.getBean("myFactoryBean"));
		//获取实际的工厂对象
		//输出com.example.lcc.basic.spring.factory.Application$MyFactoryBean@7fad8c79
		System.out.println(applicationContext.getBean("&myFactoryBean"));
	}

	@Bean
	public FactoryBean<Man> myFactoryBean(){
		return new MyFactoryBean();
	}

	public static class MyFactoryBean implements FactoryBean<Man>{

		@Override
		public Man getObject() throws Exception {
			return new Man();
		}

		@Override
		public Class<?> getObjectType() {
			return Man.class;
		}

		@Override
		public boolean isSingleton() {
			return true;
		}
	}

	public static class ObjectX {
		@Autowired
		Person person;
	}
	public static class Person {

	}
	public static class Man {

	}

	public static class StudentObjectFactory implements ObjectFactory<Person> {

		@Override
		public Person getObject() throws BeansException {
			return new Person();
		}
	}
}