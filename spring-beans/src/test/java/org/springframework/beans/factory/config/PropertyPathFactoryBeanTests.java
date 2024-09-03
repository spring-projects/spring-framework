/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.Resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.core.testfixture.io.ResourceTestUtils.qualifiedResource;

/**
 * Unit tests for {@link PropertyPathFactoryBean}.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 04.10.2004
 */
public class PropertyPathFactoryBeanTests {

	private static final Resource CONTEXT = qualifiedResource(PropertyPathFactoryBeanTests.class, "context.xml");


	@Test
	public void testPropertyPathFactoryBeanWithSingletonResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertThat(xbf.getBean("propertyPath1")).isEqualTo(12);
		assertThat(xbf.getBean("propertyPath2")).isEqualTo(11);
		assertThat(xbf.getBean("tb.age")).isEqualTo(10);
		assertThat(xbf.getType("otb.spouse")).isEqualTo(ITestBean.class);
		Object result1 = xbf.getBean("otb.spouse");
		Object result2 = xbf.getBean("otb.spouse");
		assertThat(result1 instanceof TestBean).isTrue();
		assertThat(result1).isSameAs(result2);
		assertThat(((TestBean) result1).getAge()).isEqualTo(99);
	}

	@Test
	public void testPropertyPathFactoryBeanWithPrototypeResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertThat(xbf.getType("tb.spouse")).isNull();
		assertThat(xbf.getType("propertyPath3")).isEqualTo(TestBean.class);
		Object result1 = xbf.getBean("tb.spouse");
		Object result2 = xbf.getBean("propertyPath3");
		Object result3 = xbf.getBean("propertyPath3");
		assertThat(result1 instanceof TestBean).isTrue();
		assertThat(result2 instanceof TestBean).isTrue();
		assertThat(result3 instanceof TestBean).isTrue();
		assertThat(((TestBean) result1).getAge()).isEqualTo(11);
		assertThat(((TestBean) result2).getAge()).isEqualTo(11);
		assertThat(((TestBean) result3).getAge()).isEqualTo(11);
		assertThat(result1).isNotSameAs(result2);
		assertThat(result1).isNotSameAs(result3);
		assertThat(result2).isNotSameAs(result3);
	}

	@Test
	public void testPropertyPathFactoryBeanWithNullResult() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertThat(xbf.getType("tb.spouse.spouse")).isNull();
		assertThat(xbf.getBean("tb.spouse.spouse").toString()).isEqualTo("null");
	}

	@Test
	public void testPropertyPathFactoryBeanAsInnerBean() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		TestBean spouse = (TestBean) xbf.getBean("otb.spouse");
		TestBean tbWithInner = (TestBean) xbf.getBean("tbWithInner");
		assertThat(tbWithInner.getSpouse()).isSameAs(spouse);
		assertThat(!tbWithInner.getFriends().isEmpty()).isTrue();
		assertThat(tbWithInner.getFriends().iterator().next()).isSameAs(spouse);
	}

	@Test
	public void testPropertyPathFactoryBeanAsNullReference() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertThat(xbf.getBean("tbWithNullReference", TestBean.class).getSpouse()).isNull();
	}

	@Test
	public void testPropertyPathFactoryBeanAsInnerNull() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(xbf).loadBeanDefinitions(CONTEXT);
		assertThat(xbf.getBean("tbWithInnerNull", TestBean.class).getSpouse()).isNull();
	}

}
