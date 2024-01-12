/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class ScopedProxyAutowireTests {

	@Test
	void testScopedProxyInheritsAutowireCandidateFalse() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireFalse.xml"));

		assertThat(Arrays.asList(bf.getBeanNamesForType(TestBean.class, false, false))).contains("scoped");
		assertThat(Arrays.asList(bf.getBeanNamesForType(TestBean.class, true, false))).contains("scoped");
		assertThat(bf.containsSingleton("scoped")).isFalse();
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean unscoped = (TestBean) bf.getBean("unscoped");
		assertThat(autowired.getChild()).isSameAs(unscoped);
	}

	@Test
	void testScopedProxyReplacesAutowireCandidateTrue() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(ScopedProxyAutowireTests.class, "scopedAutowireTrue.xml"));

		assertThat(Arrays.asList(bf.getBeanNamesForType(TestBean.class, true, false))).contains("scoped");
		assertThat(Arrays.asList(bf.getBeanNamesForType(TestBean.class, false, false))).contains("scoped");
		assertThat(bf.containsSingleton("scoped")).isFalse();
		TestBean autowired = (TestBean) bf.getBean("autowired");
		TestBean scoped = (TestBean) bf.getBean("scoped");
		assertThat(autowired.getChild()).isSameAs(scoped);
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
