/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.factory.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 */
class LookupMethodTests {

	private DefaultListableBeanFactory beanFactory;


	@BeforeEach
	void setup() {
		beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(beanFactory);
		reader.loadBeanDefinitions(new ClassPathResource("lookupMethodTests.xml", getClass()));
	}


	@Test
	void withoutConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		Object expected = bean.get();
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
	}

	@Test
	void withOverloadedArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.get("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
	}

	@Test
	void withOneConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getOneArgument("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
	}

	@Test
	void withTwoConstructorArg() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getTwoArguments("haha", 72);
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(expected.getAge()).isEqualTo(72);
	}

	@Test
	void withThreeArgsShouldFail() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThat(bean).isNotNull();
		assertThatExceptionOfType(AbstractMethodError.class).as("does not have a three arg constructor")
				.isThrownBy(() -> bean.getThreeArguments("name", 1, 2));
	}

	@Test
	void withOverriddenLookupMethod() {
		AbstractBean bean = (AbstractBean) beanFactory.getBean("extendedBean");
		assertThat(bean).isNotNull();
		TestBean expected = bean.getOneArgument("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(expected.isJedi()).isTrue();
	}

	@Test
	void withGenericBean() {
		RootBeanDefinition bd = new RootBeanDefinition(NumberBean.class);
		bd.getMethodOverrides().addOverride(new LookupOverride("getDoubleStore", null));
		bd.getMethodOverrides().addOverride(new LookupOverride("getFloatStore", null));
		beanFactory.registerBeanDefinition("numberBean", bd);
		beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
		beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

		NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
		assertThat(bean).isNotNull();
		assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
		assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
	}


	public abstract static class AbstractBean {

		public abstract TestBean get();

		public abstract TestBean get(String name);  // overloaded

		public abstract TestBean getOneArgument(String name);

		public abstract TestBean getTwoArguments(String name, int age);

		public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
	}


	public static class NumberStore<T extends Number> {
	}


	public static class DoubleStore extends NumberStore<Double> {
	}


	public static class FloatStore extends NumberStore<Float> {
	}


	public abstract static class NumberBean {

		public abstract NumberStore<Double> getDoubleStore();

		public abstract NumberStore<Float> getFloatStore();
	}

}
