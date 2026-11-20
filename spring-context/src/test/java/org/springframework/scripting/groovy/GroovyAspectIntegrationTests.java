/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.scripting.groovy;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * @author Dave Syer
 */
class GroovyAspectIntegrationTests {

	private GenericXmlApplicationContext context;

	@Test
	void javaBean() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-java-context.xml");
		TestService bean = context.getBean("javaBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThatRuntimeException()
			.isThrownBy(bean::sayHello)
			.withMessage("TestServiceImpl");
		assertThat(logAdvice.getCountThrows()).isEqualTo(1);
	}

	@Test
	void groovyBeanInterface() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-interface-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThatRuntimeException()
			.isThrownBy(bean::sayHello)
			.withMessage("GroovyServiceImpl");
		assertThat(logAdvice.getCountThrows()).isEqualTo(1);
	}

	@Test
	void groovyBeanDynamic() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-dynamic-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThatRuntimeException()
			.isThrownBy(bean::sayHello)
			.withMessage("GroovyServiceImpl");
		// No proxy here because the pointcut only applies to the concrete class, not the interface
		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThat(logAdvice.getCountBefore()).isEqualTo(0);
	}

	@Test
	void groovyBeanProxyTargetClass() {
		context = new GenericXmlApplicationContext(getClass(), getClass().getSimpleName() + "-groovy-proxy-target-class-context.xml");
		TestService bean = context.getBean("groovyBean", TestService.class);
		LogUserAdvice logAdvice = context.getBean(LogUserAdvice.class);

		assertThat(logAdvice.getCountThrows()).isEqualTo(0);
		assertThatRuntimeException()
			.isThrownBy(bean::sayHello)
			.withMessage("GroovyServiceImpl");
		assertThat(logAdvice.getCountBefore()).isEqualTo(1);
		assertThat(logAdvice.getCountThrows()).isEqualTo(1);
	}

	@AfterEach
	void close() {
		if (context != null) {
			context.close();
		}
	}

}
