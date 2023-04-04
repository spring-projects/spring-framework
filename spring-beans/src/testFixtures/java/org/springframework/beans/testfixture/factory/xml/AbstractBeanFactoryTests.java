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

package org.springframework.beans.testfixture.factory.xml;

import java.beans.PropertyEditorSupport;
import java.util.StringTokenizer;

import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanIsNotAFactoryException;
import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.testfixture.beans.LifecycleBean;
import org.springframework.beans.testfixture.beans.MustBeInitialized;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
		assertThat(getBeanFactory().containsBean("rod")).isTrue();
		assertThat(getBeanFactory().containsBean("roderick")).isTrue();
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean roderick = (TestBean) getBeanFactory().getBean("roderick");
		assertThat(rod).as("not == ").isNotSameAs(roderick);
		assertThat(rod.getName().equals("Rod")).as("rod.name is Rod").isTrue();
		assertThat(rod.getAge()).as("rod.age is 31").isEqualTo(31);
		assertThat(roderick.getName().equals("Roderick")).as("roderick.name is Roderick").isTrue();
		assertThat(roderick.getAge()).as("roderick.age was inherited").isEqualTo(rod.getAge());
	}

	@Test
	public void getBeanWithNullArg() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				getBeanFactory().getBean((String) null));
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
		assertThat(lb.getBeanName()).isEqualTo("lifecycle");
		// The dummy business method will throw an exception if the
		// necessary callbacks weren't invoked in the right order.
		lb.businessMethod();
		boolean condition = !lb.isDestroyed();
		assertThat(condition).as("Not destroyed").isTrue();
	}

	@Test
	public void findsValidInstance() {
		Object o = getBeanFactory().getBean("rod");
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
		TestBean rod = (TestBean) o;
		assertThat(rod.getName().equals("Rod")).as("rod.name is Rod").isTrue();
		assertThat(rod.getAge()).as("rod.age is 31").isEqualTo(31);
	}

	@Test
	public void getInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getInstanceByNonmatchingClass() {
		assertThatExceptionOfType(BeanNotOfRequiredTypeException.class).isThrownBy(() ->
				getBeanFactory().getBean("rod", BeanFactory.class))
			.satisfies(ex -> {
				assertThat(ex.getBeanName()).isEqualTo("rod");
				assertThat(ex.getRequiredType()).isEqualTo(BeanFactory.class);
				assertThat(ex.getActualType()).isEqualTo(TestBean.class).isEqualTo(getBeanFactory().getBean("rod").getClass());
			});
	}

	@Test
	public void getSharedInstanceByMatchingClass() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getSharedInstanceByMatchingClassNoCatch() {
		Object o = getBeanFactory().getBean("rod", TestBean.class);
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("Rod bean is a TestBean").isTrue();
	}

	@Test
	public void getSharedInstanceByNonmatchingClass() {
		assertThatExceptionOfType(BeanNotOfRequiredTypeException.class).isThrownBy(() ->
				getBeanFactory().getBean("rod", BeanFactory.class))
			.satisfies(ex -> {
				assertThat(ex.getBeanName()).isEqualTo("rod");
				assertThat(ex.getRequiredType()).isEqualTo(BeanFactory.class);
				assertThat(ex.getActualType()).isEqualTo(TestBean.class);
			});
	}

	@Test
	public void sharedInstancesAreEqual() {
		Object o = getBeanFactory().getBean("rod");
		boolean condition1 = o instanceof TestBean;
		assertThat(condition1).as("Rod bean1 is a TestBean").isTrue();
		Object o1 = getBeanFactory().getBean("rod");
		boolean condition = o1 instanceof TestBean;
		assertThat(condition).as("Rod bean2 is a TestBean").isTrue();
		assertThat(o).as("Object equals applies").isSameAs(o1);
	}

	@Test
	public void prototypeInstancesAreIndependent() {
		TestBean tb1 = (TestBean) getBeanFactory().getBean("kathy");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("kathy");
		assertThat(tb1).as("ref equal DOES NOT apply").isNotSameAs(tb2);
		assertThat(tb1.equals(tb2)).as("object equal true").isTrue();
		tb1.setAge(1);
		tb2.setAge(2);
		assertThat(tb1.getAge()).as("1 age independent = 1").isEqualTo(1);
		assertThat(tb2.getAge()).as("2 age independent = 2").isEqualTo(2);
		boolean condition = !tb1.equals(tb2);
		assertThat(condition).as("object equal now false").isTrue();
	}

	@Test
	public void notThere() {
		assertThat(getBeanFactory().containsBean("Mr Squiggle")).isFalse();
		assertThatExceptionOfType(BeansException.class).isThrownBy(() ->
				getBeanFactory().getBean("Mr Squiggle"));
	}

	@Test
	public void validEmpty() {
		Object o = getBeanFactory().getBean("validEmpty");
		boolean condition = o instanceof TestBean;
		assertThat(condition).as("validEmpty bean is a TestBean").isTrue();
		TestBean ve = (TestBean) o;
		assertThat(ve.getName() == null && ve.getAge() == 0 && ve.getSpouse() == null).as("Valid empty has defaults").isTrue();
	}

	@Test
	public void typeMismatch() {
		assertThatExceptionOfType(BeanCreationException.class)
			.isThrownBy(() -> getBeanFactory().getBean("typeMismatch"))
			.withCauseInstanceOf(TypeMismatchException.class);
	}

	@Test
	public void grandparentDefinitionFoundInBeanFactory() throws Exception {
		TestBean dad = (TestBean) getBeanFactory().getBean("father");
		assertThat(dad.getName().equals("Albert")).as("Dad has correct name").isTrue();
	}

	@Test
	public void factorySingleton() throws Exception {
		assertThat(getBeanFactory().isSingleton("&singletonFactory")).isTrue();
		assertThat(getBeanFactory().isSingleton("singletonFactory")).isTrue();
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb.getName().equals(DummyFactory.SINGLETON_NAME)).as("Singleton from factory has correct name, not " + tb.getName()).isTrue();
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean tb2 = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb).as("Singleton references ==").isSameAs(tb2);
		assertThat(factory.getBeanFactory()).as("FactoryBean is BeanFactoryAware").isNotNull();
	}

	@Test
	public void factoryPrototype() throws Exception {
		assertThat(getBeanFactory().isSingleton("&prototypeFactory")).isTrue();
		assertThat(getBeanFactory().isSingleton("prototypeFactory")).isFalse();
		TestBean tb = (TestBean) getBeanFactory().getBean("prototypeFactory");
		boolean condition = !tb.getName().equals(DummyFactory.SINGLETON_NAME);
		assertThat(condition).isTrue();
		TestBean tb2 = (TestBean) getBeanFactory().getBean("prototypeFactory");
		assertThat(tb).as("Prototype references !=").isNotSameAs(tb2);
	}

	/**
	 * Check that we can get the factory bean itself.
	 * This is only possible if we're dealing with a factory
	 */
	@Test
	public void getFactoryItself() throws Exception {
		assertThat(getBeanFactory().getBean("&singletonFactory")).isNotNull();
	}

	/**
	 * Check that afterPropertiesSet gets called on factory
	 */
	@Test
	public void factoryIsInitialized() throws Exception {
		TestBean tb = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(tb).isNotNull();
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		assertThat(factory.wasInitialized()).as("Factory was initialized because it implemented InitializingBean").isTrue();
	}

	/**
	 * It should be illegal to dereference a normal bean as a factory.
	 */
	@Test
	public void rejectsFactoryGetOnNormalBean() {
		assertThatExceptionOfType(BeanIsNotAFactoryException.class).isThrownBy(() ->
				getBeanFactory().getBean("&rod"));
	}

	// TODO: refactor in AbstractBeanFactory (tests for AbstractBeanFactory)
	// and rename this class
	@Test
	public void aliasing() {
		BeanFactory bf = getBeanFactory();
		if (!(bf instanceof ConfigurableBeanFactory cbf)) {
			return;
		}
		String alias = "rods alias";

		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				cbf.getBean(alias))
			.satisfies(ex -> assertThat(ex.getBeanName()).isEqualTo(alias));

		// Create alias
		cbf.registerAlias("rod", alias);
		Object rod = getBeanFactory().getBean("rod");
		Object aliasRod = getBeanFactory().getBean(alias);
		assertThat(rod).isSameAs(aliasRod);
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
