package org.springframework.beans.factory;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.junit.Test;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * SPR-5475 exposed the fact that the error message displayed when incorrectly
 * invoking a factory method is not instructive to the user and rather misleading.
 *
 * @author Chris Beams
 */
public class Spr5475Tests {

	@Test
	public void noArgFactoryMethodInvokedWithOneArg() {
		assertExceptionMessageForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("noArgFactory")
					.addConstructorArgValue("bogusArg").getBeanDefinition(),
				"Error creating bean with name 'foo': No matching factory method found: factory method 'noArgFactory(String)'. " +
				"Check that a method with the specified name and arguments exists and that it is static.");
	}

	@Test
	public void noArgFactoryMethodInvokedWithTwoArgs() {
		assertExceptionMessageForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("noArgFactory")
					.addConstructorArgValue("bogusArg1")
					.addConstructorArgValue("bogusArg2".getBytes()).getBeanDefinition(),
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

		assertExceptionMessageForMisconfiguredFactoryMethod(
				def,
				"Error creating bean with name 'foo': No matching factory method found: factory method 'noArgFactory(CharSequence,byte[])'. " +
				"Check that a method with the specified name and arguments exists and that it is static.");
	}

	private void assertExceptionMessageForMisconfiguredFactoryMethod(BeanDefinition bd, String expectedMessage) {
		DefaultListableBeanFactory factory = new DefaultListableBeanFactory();
		factory.registerBeanDefinition("foo", bd);

		try {
			factory.preInstantiateSingletons();
			fail("should have failed with BeanCreationException due to incorrectly invoked factory method");
		} catch (BeanCreationException ex) {
			assertThat(ex.getMessage(), equalTo(expectedMessage));
		}
	}

	@Test
	public void singleArgFactoryMethodInvokedWithNoArgs() {
		// calling a factory method that accepts arguments without any arguments emits an exception unlike cases
		// where a no-arg factory method is called with arguments. Adding this test just to document the difference
		assertExceptionMessageForMisconfiguredFactoryMethod(
				rootBeanDefinition(Foo.class)
					.setFactoryMethod("singleArgFactory").getBeanDefinition(),
				"Error creating bean with name 'foo': " +
				"Unsatisfied dependency expressed through constructor argument with index 0 of type [java.lang.String]: " +
				"Ambiguous factory method argument types - did you specify the correct bean references as factory method arguments?");
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
