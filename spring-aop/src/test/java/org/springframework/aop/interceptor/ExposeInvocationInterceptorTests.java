/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.interceptor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Non-XML tests are in AbstractAopProxyTests
 *
 * @author Rod Johnson
 * @author Chris Beams
 */
class ExposeInvocationInterceptorTests {

	@Test
	void xmlConfig() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ExposeInvocationInterceptorTests.class, "context.xml"));
		ITestBean tb = (ITestBean) bf.getBean("proxy");
		String name = "tony";
		tb.setName(name);
		// Fires context checks
		assertThat(tb.getName()).isEqualTo(name);
	}

}
