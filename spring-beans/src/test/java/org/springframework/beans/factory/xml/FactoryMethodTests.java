/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.FactoryMethods;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
class FactoryMethodTests {

	@Test
	void factoryMethodsSingletonOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean");
		assertThat(tb.getName()).isEqualTo("defaultInstance");
		assertThat(tb.getAge()).isEqualTo(1);

		FactoryMethods fm = (FactoryMethods) xbf.getBean("default");
		assertThat(fm.getNum()).isEqualTo(0);
		assertThat(fm.getName()).isEqualTo("default");
		assertThat(fm.getTestBean().getName()).isEqualTo("defaultInstance");
		assertThat(fm.getStringValue()).isEqualTo("setterString");

		fm = (FactoryMethods) xbf.getBean("testBeanOnly");
		assertThat(fm.getNum()).isEqualTo(0);
		assertThat(fm.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("full");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isEqualTo("gotcha");
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("full");
		assertThat(fm2).isSameAs(fm);

		xbf.destroySingletons();
		assertThat(tb.wasDestroyed()).isTrue();
	}

	@Test
	void factoryMethodsWithInvalidDestroyMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("defaultTestBeanWithInvalidDestroyMethod"));
	}

	@Test
	void factoryMethodsWithNullInstance() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertThat(xbf.getBean("null").toString()).isEqualTo("null");
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("nullWithProperty"));
	}

	@Test
	void factoryMethodsWithNullValue() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithNull");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isNull();
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("fullWithGenericNull");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isNull();
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");

		fm = (FactoryMethods) xbf.getBean("fullWithNamedNull");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isNull();
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
	}

	@Test
	void factoryMethodsWithAutowire() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithAutowire");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isEqualTo("gotchaAutowired");
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
	}

	@Test
	void protectedFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.protected");
		assertThat(tb.getAge()).isEqualTo(1);
	}

	@Test
	void privateFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.private");
		assertThat(tb.getAge()).isEqualTo(1);
	}

	@Test
	void factoryMethodsPrototypeOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		FactoryMethods fm = (FactoryMethods) xbf.getBean("defaultPrototype");
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("defaultPrototype");
		assertThat(fm.getNum()).isEqualTo(0);
		assertThat(fm.getName()).isEqualTo("default");
		assertThat(fm.getTestBean().getName()).isEqualTo("defaultInstance");
		assertThat(fm.getStringValue()).isEqualTo("setterString");
		assertThat(fm2.getNum()).isEqualTo(fm.getNum());
		assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean is created separately for each bean
		assertThat(fm2.getTestBean()).isNotSameAs(fm.getTestBean());
		assertThat(fm2).isNotSameAs(fm);

		fm = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		assertThat(fm.getNum()).isEqualTo(0);
		assertThat(fm.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
		assertThat(fm2.getNum()).isEqualTo(fm.getNum());
		assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertThat(fm2.getTestBean()).isSameAs(fm.getTestBean());
		assertThat(fm2).isNotSameAs(fm);

		fm = (FactoryMethods) xbf.getBean("fullPrototype");
		fm2 = (FactoryMethods) xbf.getBean("fullPrototype");
		assertThat(fm.getNum()).isEqualTo(27);
		assertThat(fm.getName()).isEqualTo("gotcha");
		assertThat(fm.getTestBean().getName()).isEqualTo("Juergen");
		assertThat(fm2.getNum()).isEqualTo(fm.getNum());
		assertThat(fm2.getStringValue()).isEqualTo(fm.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertThat(fm2.getTestBean()).isSameAs(fm.getTestBean());
		assertThat(fm2).isNotSameAs(fm);
	}

	/**
	 * Tests where the static factory method is on a different class.
	 */
	@Test
	void factoryMethodsOnExternalClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertThat(xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
		assertThat(xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
		String[] names = xbf.getBeanNamesForType(TestBean.class);
		assertThat(Arrays.asList(names)).contains("externalFactoryMethodWithoutArgs");
		assertThat(Arrays.asList(names)).contains("externalFactoryMethodWithArgs");

		TestBean tb = (TestBean) xbf.getBean("externalFactoryMethodWithoutArgs");
		assertThat(tb.getAge()).isEqualTo(2);
		assertThat(tb.getName()).isEqualTo("Tristan");
		tb = (TestBean) xbf.getBean("externalFactoryMethodWithArgs");
		assertThat(tb.getAge()).isEqualTo(33);
		assertThat(tb.getName()).isEqualTo("Rod");

		assertThat(xbf.getType("externalFactoryMethodWithoutArgs")).isEqualTo(TestBean.class);
		assertThat(xbf.getType("externalFactoryMethodWithArgs")).isEqualTo(TestBean.class);
		names = xbf.getBeanNamesForType(TestBean.class);
		assertThat(Arrays.asList(names)).contains("externalFactoryMethodWithoutArgs");
		assertThat(Arrays.asList(names)).contains("externalFactoryMethodWithArgs");
	}

	@Test
	void instanceFactoryMethodWithoutArgs() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		InstanceFactory.count = 0;
		xbf.preInstantiateSingletons();
		assertThat(InstanceFactory.count).isEqualTo(1);
		FactoryMethods fm = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertThat(fm.getTestBean().getName()).isEqualTo("instanceFactory");
		assertThat(InstanceFactory.count).isEqualTo(1);
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertThat(fm2.getTestBean().getName()).isEqualTo("instanceFactory");
		assertThat(fm).isSameAs(fm2);
		assertThat(InstanceFactory.count).isEqualTo(1);
	}

	@Test
	void factoryMethodNoMatchingStaticMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).as("No static method matched").isThrownBy(() ->
				xbf.getBean("noMatchPrototype"));
	}

	@Test
	void nonExistingFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("invalidPrototype"))
			.withMessageContaining("nonExisting(TestBean)");
	}

	@Test
	void factoryMethodArgumentsForNonExistingMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				xbf.getBean("invalidPrototype", new TestBean()))
			.withMessageContaining("nonExisting(TestBean)");
	}

	@Test
	void canSpecifyFactoryMethodArgumentsOnFactoryMethodPrototype() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		TestBean tbArg = new TestBean();
		tbArg.setName("arg1");
		TestBean tbArg2 = new TestBean();
		tbArg2.setName("arg2");

		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertThat(fm1.getNum()).isEqualTo(0);
		assertThat(fm1.getName()).isEqualTo("default");
		// This comes from the test bean
		assertThat(fm1.getTestBean().getName()).isEqualTo("arg1");

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2);
		assertThat(fm2.getTestBean().getName()).isEqualTo("arg2");
		assertThat(fm2.getNum()).isEqualTo(fm1.getNum());
		assertThat(fm2.getStringValue()).isEqualTo("testBeanOnlyPrototypeDISetterString");
		assertThat(fm2.getStringValue()).isEqualTo(fm2.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertThat(fm2.getTestBean()).isSameAs(fm2.getTestBean());
		assertThat(fm2).isNotSameAs(fm1);

		FactoryMethods fm3 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2, 1, "myName");
		assertThat(fm3.getNum()).isEqualTo(1);
		assertThat(fm3.getName()).isEqualTo("myName");
		assertThat(fm3.getTestBean().getName()).isEqualTo("arg2");

		FactoryMethods fm4 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertThat(fm4.getNum()).isEqualTo(0);
		assertThat(fm4.getName()).isEqualTo("default");
		assertThat(fm4.getTestBean().getName()).isEqualTo("arg1");
	}

	@Test
	void canSpecifyFactoryMethodArgumentsOnSingleton() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		TestBean tb = new TestBean();
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly", tb);
		assertThat(fm1.getTestBean()).isSameAs(tb);
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertThat(fm2).isSameAs(fm1);
		assertThat(fm2.getTestBean()).isSameAs(tb);
	}

	@Test
	void cannotSpecifyFactoryMethodArgumentsOnSingletonAfterCreation() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly");
		TestBean tb = fm1.getTestBean();
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertThat(fm2).isSameAs(fm1);
		assertThat(fm2.getTestBean()).isSameAs(tb);
	}

	@Test
	void factoryMethodWithDifferentReturnType() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// Check that listInstance is not considered a bean of type FactoryMethods.
		assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
		String[] names = xbf.getBeanNamesForType(FactoryMethods.class);
		assertThat(Arrays.asList(names)).doesNotContain("listInstance");
		names = xbf.getBeanNamesForType(List.class);
		assertThat(Arrays.asList(names)).contains("listInstance");

		xbf.preInstantiateSingletons();
		assertThat(List.class.isAssignableFrom(xbf.getType("listInstance"))).isTrue();
		names = xbf.getBeanNamesForType(FactoryMethods.class);
		assertThat(Arrays.asList(names)).doesNotContain("listInstance");
		names = xbf.getBeanNamesForType(List.class);
		assertThat(Arrays.asList(names)).contains("listInstance");
		List<?> list = (List<?>) xbf.getBean("listInstance");
		assertThat(list).isEqualTo(Collections.EMPTY_LIST);
	}

	@Test
	void factoryMethodForJavaMailSession() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		MailSession session = (MailSession) xbf.getBean("javaMailSession");
		assertThat(session.getProperty("mail.smtp.user")).isEqualTo("someuser");
		assertThat(session.getProperty("mail.smtp.password")).isEqualTo("somepw");
	}
}


class MailSession {

	private Properties props;

	private MailSession() {
	}

	void setProperties(Properties props) {
		this.props = props;
	}

	public static MailSession getDefaultInstance(Properties props) {
		MailSession session = new MailSession();
		session.setProperties(props);
		return session;
	}

	public Object getProperty(String key) {
		return this.props.get(key);
	}
}
