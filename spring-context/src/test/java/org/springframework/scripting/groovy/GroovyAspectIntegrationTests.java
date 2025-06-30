/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
