/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.NestedCheckedException;
import org.springframework.core.NestedRuntimeException;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

/**
 * Unit tests for {@link ServiceLocatorFactoryBean}.
 *
 * @author Colin Sampaleanu
 * @author Rick Evans
 * @author Chris Beams
 */
public class ServiceLocatorFactoryBeanTests {

	private DefaultListableBeanFactory bf;

	@Before
	public void setUp() {
		bf = new DefaultListableBeanFactory();
	}

	@Test
	public void testNoArgGetter() {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
				.getBeanDefinition());

		TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
		TestService testService = factory.getTestService();
		assertNotNull(testService);
	}

	@Test
	public void testErrorOnTooManyOrTooFew() throws Exception {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("testServiceInstance2", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
				.getBeanDefinition());
		bf.registerBeanDefinition("factory2",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
				.getBeanDefinition());
		bf.registerBeanDefinition("factory3",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestService2Locator.class)
				.getBeanDefinition());

		try {
			TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
			factory.getTestService();
			fail("Must fail on more than one matching type");
		}
		catch (NoSuchBeanDefinitionException ex) { /* expected */ }

		try {
			TestServiceLocator2 factory = (TestServiceLocator2) bf.getBean("factory2");
			factory.getTestService(null);
			fail("Must fail on more than one matching type");
		}
		catch (NoSuchBeanDefinitionException ex) { /* expected */ }

		try {
			TestService2Locator factory = (TestService2Locator) bf.getBean("factory3");
			factory.getTestService();
			fail("Must fail on no matching types");
		}
		catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Test
	public void testErrorOnTooManyOrTooFewWithCustomServiceLocatorException() {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("testServiceInstance2", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
				.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException1.class)
				.getBeanDefinition());
		bf.registerBeanDefinition("factory2",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
				.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException2.class)
				.getBeanDefinition());
		bf.registerBeanDefinition("factory3",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestService2Locator.class)
				.addPropertyValue("serviceLocatorExceptionClass", CustomServiceLocatorException3.class)
				.getBeanDefinition());

		try {
			TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
			factory.getTestService();
			fail("Must fail on more than one matching type");
		}
		catch (CustomServiceLocatorException1 expected) {
			assertTrue(expected.getCause() instanceof NoSuchBeanDefinitionException);
		}

		try {
			TestServiceLocator2 factory2 = (TestServiceLocator2) bf.getBean("factory2");
			factory2.getTestService(null);
			fail("Must fail on more than one matching type");
		}
		catch (CustomServiceLocatorException2 expected) {
			assertTrue(expected.getCause() instanceof NoSuchBeanDefinitionException);
		}

		try {
			TestService2Locator factory3 = (TestService2Locator) bf.getBean("factory3");
			factory3.getTestService();
			fail("Must fail on no matching type");
		}
		catch (CustomServiceLocatorException3 ex) { /* expected */ }
	}

	@Test
	public void testStringArgGetter() throws Exception {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator2.class)
				.getBeanDefinition());

		// test string-arg getter with null id
		TestServiceLocator2 factory = (TestServiceLocator2) bf.getBean("factory");

		@SuppressWarnings("unused")
		TestService testBean = factory.getTestService(null);
		// now test with explicit id
		testBean = factory.getTestService("testService");
		// now verify failure on bad id
		try {
			factory.getTestService("bogusTestService");
			fail("Illegal operation allowed");
		}
		catch (NoSuchBeanDefinitionException ex) { /* expected */ }
	}

	@Ignore @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
	public void testCombinedLocatorInterface() {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerAlias("testService", "1");

		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class)
				.getBeanDefinition());

