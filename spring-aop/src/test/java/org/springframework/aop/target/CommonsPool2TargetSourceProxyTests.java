/*
 * Copyright 2002-2019 the original author or authors.
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
public class CommonsPool2TargetSourceProxyTests {

	private static final Resource CONTEXT =
		qualifiedResource(CommonsPool2TargetSourceProxyTests.class, "context.xml");

	@Test
	public void testProxy() throws Exception {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(CONTEXT);
		beanFactory.preInstantiateSingletons();
		ITestBean bean = (ITestBean)beanFactory.getBean("testBean");
		assertThat(AopUtils.isAopProxy(bean)).isTrue();
	}
}
