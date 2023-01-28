package org.springframework.beans.factory.config;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class BeanUserDefineTest {

	@Test
	public  void beanUserDefineTest() {
		AbstractApplicationContext context = new ClassPathXmlApplicationContext("lifecycleTests.xml", getClass());
		context.start();
		BeanUserDefine bean1 = (BeanUserDefine) context.getBean("beanUserDefine");
		System.out.println(bean1);

	}

}