//		StaticApplicationContext ctx = new StaticApplicationContext();
//		ctx.registerPrototype("testService", TestService.class, new MutablePropertyValues());
//		ctx.registerAlias("testService", "1");
//		MutablePropertyValues mpv = new MutablePropertyValues();
//		mpv.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);
//		ctx.registerSingleton("factory", ServiceLocatorFactoryBean.class, mpv);
//		ctx.refresh();

		TestServiceLocator3 factory = (TestServiceLocator3) bf.getBean("factory");
		TestService testBean1 = factory.getTestService();
		TestService testBean2 = factory.getTestService("testService");
		TestService testBean3 = factory.getTestService(1);
		TestService testBean4 = factory.someFactoryMethod();
		assertNotSame(testBean1, testBean2);
		assertNotSame(testBean1, testBean3);
		assertNotSame(testBean1, testBean4);
		assertNotSame(testBean2, testBean3);
		assertNotSame(testBean2, testBean4);
		assertNotSame(testBean3, testBean4);

		assertTrue(factory.toString().contains("TestServiceLocator3"));
	}

	@Ignore @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
	public void testServiceMappings() {
		bf.registerBeanDefinition("testService1", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("testService2", genericBeanDefinition(ExtendedTestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class)
				.addPropertyValue("serviceMappings", "=testService1\n1=testService1\n2=testService2")
				.getBeanDefinition());

//		StaticApplicationContext ctx = new StaticApplicationContext();
//		ctx.registerPrototype("testService1", TestService.class, new MutablePropertyValues());
//		ctx.registerPrototype("testService2", ExtendedTestService.class, new MutablePropertyValues());
//		MutablePropertyValues mpv = new MutablePropertyValues();
//		mpv.addPropertyValue("serviceLocatorInterface", TestServiceLocator3.class);
//		mpv.addPropertyValue("serviceMappings", "=testService1\n1=testService1\n2=testService2");
//		ctx.registerSingleton("factory", ServiceLocatorFactoryBean.class, mpv);
//		ctx.refresh();

		TestServiceLocator3 factory = (TestServiceLocator3) bf.getBean("factory");
		TestService testBean1 = factory.getTestService();
		TestService testBean2 = factory.getTestService("testService1");
		TestService testBean3 = factory.getTestService(1);
		TestService testBean4 = factory.getTestService(2);
		assertNotSame(testBean1, testBean2);
		assertNotSame(testBean1, testBean3);
		assertNotSame(testBean1, testBean4);
		assertNotSame(testBean2, testBean3);
		assertNotSame(testBean2, testBean4);
		assertNotSame(testBean3, testBean4);
		assertFalse(testBean1 instanceof ExtendedTestService);
		assertFalse(testBean2 instanceof ExtendedTestService);
		assertFalse(testBean3 instanceof ExtendedTestService);
		assertTrue(testBean4 instanceof ExtendedTestService);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testNoServiceLocatorInterfaceSupplied() throws Exception {
		new ServiceLocatorFactoryBean().afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWhenServiceLocatorInterfaceIsNotAnInterfaceType() throws Exception {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorInterface(getClass());
		factory.afterPropertiesSet();
		// should throw, bad (non-interface-type) serviceLocator interface supplied
	}

	@Test(expected = IllegalArgumentException.class)
	public void testWhenServiceLocatorExceptionClassToExceptionTypeWithOnlyNoArgCtor() throws Exception {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorExceptionClass(ExceptionClassWithOnlyZeroArgCtor.class);
		// should throw, bad (invalid-Exception-type) serviceLocatorException class supplied
	}

	@Test(expected = IllegalArgumentException.class)
	@SuppressWarnings("unchecked")
	public void testWhenServiceLocatorExceptionClassIsNotAnExceptionSubclass() throws Exception {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorExceptionClass((Class) getClass());
		// should throw, bad (non-Exception-type) serviceLocatorException class supplied
	}

	@Test(expected = UnsupportedOperationException.class)
	public void testWhenServiceLocatorMethodCalledWithTooManyParameters() throws Exception {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorInterface(ServiceLocatorInterfaceWithExtraNonCompliantMethod.class);
		factory.afterPropertiesSet();
		ServiceLocatorInterfaceWithExtraNonCompliantMethod locator = (ServiceLocatorInterfaceWithExtraNonCompliantMethod) factory.getObject();
		locator.getTestService("not", "allowed"); //bad method (too many args, doesn't obey class contract)
	}

	@Test
	public void testRequiresListableBeanFactoryAndChokesOnAnythingElse() throws Exception {
		BeanFactory beanFactory = mock(BeanFactory.class);
		try {
			ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
			factory.setBeanFactory(beanFactory);
		}
		catch (FatalBeanException ex) {
			// expected
		}
	}


	public static class TestService {

	}


	public static class ExtendedTestService extends TestService {

	}


	public static class TestService2 {

	}


	public static interface TestServiceLocator {

		TestService getTestService();
	}


	public static interface TestServiceLocator2 {

		TestService getTestService(String id) throws CustomServiceLocatorException2;
	}


	public static interface TestServiceLocator3 {

		TestService getTestService();

		TestService getTestService(String id);

		TestService getTestService(int id);

		TestService someFactoryMethod();
	}


	public static interface TestService2Locator {

		TestService2 getTestService() throws CustomServiceLocatorException3;
	}


	public static interface ServiceLocatorInterfaceWithExtraNonCompliantMethod {

		TestService2 getTestService();

		TestService2 getTestService(String serviceName, String defaultNotAllowedParameter);
	}


	@SuppressWarnings("serial")
	public static class CustomServiceLocatorException1 extends NestedRuntimeException {

		public CustomServiceLocatorException1(String message, Throwable cause) {
			super(message, cause);
		}
	}


	@SuppressWarnings("serial")
	public static class CustomServiceLocatorException2 extends NestedCheckedException {

		public CustomServiceLocatorException2(Throwable cause) {
			super("", cause);
		}
	}


	@SuppressWarnings("serial")
	public static class CustomServiceLocatorException3 extends NestedCheckedException {

		public CustomServiceLocatorException3(String message) {
			super(message);
		}
	}


	@SuppressWarnings("serial")
	public static class ExceptionClassWithOnlyZeroArgCtor extends Exception {

	}

}
