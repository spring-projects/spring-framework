/*
 * Copyright 2002-2015 the original author or authors.
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

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.PropertyBatchUpdateException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.tests.sample.beans.LifecycleBean;
import org.springframework.tests.sample.beans.MustBeInitialized;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.factory.DummyFactory;

import static org.junit.Assert.*;

/**
 * Subclasses must initialize the bean factory and any other variables they need.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Sam Brannen
 */
public abstract class AbstractBeanFactoryTests {

	protected abstract BeanFactory getBeanFactory();

	/**
	 * Roderick bean inherits from rod, overriding name only.
	 */
	@Test
	public void inheritance() {
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

	@Test(expected = IllegalArgumentException.class)
	public void getBeanWithNullArg() {
		getBeanFactory().getBean((String) null);
	}

	/**
	 * Test that InitializingBean objects receive the afterPropertiesSet() callback
	 */
	@Test
	public void initializingBeanCallback() {
		MustBeInitialized mbi = (MustBeInitialized) getBeanFactory().getBean("mustBeInitialized");
		// The dummy business method will throw an exception if the
		// afterPropertiesSet() callback wasn't invoked
		mbi.businessMethod();
	}

	/**
	 * Test that InitializingBean/BeanFactoryAware/DisposableBean objects receive the
	 * afterPropertiesSet() callback before BeanFactoryAware callbacks
	 */
	@Test
	public void lifecycleCallbacks() {
		LifecycleBean lb = (LifecycleBean) getBeanFactory().getBean("lifecycle");
		assertEquals("lifecycle", lb.getBeanName());
		// The dummy business method will throw an exception if the
		// necessary callbacks weren't invoked in the right order.
		lb.businessMethod();
		assertTrue("Not destroyed", !lb.isDestroyed());
	}

	@Test
	public void findsValidInstance() {
		Object o = getBeanFactory().getBean("rod");
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
		TestBean rod = (TestBean) o;
		assertTrue("rod.name is Rod", rod.getName().equals("Rod"));
		assertTrue("rod.age is 31", rod.getAge() == 31);
	}

	@Test
	public void getInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
	}

	@Test
	public void getInstanceByNonmatchingClass() {
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
	}

	@Test
	public void getSharedInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
	}

	@Test
	public void getSharedInstanceByMatchingClassNoCatch() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		assertTrue("Rod bean is a TestBean", o instanceof TestBean);
	}

	@Test
	public void getSharedInstanceByNonmatchingClass() {
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
	}

	@Test
	public void sharedInstancesAreEqual() {
		Object o = getBeanFactory().getBean("rod");
		assertTrue("Rod bean1 is a TestBean", o instanceof TestBean);
		Object o1 = getBeanFactory().getBean("rod");
		assertTrue("Rod bean2 is a TestBean", o1 instanceof TestBean);
		assertTrue("Object equals applies", o == o1);
	}

	@Test
	public void prototypeInstancesAreIndependent() {
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

	@Test(expected = BeansException.class)
	public void notThere() {
		assertFalse(getBeanFactory().containsBean("Mr Squiggle"));
		getBeanFactory().getBean("Mr Squiggle");
	}

	@Test
	public void validEmpty() {
		Object o = getBeanFactory().getBean("validEmpty");
		assertTrue("validEmpty bean is a TestBean", o instanceof TestBean);
		TestBean ve = (TestBean) o;
		assertTrue("Valid empty has defaults", ve.getName() == null && ve.getAge() == 0 && ve.getSpouse() == null);
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

	@Test
	public void grandparentDefinitionFoundInBeanFactory() throws Exception {
		TestBean dad = (TestBean) getBeanFactory().getBean("father");
		assertTrue("Dad has correct name", dad.getName().equals("Albert"));
	}

	@Test
	public void factorySingleton() throws Exception {
		assertTrue(getBeanFactory().isSingleton("&singletonFactory"));
		assertTrue(getBeanFactory().isSingleton("singletonFactory"));
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertTrue("Singleton from factory has correct name, not " + tb.getName(), tb.getName().equals(DummyFactory.SINGLETON_NAME));
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertTrue("Singleton references ==", tb == tb2);
		assertTrue("FactoryBean is BeanFactoryAware", factory.getBeanFactory() != null);
	}

	@Test
	public void factoryPrototype() throws Exception {
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
	@Test
	public void getFactoryItself() throws Exception {
		assertNotNull(getBeanFactory().getBean("&singletonFactory"));
	}

	/**
	 * Check that afterPropertiesSet gets called on factory
	 * @throws Exception
	 */
	@Test
	public void factoryIsInitialized() throws Exception {
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertNotNull(tb);
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		assertTrue("Factory was initialized because it implemented InitializingBean", factory.wasInitialized());
	}

	/**
	 * It should be illegal to dereference a normal bean as a factory.
	 */
	@Test(expected = BeanIsNotAFactoryException.class)
	public void rejectsFactoryGetOnNormalBean() {
		getBeanFactory().getBean("&rod");
	}

	// TODO: refactor in AbstractBeanFactory (tests for AbstractBeanFactory)
	// and rename this class
	@Test
	public void aliasing() {
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

		@Override
		public void setAsText(String text) {
			TestBean tb = new TestBean();
			StringTokenizer st = new StringTokenizer(text, "_");
			tb.setName(st.nextToken());
			tb.setAge(Integer.parseInt(st.nextToken()));
			setValue(tb);
		}
	}

}
