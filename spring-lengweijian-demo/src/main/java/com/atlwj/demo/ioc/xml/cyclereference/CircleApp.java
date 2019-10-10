package com.atlwj.demo.ioc.xml.cyclereference;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;


/**
 *  结论：
 *  1. 通过set方法，无论bean是单例的还是多实例的，都可以完成循环依赖。
 *  2. 通过构造器完成依赖注入，不管如果bean是多实例的还是单实例的，在循环依赖的时候都会出现【org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'a': Requested bean is currently in creation:
 *     Is there an unresolvable circular reference?】 异常。
 *  3. 使用@Autowired注解完成依赖注入，在循环依赖的场景下，如果bean是单实例的对象，可以完成依赖注入；
 *     但是如果bean是多实例对象的情况下，会出现【org.springframework.beans.factory.BeanCurrentlyInCreationException: Error creating bean with name 'a': Requested bean is currently in creation:
 *     Is there an unresolvable circular reference?】异常。(@Resource注解和@Autowired情况相同。)
 *
 *
 *
 *
 */
@Configurable
@ComponentScan(value = "com.atlwj.demo.ioc.xml.cyclereference")
public class CircleApp {


	public static void main(String[] args) {
		AnnotationConfigApplicationContext ioc = new AnnotationConfigApplicationContext(CircleApp.class);
		A a = ioc.getBean(A.class);
		a.showA();
		B b = ioc.getBean(B.class);
		b.showB();
		C c = ioc.getBean(C.class);
		c.showC();
	}
}
