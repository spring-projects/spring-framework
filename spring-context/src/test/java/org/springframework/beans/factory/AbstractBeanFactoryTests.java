/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.beans.factory;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

import junit.framework.TestCase;
import junit.framework.Assert;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * Subclasses must implement setUp() to initialize bean factory
 * and any other variables they need.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 */
public abstract class AbstractBeanFactoryTests extends TestCase {

	protected abstract BeanFactory getBeanFactory();

	/**
	 * Roderick beans inherits from rod, overriding name only.
	 */
	public void testInheritance() {
		assertTrue(getBeanFactory().containsBean("rod"));
		assertTrue(getBeanFactory().containsBean("roderick"));
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean roderick = (TestBean) getBeanFactory().getBean("roderick");
		assertTrue("not == ", rod != roderick);
		assertTrue("rod.name is Rod", rod.getName().equals("Rod"));
		assertTrue("rod.age is 31", rod.getAge() == 31);
		assertTrue("roderick.name is Roderick", roderick.getName().equals("Roderick"));
		assertTrue("roderick.age was inherited", roderick.getAge() == rod.getAge());
	}

	public void testGetBeanWithNullArg() {
		try {
			getBeanFactory().getBean((String) null);
			fail("Can't get null bean");
		}
		catch (IllegalArgumentException ex) {
			// OK
		}
	}

	/**
	 * Test that InitializingBean objects receive the afterPropertiesSet() callback
	 */
	public void testInitializingBeanCallback() {
		MustBeInitialized mbi = (MustBeInitialized) getBeanFactory().getBean("mustBeInitialized");
		// The dummy business method will throw an exception if the
		// afterPropertiesSet() callback wasn't invoked
		mbi.businessMethod();
	}

	/**
	 * Test that InitializingBean/BeanFactoryAware/DisposableBean objects receive the
	 * afterPropertiesSet() callback before BeanFactoryAware callbacks
	 */
	public void testLifecycleCallbacks() {
		LifecycleBean lb = (LifecycleBean) getBeanFactory().getBean("lifecycle");
		Assert.assertEquals("lifecycle", lb.getBeanName());
		// The dummy business method will throw an exception if the
		// necessary callbacks weren't invoked in the right order.
		lb.businessMethod();
		assertTrue("Not destroyed", !lb.isDestroyed());
	}

	public void testFindsValidInstance() {
		try {
			Object o = getBeanFactory().getBean("rod");
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
			TestBean rod = (TestBean) o;
			assertTrue("rod.name is Rod", rod.getName().equals("Rod"));
			assertTrue("rod.age is 31", rod.getAge() == 31);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testGetInstanceByMatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", TestBean.class);
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance with matching class");
		}
	}

