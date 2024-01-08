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

package org.springframework.beans.factory.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.beans.FatalBeanException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.NestedCheckedException;
import org.springframework.core.NestedRuntimeException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.Mockito.mock;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

/**
 * Tests for {@link ServiceLocatorFactoryBean}.
 *
 * @author Colin Sampaleanu
 * @author Rick Evans
 * @author Chris Beams
 */
class ServiceLocatorFactoryBeanTests {

	private DefaultListableBeanFactory bf;

	@BeforeEach
	void setUp() {
		bf = new DefaultListableBeanFactory();
	}

	@Test
	void testNoArgGetter() {
		bf.registerBeanDefinition("testService", genericBeanDefinition(TestService.class).getBeanDefinition());
		bf.registerBeanDefinition("factory",
				genericBeanDefinition(ServiceLocatorFactoryBean.class)
				.addPropertyValue("serviceLocatorInterface", TestServiceLocator.class)
				.getBeanDefinition());

		TestServiceLocator factory = (TestServiceLocator) bf.getBean("factory");
		TestService testService = factory.getTestService();
		assertThat(testService).isNotNull();
	}

	@Test
	void testErrorOnTooManyOrTooFew() {
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
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("more than one matching type").isThrownBy(() ->
			((TestServiceLocator) bf.getBean("factory")).getTestService());
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("more than one matching type").isThrownBy(() ->
			((TestServiceLocator2) bf.getBean("factory2")).getTestService(null));
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).as("no matching types").isThrownBy(() ->
			((TestService2Locator) bf.getBean("factory3")).getTestService());
	}

	@Test
	void testErrorOnTooManyOrTooFewWithCustomServiceLocatorException() {
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
		assertThatExceptionOfType(CustomServiceLocatorException1.class).as("more than one matching type").isThrownBy(() ->
				((TestServiceLocator) bf.getBean("factory")).getTestService())
			.withCauseInstanceOf(NoSuchBeanDefinitionException.class);
		assertThatExceptionOfType(CustomServiceLocatorException2.class).as("more than one matching type").isThrownBy(() ->
				((TestServiceLocator2) bf.getBean("factory2")).getTestService(null))
			.withCauseInstanceOf(NoSuchBeanDefinitionException.class);
		assertThatExceptionOfType(CustomServiceLocatorException3.class).as("no matching type").isThrownBy(() ->
				((TestService2Locator) bf.getBean("factory3")).getTestService());
	}

	@Test
	void testStringArgGetter() throws Exception {
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
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				factory.getTestService("bogusTestService"));
	}

	@Disabled @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
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
		assertThat(testBean2).isNotSameAs(testBean1);
		assertThat(testBean3).isNotSameAs(testBean1);
		assertThat(testBean4).isNotSameAs(testBean1);
		assertThat(testBean3).isNotSameAs(testBean2);
		assertThat(testBean4).isNotSameAs(testBean2);
		assertThat(testBean4).isNotSameAs(testBean3);

		assertThat(factory.toString()).contains("TestServiceLocator3");
	}

	@Disabled @Test // worked when using an ApplicationContext (see commented), fails when using BeanFactory
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
		assertThat(testBean2).isNotSameAs(testBean1);
		assertThat(testBean3).isNotSameAs(testBean1);
		assertThat(testBean4).isNotSameAs(testBean1);
		assertThat(testBean3).isNotSameAs(testBean2);
		assertThat(testBean4).isNotSameAs(testBean2);
		assertThat(testBean4).isNotSameAs(testBean3);
		assertThat(testBean1).isNotInstanceOf(ExtendedTestService.class);
		assertThat(testBean2).isNotInstanceOf(ExtendedTestService.class);
		assertThat(testBean3).isNotInstanceOf(ExtendedTestService.class);
		assertThat(testBean4).isInstanceOf(ExtendedTestService.class);
	}

	@Test
	void testNoServiceLocatorInterfaceSupplied() {
		assertThatIllegalArgumentException().isThrownBy(
				new ServiceLocatorFactoryBean()::afterPropertiesSet);
	}

	@Test
	void testWhenServiceLocatorInterfaceIsNotAnInterfaceType() {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorInterface(getClass());
		assertThatIllegalArgumentException().isThrownBy(
					factory::afterPropertiesSet);
		// should throw, bad (non-interface-type) serviceLocator interface supplied
	}

	@Test
	void testWhenServiceLocatorExceptionClassToExceptionTypeWithOnlyNoArgCtor() {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setServiceLocatorExceptionClass(ExceptionClassWithOnlyZeroArgCtor.class));
		// should throw, bad (invalid-Exception-type) serviceLocatorException class supplied
	}

	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void testWhenServiceLocatorExceptionClassIsNotAnExceptionSubclass() {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		assertThatIllegalArgumentException().isThrownBy(() ->
				factory.setServiceLocatorExceptionClass((Class) getClass()));
		// should throw, bad (non-Exception-type) serviceLocatorException class supplied
	}

	@Test
	void testWhenServiceLocatorMethodCalledWithTooManyParameters() {
		ServiceLocatorFactoryBean factory = new ServiceLocatorFactoryBean();
		factory.setServiceLocatorInterface(ServiceLocatorInterfaceWithExtraNonCompliantMethod.class);
		factory.afterPropertiesSet();
		ServiceLocatorInterfaceWithExtraNonCompliantMethod locator = (ServiceLocatorInterfaceWithExtraNonCompliantMethod) factory.getObject();
		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
				locator.getTestService("not", "allowed")); //bad method (too many args, doesn't obey class contract)
	}

	@Test
	void testRequiresListableBeanFactoryAndChokesOnAnythingElse() {
		BeanFactory beanFactory = mock();
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


	public interface TestServiceLocator {

		TestService getTestService();
	}


	public interface TestServiceLocator2 {

		TestService getTestService(String id) throws CustomServiceLocatorException2;
	}


	public interface TestServiceLocator3 {

		TestService getTestService();

		TestService getTestService(String id);

		TestService getTestService(int id);

		TestService someFactoryMethod();
	}


	public interface TestService2Locator {

		TestService2 getTestService() throws CustomServiceLocatorException3;
	}


	public interface ServiceLocatorInterfaceWithExtraNonCompliantMethod {

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
