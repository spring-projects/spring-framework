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

package org.springframework.beans.factory.config;

import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.tests.sample.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.tests.TestResourceUtils.qualifiedResource;

/**
 * Simple test to illustrate and verify scope usage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class SimpleScopeTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	public void setup() {
		beanFactory = new DefaultListableBeanFactory();
		Scope scope = new NoOpScope() {
			private int index;
			private List<TestBean> objects = new LinkedList<>(); {
				objects.add(new TestBean());
				objects.add(new TestBean());
			}
			@Override
			public Object get(String name, ObjectFactory<?> objectFactory) {
				if (index >= objects.size()) {
					index = 0;
				}
				return objects.get(index++);
			}
		};

		beanFactory.registerScope("myScope", scope);

		String[] scopeNames = beanFactory.getRegisteredScopeNames();
		assertThat(scopeNames.length).isEqualTo(1);
		assertThat(scopeNames[0]).isEqualTo("myScope");
		assertThat(beanFactory.getRegisteredScope("myScope")).isSameAs(scope);

		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				qualifiedResource(SimpleScopeTests.class, "context.xml"));
	}


	@Test
	public void testCanGetScopedObject() {
		TestBean tb1 = (TestBean) beanFactory.getBean("usesScope");
		TestBean tb2 = (TestBean) beanFactory.getBean("usesScope");
		assertThat(tb2).isNotSameAs(tb1);
		TestBean tb3 = (TestBean) beanFactory.getBean("usesScope");
		assertThat(tb1).isSameAs(tb3);
	}

}