	public void testGetInstanceByNonmatchingClass() {
		try {
			getBeanFactory().getBean("rod", BeanFactory.class);
			fail("Rod bean is not of type BeanFactory; getBeanInstance(rod, BeanFactory.class) should throw BeanNotOfRequiredTypeException");
		}
		catch (BeanNotOfRequiredTypeException ex) {
			// So far, so good
			assertTrue("Exception has correct bean name", ex.getBeanName().equals("rod"));
			assertTrue("Exception requiredType must be BeanFactory.class", ex.getRequiredType().equals(BeanFactory.class));
			assertTrue("Exception actualType as TestBean.class", TestBean.class.isAssignableFrom(ex.getActualType()));
			assertTrue("Actual type is correct", ex.getActualType() == getBeanFactory().getBean("rod").getClass());
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testGetSharedInstanceByMatchingClass() {
		try {
			Object o = getBeanFactory().getBean("rod", TestBean.class);
			assertTrue("Rod bean is a TestBean", o instanceof TestBean);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance with matching class");
		}
	}

	public void testGetSharedInstanceByMatchingClassNoCatch() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
	}

	public void testGetSharedInstanceByNonmatchingClass() {
		try {
			getBeanFactory().getBean("rod", BeanFactory.class);
			fail("Rod bean is not of type BeanFactory; getBeanInstance(rod, BeanFactory.class) should throw BeanNotOfRequiredTypeException");
		}
		catch (BeanNotOfRequiredTypeException ex) {
			// So far, so good
			assertTrue("Exception has correct bean name", ex.getBeanName().equals("rod"));
			assertTrue("Exception requiredType must be BeanFactory.class", ex.getRequiredType().equals(BeanFactory.class));
			assertTrue("Exception actualType as TestBean.class", TestBean.class.isAssignableFrom(ex.getActualType()));
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testSharedInstancesAreEqual() {
		try {
			Object o = getBeanFactory().getBean("rod");
			assertTrue("Rod bean1 is a TestBean", o instanceof TestBean);
			Object o1 = getBeanFactory().getBean("rod");
			assertTrue("Rod bean2 is a TestBean", o1 instanceof TestBean);
			assertTrue("Object equals applies", o == o1);
		}
		catch (Exception ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testPrototypeInstancesAreIndependent() {
		TestBean tb1 = (TestBean) getBeanFactory().getBean("kathy");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("kathy");
		assertTrue("ref equal DOES NOT apply", tb1 != tb2);
		assertTrue("object equal true", tb1.equals(tb2));
		tb1.setAge(1);
		tb2.setAge(2);
		assertTrue("1 age independent = 1", tb1.getAge() == 1);
		assertTrue("2 age independent = 2", tb2.getAge() == 2);
		assertTrue("object equal now false", !tb1.equals(tb2));
	}

	public void testNotThere() {
		assertFalse(getBeanFactory().containsBean("Mr Squiggle"));
		try {
			getBeanFactory().getBean("Mr Squiggle");
			fail("Can't find missing bean");
		}
		catch (BeansException ex) {
			//ex.printStackTrace();
			//fail("Shouldn't throw exception on getting valid instance");
		}
	}

	public void testValidEmpty() {
		try {
			Object o = getBeanFactory().getBean("validEmpty");
			assertTrue("validEmpty bean is a TestBean", o instanceof TestBean);
			TestBean ve = (TestBean) o;
			assertTrue("Valid empty has defaults", ve.getName() == null && ve.getAge() == 0 && ve.getSpouse() == null);
		}
		catch (BeansException ex) {
			ex.printStackTrace();
			fail("Shouldn't throw exception on valid empty");
		}
	}

	public void xtestTypeMismatch() {
		try {
			getBeanFactory().getBean("typeMismatch");
			fail("Shouldn't succeed with type mismatch");
		}
		catch (BeanCreationException wex) {
			assertEquals("typeMismatch", wex.getBeanName());
			assertTrue(wex.getCause() instanceof PropertyBatchUpdateException);
			PropertyBatchUpdateException ex = (PropertyBatchUpdateException) wex.getCause();
			// Further tests
			assertTrue("Has one error ", ex.getExceptionCount() == 1);
			assertTrue("Error is for field age", ex.getPropertyAccessException("age") != null);
			assertTrue("We have rejected age in exception", ex.getPropertyAccessException("age").getPropertyChangeEvent().getNewValue().equals("34x"));
		}
	}

	public void testGrandparentDefinitionFoundInBeanFactory() throws Exception {
		TestBean dad = (TestBean) getBeanFactory().getBean("father");
		assertTrue("Dad has correct name", dad.getName().equals("Albert"));
	}

	public void testFactorySingleton() throws Exception {
		assertTrue(getBeanFactory().isSingleton("&singletonFactory"));
		assertTrue(getBeanFactory().isSingleton("singletonFactory"));
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertTrue("Singleton from factory has correct name, not " + tb.getName(), tb.getName().equals(DummyFactory.SINGLETON_NAME));
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertTrue("Singleton references ==", tb == tb2);
		assertTrue("FactoryBean is BeanFactoryAware", factory.getBeanFactory() != null);
	}

	public void testFactoryPrototype() throws Exception {
		assertTrue(getBeanFactory().isSingleton("&prototypeFactory"));
		assertFalse(getBeanFactory().isSingleton("prototypeFactory"));
		TestBean tb = (TestBean) getBeanFactory().getBean("prototypeFactory");
		assertTrue(!tb.getName().equals(DummyFactory.SINGLETON_NAME));
		TestBean tb2 = (TestBean) getBeanFactory().getBean("prototypeFactory");
		assertTrue("Prototype references !=", tb != tb2);
	}

	/**
	 * Check that we can get the factory bean itself.
	 * This is only possible if we're dealing with a factory
	 * @throws Exception
	 */
	public void testGetFactoryItself() throws Exception {
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		assertTrue(factory != null);
	}

	/**
	 * Check that afterPropertiesSet gets called on factory
	 * @throws Exception
	 */
	public void testFactoryIsInitialized() throws Exception {
		getBeanFactory().getBean("singletonFactory");
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		assertTrue("Factory was initialized because it implemented InitializingBean", factory.wasInitialized());
	}

	/**
	 * It should be illegal to dereference a normal bean
	 * as a factory
	 */
	public void testRejectsFactoryGetOnNormalBean() {
		try {
			getBeanFactory().getBean("&rod");
			fail("Shouldn't permit factory get on normal bean");
		}
		catch (BeanIsNotAFactoryException ex) {
			// Ok
		}
	}

	// TODO: refactor in AbstractBeanFactory (tests for AbstractBeanFactory)
	// and rename this class
	public void testAliasing() {
		BeanFactory bf = getBeanFactory();
		if (!(bf instanceof ConfigurableBeanFactory)) {
			return;
		}
		ConfigurableBeanFactory cbf = (ConfigurableBeanFactory) bf;

		String alias = "rods alias";
		try {
			cbf.getBean(alias);
			fail("Shouldn't permit factory get on normal bean");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// Ok
			assertTrue(alias.equals(ex.getBeanName()));
		}

		// Create alias
		cbf.registerAlias("rod", alias);
		Object rod = getBeanFactory().getBean("rod");
		Object aliasRod = getBeanFactory().getBean(alias);
		assertTrue(rod == aliasRod);
	}


	public static class TestBeanEditor extends PropertyEditorSupport {

		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}

}
