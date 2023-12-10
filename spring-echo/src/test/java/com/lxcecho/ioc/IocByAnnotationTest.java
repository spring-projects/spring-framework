package com.lxcecho.ioc;

import com.lxcecho.ioc.iocannoaop.config.SpringConfig;
import com.lxcecho.ioc.iocannoaop.controller.AutowiredUserController;
import com.lxcecho.ioc.iocannoaop.controller.ResourceUserController;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author lxcecho azaki0426@gmail.com
 * @since 2023/12/10
 */
public class IocByAnnotationTest {

	@Test
	public void testAnnotationXml() {
		ApplicationContext context = new ClassPathXmlApplicationContext("bean-annoioc.xml");
		AutowiredUserController controller = context.getBean(AutowiredUserController.class);
		controller.add();
	}

	@Test
	public void testAnnotation() {
		// 加载配置类
		ApplicationContext context = new AnnotationConfigApplicationContext(SpringConfig.class);
		ResourceUserController controller = context.getBean(ResourceUserController.class);
		controller.add();
	}

}
