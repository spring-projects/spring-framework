/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.framework;

import org.junit.jupiter.api.Test;

import org.springframework.aop.interceptor.DebugInterceptor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * Integration test for Objenesis proxy creation.
 *
 * @author Oliver Gierke
 */
class ObjenesisProxyTests {

	@Test
	void appliesAspectToClassWithComplexConstructor() {
		ApplicationContext context = new ClassPathXmlApplicationContext("ObjenesisProxyTests-context.xml", getClass());

		ClassWithComplexConstructor bean = context.getBean(ClassWithComplexConstructor.class);
		bean.method();

		DebugInterceptor interceptor = context.getBean(DebugInterceptor.class);
		assertThat(interceptor.getCount()).isEqualTo(1L);
		assertThat(bean.getDependency().getValue()).isEqualTo(1);
	}

}
