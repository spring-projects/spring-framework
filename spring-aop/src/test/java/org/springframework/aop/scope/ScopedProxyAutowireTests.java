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

package org.springframework.aop.scope;

import java.util.Arrays;

import org.junit.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import static org.junit.Assert.*;
import static org.springframework.tests.TestResourceUtils.*;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class ScopedProxyAutowireTests {

	@Test
	public void testScopedProxyInheritsAutowireCandidateFalse() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireFalse.xml"));

		assertTrue(Arrays.asList(bf.getBeanNamesForType(TestBean.class, false, false)).contains("scoped"));
		assertTrue(Arrays.asList(bf.getBeanNamesForType(TestBean.class, true, false)).contains("scoped"));
		assertFalse(bf.containsSingleton("scoped"));
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean unscoped = (TestBean) bf.getBean("unscoped");
		assertSame(unscoped, autowired.getChild());
	}

	@Test
	public void testScopedProxyReplacesAutowireCandidateTrue() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireTrue.xml"));

		assertTrue(Arrays.asList(bf.getBeanNamesForType(TestBean.class, true, false)).contains("scoped"));
		assertTrue(Arrays.asList(bf.getBeanNamesForType(TestBean.class, false, false)).contains("scoped"));
		assertFalse(bf.containsSingleton("scoped"));
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean scoped = (TestBean) bf.getBean("scoped");
		assertSame(scoped, autowired.getChild());
	}


	static class TestBean {

		private TestBean child;

		public void setChild(TestBean child) {
			this.child = child;
		}

		public TestBean getChild() {
			return this.child;
		}
	}

}
