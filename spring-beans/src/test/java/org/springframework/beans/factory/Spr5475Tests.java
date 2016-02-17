package org.springframework.beans.factory;

import org.junit.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.*;

/**
 * SPR-5475 exposed the fact that the error message displayed when incorrectly
 * invoking a factory method is not instructive to the user and rather misleading.
 *
 * @author Chris Beams
 */
public class Spr5475Tests {

	@Test
	public void noArgFactoryMethodInvokedWithOneArg() {
		assertExceptionForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("noArgFactory")
					.addConstructorArgValue("bogusArg").getBeanDefinition(),
					BeanCreationException.class,
				"Error creating bean with name 'foo': No matching factory method found: factory method 'noArgFactory(String)'. " +
				"Check that a method with the specified name and arguments exists and that it is static.");
	}

	@Test
	public void noArgFactoryMethodInvokedWithTwoArgs() {
		assertExceptionForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("noArgFactory")
					.addConstructorArgValue("bogusArg1")
					.addConstructorArgValue("bogusArg2".getBytes()).getBeanDefinition(),
					BeanCreationException.class,
				"Error creating bean with name 'foo': No matching factory method found: factory method 'noArgFactory(String,byte[])'. " +
				"Check that a method with the specified name and arguments exists and that it is static.");
	}

	@Test
	public void noArgFactoryMethodInvokedWithTwoArgsAndTypesSpecified() {
		RootBeanDefinition def = new RootBeanDefinition(Foo.class);
		def.setFactoryMethodName("noArgFactory");
		ConstructorArgumentValues cav = new ConstructorArgumentValues();
		cav.addIndexedArgumentValue(0, "bogusArg1", CharSequence.class.getName());
		cav.addIndexedArgumentValue(1, "bogusArg2".getBytes());
		def.setConstructorArgumentValues(cav);

		assertExceptionForMisconfiguredFactoryMethod(
				def, BeanCreationException.class,
				"Error creating bean with name 'foo': No matching factory method found: factory method 'noArgFactory(CharSequence,byte[])'. " +
				"Check that a method with the specified name and arguments exists and that it is static.");
	}

	private void assertExceptionForMisconfiguredFactoryMethod(BeanDefinition bd, Class<? extends Throwable> expectedType, String expectedMessage) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("foo", bd);

		try {
			factory.preInstantiateSingletons();
			fail("should have failed with BeanCreationException due to incorrectly invoked factory method");
		} catch (BeanCreationException ex) {
			assertThat(ex.getMessage(), equalTo(expectedMessage));
			assertThat(ex, instanceOf(expectedType));
		}
	}

	@Test
	public void singleArgFactoryMethodInvokedWithNoArgs() {
		// calling a factory method that accepts arguments without any arguments emits an exception unlike cases
		// where a no-arg factory method is called with arguments. Adding this test just to document the difference
		assertExceptionForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("singleArgFactory").getBeanDefinition(),
					UnsatisfiedFactoryMethodDependencyException.class,
				"Error creating bean with name 'foo': " +
				"Unsatisfied dependency expressed through argument with index 0 of type [java.lang.String]: " +
				"Ambiguous argument types - did you specify the correct bean references as arguments?");
	}


	static class Foo {
		static Foo noArgFactory() {
			return new Foo();
		}

		static Foo singleArgFactory(String arg) {
			return new Foo();
		}
	}

}
