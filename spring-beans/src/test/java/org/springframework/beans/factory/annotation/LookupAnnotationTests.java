/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.beans.factory.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Karl Pietrzak
 * @author Juergen Hoeller
 */
class LookupAnnotationTests {

	@Test
	void testWithoutConstructorArg() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		Object expected = bean.get();
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithOverloadedArg() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		TestBean expected = bean.get("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithOneConstructorArg() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		TestBean expected = bean.getOneArgument("haha");
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithTwoConstructorArg() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		TestBean expected = bean.getTwoArguments("haha", 72);
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(expected.getName()).isEqualTo("haha");
		assertThat(expected.getAge()).isEqualTo(72);
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithThreeArgsShouldFail() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = (AbstractBean) beanFactory.getBean("abstractBean");
		assertThatExceptionOfType(AbstractMethodError.class).as("TestBean has no three arg constructor").isThrownBy(() ->
				bean.getThreeArguments("name", 1, 2));
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithEarlyInjection() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		AbstractBean bean = beanFactory.getBean("beanConsumer", BeanConsumer.class).abstractBean;
		Object expected = bean.get();
		assertThat(expected.getClass()).isEqualTo(TestBean.class);
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test  // gh-25806
	public void testWithNullBean() {
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class, () -> null);
		tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		DefaultListableBeanFactory beanFactory = configureBeanFactory(tbd);

		AbstractBean bean = beanFactory.getBean("beanConsumer", BeanConsumer.class).abstractBean;
		Object expected = bean.get();
		assertThat(expected).isNull();
		assertThat(beanFactory.getBean(BeanConsumer.class).abstractBean).isSameAs(bean);
	}

	@Test
	void testWithGenericBean() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class));
		beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
		beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

		NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
		assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
		assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
	}

	@Test
	void testSingletonWithoutMetadataCaching() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		beanFactory.setCacheBeanMetadata(false);

		beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class));
		beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
		beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

		NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
		assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
		assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
	}

	@Test
	void testPrototypeWithoutMetadataCaching() {
		DefaultListableBeanFactory beanFactory = configureBeanFactory();
		beanFactory.setCacheBeanMetadata(false);

		beanFactory.registerBeanDefinition("numberBean", new RootBeanDefinition(NumberBean.class, BeanDefinition.SCOPE_PROTOTYPE, null));
		beanFactory.registerBeanDefinition("doubleStore", new RootBeanDefinition(DoubleStore.class));
		beanFactory.registerBeanDefinition("floatStore", new RootBeanDefinition(FloatStore.class));

		NumberBean bean = (NumberBean) beanFactory.getBean("numberBean");
		assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
		assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());

		bean = (NumberBean) beanFactory.getBean("numberBean");
		assertThat(beanFactory.getBean(DoubleStore.class)).isSameAs(bean.getDoubleStore());
		assertThat(beanFactory.getBean(FloatStore.class)).isSameAs(bean.getFloatStore());
	}

	private DefaultListableBeanFactory configureBeanFactory(RootBeanDefinition tbd) {
		DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor aabpp = new AutowiredAnnotationBeanPostProcessor();
		aabpp.setBeanFactory(beanFactory);
		beanFactory.addBeanPostProcessor(aabpp);
		beanFactory.registerBeanDefinition("abstractBean", new RootBeanDefinition(AbstractBean.class));
		beanFactory.registerBeanDefinition("beanConsumer", new RootBeanDefinition(BeanConsumer.class));
		beanFactory.registerBeanDefinition("testBean", tbd);
		return beanFactory;
	}

	private DefaultListableBeanFactory configureBeanFactory() {
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		return configureBeanFactory(tbd);
	}


	public abstract static class AbstractBean {

		@Lookup("testBean")
		public abstract TestBean get();

		@Lookup
		public abstract TestBean get(String name);  // overloaded

		@Lookup
		public abstract TestBean getOneArgument(String name);

		@Lookup
		public abstract TestBean getTwoArguments(String name, int age);

		// no @Lookup annotation
		public abstract TestBean getThreeArguments(String name, int age, int anotherArg);
	}


	public static class BeanConsumer {

		@Autowired
		AbstractBean abstractBean;
	}


	public static class NumberStore<T extends Number> {
	}


	public static class DoubleStore extends NumberStore<Double> {
	}


	public static class FloatStore extends NumberStore<Float> {
	}


	public abstract static class NumberBean {

		@Lookup
		public abstract NumberStore<Double> getDoubleStore();

		@Lookup
		public abstract NumberStore<Float> getFloatStore();
	}

}
