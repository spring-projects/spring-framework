/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.config;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Simple test to illustrate and verify scope usage.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class SimpleScopeTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setup() {
		beanFactory = new DefaultListableBeanFactory();
		Scope scope = new NoOpScope() {
			private int index;
			private List<TestBean> objects = new ArrayList<>(); {
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
		assertThat(scopeNames).hasSize(1);
		assertThat(scopeNames[0]).isEqualTo("myScope");
		assertThat(beanFactory.getRegisteredScope("myScope")).isSameAs(scope);

		new XmlBeanDefinitionReader(beanFactory).loadBeanDefinitions(
				qualifiedResource(SimpleScopeTests.class, "context.xml"));
	}


	@Test
	void canGetScopedObject() {
		TestBean tb1 = (TestBean) beanFactory.getBean("usesScope");
		TestBean tb2 = (TestBean) beanFactory.getBean("usesScope");
		assertThat(tb2).isNotSameAs(tb1);
		TestBean tb3 = (TestBean) beanFactory.getBean("usesScope");
		assertThat(tb1).isSameAs(tb3);
	}

}
