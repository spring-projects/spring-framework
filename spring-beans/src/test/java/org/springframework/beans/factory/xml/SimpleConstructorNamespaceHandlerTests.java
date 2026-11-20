/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.DummyBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Costin Leau
 */
class SimpleConstructorNamespaceHandlerTests {

	@Test
	void simpleValue() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "simple";
		//		beanFactory.getBean("simple1", DummyBean.class);
		DummyBean nameValue = beanFactory.getBean(name, DummyBean.class);
		assertThat(nameValue.getValue()).isEqualTo("simple");
	}

	@Test
	void simpleRef() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "simple-ref";
		//		beanFactory.getBean("name-value1", TestBean.class);
		DummyBean nameValue = beanFactory.getBean(name, DummyBean.class);
		assertThat(nameValue.getValue()).isEqualTo(beanFactory.getBean("name"));
	}

	@Test
	void nameValue() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		String name = "name-value";
		//		beanFactory.getBean("name-value1", TestBean.class);
		TestBean nameValue = beanFactory.getBean(name, TestBean.class);
		assertThat(nameValue.getName()).isEqualTo(name);
		assertThat(nameValue.getAge()).isEqualTo(10);
	}

	@Test
	void nameRef() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		TestBean nameValue = beanFactory.getBean("name-value", TestBean.class);
		DummyBean nameRef = beanFactory.getBean("name-ref", DummyBean.class);

		assertThat(nameRef.getName()).isEqualTo("some-name");
		assertThat(nameRef.getSpouse()).isEqualTo(nameValue);
	}

	@Test
	void typeIndexedValue() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean typeRef = beanFactory.getBean("indexed-value", DummyBean.class);

		assertThat(typeRef.getName()).isEqualTo("at");
		assertThat(typeRef.getValue()).isEqualTo("austria");
		assertThat(typeRef.getAge()).isEqualTo(10);
	}

	@Test
	void typeIndexedRef() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean typeRef = beanFactory.getBean("indexed-ref", DummyBean.class);

		assertThat(typeRef.getName()).isEqualTo("some-name");
		assertThat(typeRef.getSpouse()).isEqualTo(beanFactory.getBean("name-value"));
	}

	@Test
	void ambiguousConstructor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		assertThatExceptionOfType(BeanDefinitionStoreException.class).isThrownBy(() ->
				new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
							new ClassPathResource("simpleConstructorNamespaceHandlerTestsWithErrors.xml", getClass())));
	}

	@Test
	void constructorWithNameEndingInRef() {
		DefaultListableBeanFactory beanFactory = createFactory("simpleConstructorNamespaceHandlerTests.xml");
		DummyBean derivedBean = beanFactory.getBean("beanWithRefConstructorArg", DummyBean.class);
		assertThat(derivedBean.getAge()).isEqualTo(10);
		assertThat(derivedBean.getName()).isEqualTo("silly name");
	}

	private DefaultListableBeanFactory createFactory(String resourceName) {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		new XmlBeanDefinitionReader(bf).loadBeanDefinitions(
				new ClassPathResource(resourceName, getClass()));
		return bf;
	}
}
