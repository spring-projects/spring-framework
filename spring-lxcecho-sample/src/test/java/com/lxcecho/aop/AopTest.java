package com.lxcecho.aop;

import com.lxcecho.aop.annoaop.Calculator;
import com.lxcecho.aop.annoaop.SpringAopConfig;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/11
 */
public class AopTest {

	@Test
	public void testAnnoAop() {
		ApplicationContext context = new AnnotationConfigApplicationContext(SpringAopConfig.class);
		Calculator calculator = context.getBean(Calculator.class);
		System.out.println(calculator.div(10, 3));
	}

	@Test
	public void testAdd() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-aopanno.xml");
		Calculator calculator = context.getBean(Calculator.class);
		calculator.add(2, 3);
	}

	@Test
	public void testAdd02() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-aopxml.xml");
		com.lxcecho.aop.xmlaop.Calculator calculator = context.getBean(com.lxcecho.aop.xmlaop.Calculator.class);
		calculator.add(4, 3);
	}

}
