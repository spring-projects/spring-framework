/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.aop.target;

import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Stephane Nicoll
 */
class CommonsPool2TargetSourceProxyTests {

	private static final Resource CONTEXT =
		qualifiedResource(CommonsPool2TargetSourceProxyTests.class, "context.xml");

	@Test
	void proxy() {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(CONTEXT);
		beanFactory.preInstantiateSingletons();
		ITestBean bean = (ITestBean)beanFactory.getBean("testBean");
		assertThat(AopUtils.isAopProxy(bean)).isTrue();
	}
}
