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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.DependencyDescriptor;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Tests for {@link CustomAutowireConfigurer}.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class CustomAutowireConfigurerTests {

	@Test
	void testCustomResolver() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				qualifiedResource(CustomAutowireConfigurerTests.class, "context.xml"));

		CustomAutowireConfigurer cac = new CustomAutowireConfigurer();
		CustomResolver customResolver = new CustomResolver();
		bf.setAutowireCandidateResolver(customResolver);
		cac.postProcessBeanFactory(bf);
		TestBean testBean = (TestBean) bf.getBean("testBean");
		assertThat(testBean.getName()).isEqualTo("#1!");
	}


	public static class TestBean {

		private String name;

		public TestBean(String name) {
			this.name = name;
		}

		public String getName() {
			return this.name;
		}
	}


	public static class CustomResolver implements AutowireCandidateResolver {

		@Override
		public boolean isAutowireCandidate(BeanDefinitionHolder bdHolder, DependencyDescriptor descriptor) {
			if (!bdHolder.getBeanDefinition().isAutowireCandidate()) {
				return false;
			}
			if (!bdHolder.getBeanName().matches("[a-z-]+")) {
				return false;
			}
			if (bdHolder.getBeanDefinition().getAttribute("priority").equals("1")) {
				return true;
			}
			return false;
		}

		@Override
		public Object getSuggestedValue(DependencyDescriptor descriptor) {
			return null;
		}

		@Override
		public Object getLazyResolutionProxyIfNecessary(DependencyDescriptor descriptor, String beanName) {
			return null;
		}
	}

}
