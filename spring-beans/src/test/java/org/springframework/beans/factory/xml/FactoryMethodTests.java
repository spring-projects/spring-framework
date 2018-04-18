/*
 * Copyright 2002-2017 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class FactoryMethodTests {

	@Test
	public void testFactoryMethodsSingletonOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean");
		assertEquals("defaultInstance", tb.getName());
		assertEquals(1, tb.getAge());

		FactoryMethods fm = (FactoryMethods) xbf.getBean("default");
		assertEquals(0, fm.getNum());
		assertEquals("default", fm.getName());
		assertEquals("defaultInstance", fm.getTestBean().getName());
		assertEquals("setterString", fm.getStringValue());

		fm = (FactoryMethods) xbf.getBean("testBeanOnly");
		assertEquals(0, fm.getNum());
		assertEquals("default", fm.getName());
		// This comes from the test bean
		assertEquals("Juergen", fm.getTestBean().getName());

		fm = (FactoryMethods) xbf.getBean("full");
		assertEquals(27, fm.getNum());
		assertEquals("gotcha", fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("full");
		assertSame(fm, fm2);

		xbf.destroySingletons();
		assertTrue(tb.wasDestroyed());
	}

	@Test
	public void testFactoryMethodsWithInvalidDestroyMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		try {
			xbf.getBean("defaultTestBeanWithInvalidDestroyMethod");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
		}
	}

	@Test
	public void testFactoryMethodsWithNullInstance() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertEquals("null", xbf.getBean("null").toString());

		try {
			xbf.getBean("nullWithProperty");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			// expected
		}
	}

	@Test
	public void testFactoryMethodsWithNullValue() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithNull");
		assertEquals(27, fm.getNum());
		assertEquals(null, fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());

		fm = (FactoryMethods) xbf.getBean("fullWithGenericNull");
		assertEquals(27, fm.getNum());
		assertEquals(null, fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());

		fm = (FactoryMethods) xbf.getBean("fullWithNamedNull");
		assertEquals(27, fm.getNum());
		assertEquals(null, fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());
	}

	@Test
	public void testFactoryMethodsWithAutowire() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		FactoryMethods fm = (FactoryMethods) xbf.getBean("fullWithAutowire");
		assertEquals(27, fm.getNum());
		assertEquals("gotchaAutowired", fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());
	}

	@Test
	public void testProtectedFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.protected");
		assertEquals(1, tb.getAge());
	}

	@Test
	public void testPrivateFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		TestBean tb = (TestBean) xbf.getBean("defaultTestBean.private");
		assertEquals(1, tb.getAge());
	}

	@Test
	public void testFactoryMethodsPrototypeOnTargetClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		FactoryMethods fm = (FactoryMethods) xbf.getBean("defaultPrototype");
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("defaultPrototype");
		assertEquals(0, fm.getNum());
		assertEquals("default", fm.getName());
		assertEquals("defaultInstance", fm.getTestBean().getName());
		assertEquals("setterString", fm.getStringValue());
		assertEquals(fm.getNum(), fm2.getNum());
		assertEquals(fm.getStringValue(), fm2.getStringValue());
		// The TestBean is created separately for each bean
		assertNotSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);

		fm = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype");
		assertEquals(0, fm.getNum());
		assertEquals("default", fm.getName());
		// This comes from the test bean
		assertEquals("Juergen", fm.getTestBean().getName());
		assertEquals(fm.getNum(), fm2.getNum());
		assertEquals(fm.getStringValue(), fm2.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);

		fm = (FactoryMethods) xbf.getBean("fullPrototype");
		fm2 = (FactoryMethods) xbf.getBean("fullPrototype");
		assertEquals(27, fm.getNum());
		assertEquals("gotcha", fm.getName());
		assertEquals("Juergen", fm.getTestBean().getName());
		assertEquals(fm.getNum(), fm2.getNum());
		assertEquals(fm.getStringValue(), fm2.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm.getTestBean(), fm2.getTestBean());
		assertNotSame(fm, fm2);
	}

	/**
	 * Tests where the static factory method is on a different class.
	 */
	@Test
	public void testFactoryMethodsOnExternalClass() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		assertEquals(TestBean.class, xbf.getType("externalFactoryMethodWithoutArgs"));
		assertEquals(TestBean.class, xbf.getType("externalFactoryMethodWithArgs"));
		String[] names = xbf.getBeanNamesForType(TestBean.class);
		assertTrue(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs"));
		assertTrue(Arrays.asList(names).contains("externalFactoryMethodWithArgs"));

		TestBean tb = (TestBean) xbf.getBean("externalFactoryMethodWithoutArgs");
		assertEquals(2, tb.getAge());
		assertEquals("Tristan", tb.getName());
		tb = (TestBean) xbf.getBean("externalFactoryMethodWithArgs");
		assertEquals(33, tb.getAge());
		assertEquals("Rod", tb.getName());

		assertEquals(TestBean.class, xbf.getType("externalFactoryMethodWithoutArgs"));
		assertEquals(TestBean.class, xbf.getType("externalFactoryMethodWithArgs"));
		names = xbf.getBeanNamesForType(TestBean.class);
		assertTrue(Arrays.asList(names).contains("externalFactoryMethodWithoutArgs"));
		assertTrue(Arrays.asList(names).contains("externalFactoryMethodWithArgs"));
	}

	@Test
	public void testInstanceFactoryMethodWithoutArgs() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		InstanceFactory.count = 0;
		xbf.preInstantiateSingletons();
		assertEquals(1, InstanceFactory.count);
		FactoryMethods fm = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertEquals("instanceFactory", fm.getTestBean().getName());
		assertEquals(1, InstanceFactory.count);
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("instanceFactoryMethodWithoutArgs");
		assertEquals("instanceFactory", fm2.getTestBean().getName());
		assertSame(fm2, fm);
		assertEquals(1, InstanceFactory.count);
	}

	@Test
	public void testFactoryMethodNoMatchingStaticMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		try {
			xbf.getBean("noMatchPrototype");
			fail("No static method matched");
		}
		catch (BeanCreationException ex) {
			// Ok
		}
	}

	@Test
	public void testNonExistingFactoryMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		try {
			xbf.getBean("invalidPrototype");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("nonExisting(TestBean)"));
		}
	}

	@Test
	public void testFactoryMethodArgumentsForNonExistingMethod() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		try {
			xbf.getBean("invalidPrototype", new TestBean());
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getMessage().contains("nonExisting(TestBean)"));
		}
	}

	@Test
	public void testCanSpecifyFactoryMethodArgumentsOnFactoryMethodPrototype() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));
		TestBean tbArg = new TestBean();
		tbArg.setName("arg1");
		TestBean tbArg2 = new TestBean();
		tbArg2.setName("arg2");

		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertEquals(0, fm1.getNum());
		assertEquals("default", fm1.getName());
		// This comes from the test bean
		assertEquals("arg1", fm1.getTestBean().getName());

		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2);
		assertEquals("arg2", fm2.getTestBean().getName());
		assertEquals(fm1.getNum(), fm2.getNum());
		assertEquals(fm2.getStringValue(), "testBeanOnlyPrototypeDISetterString");
		assertEquals(fm2.getStringValue(), fm2.getStringValue());
		// The TestBean reference is resolved to a prototype in the factory
		assertSame(fm2.getTestBean(), fm2.getTestBean());
		assertNotSame(fm1, fm2);

		FactoryMethods fm3 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg2, new Integer(1), "myName");
		assertEquals(1, fm3.getNum());
		assertEquals("myName", fm3.getName());
		assertEquals("arg2", fm3.getTestBean().getName());

		FactoryMethods fm4 = (FactoryMethods) xbf.getBean("testBeanOnlyPrototype", tbArg);
		assertEquals(0, fm4.getNum());
		assertEquals("default", fm4.getName());
		assertEquals("arg1", fm4.getTestBean().getName());
	}

	@Test
	public void testCanSpecifyFactoryMethodArgumentsOnSingleton() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		TestBean tb = new TestBean();
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly", tb);
		assertSame(tb, fm1.getTestBean());
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertSame(fm1, fm2);
		assertSame(tb, fm2.getTestBean());
	}

	@Test
	public void testCannotSpecifyFactoryMethodArgumentsOnSingletonAfterCreation() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// First getBean call triggers actual creation of the singleton bean
		FactoryMethods fm1 = (FactoryMethods) xbf.getBean("testBeanOnly");
		TestBean tb = fm1.getTestBean();
		FactoryMethods fm2 = (FactoryMethods) xbf.getBean("testBeanOnly", new TestBean());
		assertSame(fm1, fm2);
		assertSame(tb, fm2.getTestBean());
	}

	@Test
	public void testFactoryMethodWithDifferentReturnType() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		// Check that listInstance is not considered a bean of type FactoryMethods.
		assertTrue(List.class.isAssignableFrom(xbf.getType("listInstance")));
		String[] names = xbf.getBeanNamesForType(FactoryMethods.class);
		assertTrue(!Arrays.asList(names).contains("listInstance"));
		names = xbf.getBeanNamesForType(List.class);
		assertTrue(Arrays.asList(names).contains("listInstance"));

		xbf.preInstantiateSingletons();
		assertTrue(List.class.isAssignableFrom(xbf.getType("listInstance")));
		names = xbf.getBeanNamesForType(FactoryMethods.class);
		assertTrue(!Arrays.asList(names).contains("listInstance"));
		names = xbf.getBeanNamesForType(List.class);
		assertTrue(Arrays.asList(names).contains("listInstance"));
		List<?> list = (List<?>) xbf.getBean("listInstance");
		assertEquals(Collections.EMPTY_LIST, list);
	}

	@Test
	public void testFactoryMethodForJavaMailSession() {
		DefaultListableBeanFactory xbf = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(xbf);
		reader.loadBeanDefinitions(new ClassPathResource("factory-methods.xml", getClass()));

		MailSession session = (MailSession) xbf.getBean("javaMailSession");
		assertEquals("someuser", session.getProperty("mail.smtp.user"));
		assertEquals("somepw", session.getProperty("mail.smtp.password"));
	}
}


class MailSession {

	private Properties props;

	private MailSession() {
	}

	public void setProperties(Properties props) {
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
