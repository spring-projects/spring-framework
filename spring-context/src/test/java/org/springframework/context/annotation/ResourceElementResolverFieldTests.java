/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanNotOfRequiredTypeException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.NoUniqueBeanDefinitionException;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link ResourceElementResolver} with fields.
 *
 * @author Stephane Nicoll
 */
class ResourceElementResolverFieldTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@Test
	void resolveWhenFieldIsMissingThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> ResourceElementResolver.forField("missing").resolve(registeredBean))
				.withMessage("No field 'missing' found on " + TestBean.class.getName());
	}

	@Test
	void resolveReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = ResourceElementResolver.forField("one")
				.resolve(registeredBean);
		assertThat(resolved).isEqualTo("1");
	}

	@Test
	void resolveWhenResourceNameAndMatchReturnsValue() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = ResourceElementResolver.forField("test", "two").resolve(registeredBean);
		assertThat(resolved).isEqualTo("2");
	}

	@Test
	void resolveWheNoMatchFallbackOnType() {
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		Object resolved = ResourceElementResolver.forField("one").resolve(registeredBean);
		assertThat(resolved).isEqualTo("2");
	}

	@Test
	void resolveWhenMultipleCandidatesWithNoNameMatchThrowsException() {
		this.beanFactory.registerSingleton("one", "1");
		this.beanFactory.registerSingleton("two", "2");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forField("test").resolve(registeredBean))
				.isInstanceOf(NoUniqueBeanDefinitionException.class)
				.hasMessageContaining(String.class.getName())
				.hasMessageContaining("one").hasMessageContaining("two");
	}

	@Test
	void resolveWhenNoCandidateMatchingTypeThrowsException() {
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forField("test").resolve(registeredBean))
				.isInstanceOf(NoSuchBeanDefinitionException.class)
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void resolveWhenInvalidMatchingTypeThrowsException() {
		this.beanFactory.registerSingleton("count", "counter");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		assertThatThrownBy(() -> ResourceElementResolver.forField("count").resolve(registeredBean))
				.isInstanceOf(BeanNotOfRequiredTypeException.class)
				.hasMessageContaining(Integer.class.getName())
				.hasMessageContaining(String.class.getName());
	}

	@Test
	void resolveAndSetSetsValue() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		TestBean testBean = new TestBean();
		ResourceElementResolver.forField("one").resolveAndSet(registeredBean, testBean);
		assertThat(testBean.one).isEqualTo("1");
	}

	@Test
	void resolveRegistersDependantBeans() {
		this.beanFactory.registerSingleton("one", "1");
		RegisteredBean registeredBean = registerTestBean(this.beanFactory);
		ResourceElementResolver.forField("one").resolve(registeredBean);
		assertThat(this.beanFactory.getDependentBeans("one")).containsExactly("testBean");
	}

	private RegisteredBean registerTestBean(DefaultListableBeanFactory beanFactory) {
		beanFactory.registerBeanDefinition("testBean", new RootBeanDefinition(TestBean.class));
		return RegisteredBean.of(beanFactory, "testBean");
	}


	static class TestBean {

		String one;

		String test;

		Integer count;
	}

}
