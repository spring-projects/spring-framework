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

package org.springframework.context.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.aop.TargetSource;
import org.springframework.aop.framework.Advised;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.util.ObjectUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
class LazyAutowiredAnnotationBeanPostProcessorTests {

	private void doTestLazyResourceInjection(Class<? extends TestBeanHolder> annotatedBeanClass) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition abd = new RootBeanDefinition(annotatedBeanClass);
		abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("annotatedBean", abd);
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setLazyInit(true);
		ac.registerBeanDefinition("testBean", tbd);
		ac.refresh();

		ConfigurableListableBeanFactory bf = ac.getBeanFactory();
		TestBeanHolder bean = ac.getBean("annotatedBean", TestBeanHolder.class);
		assertThat(bf.containsSingleton("testBean")).isFalse();
		assertThat(bean.getTestBean()).isNotNull();
		assertThat(bean.getTestBean().getName()).isNull();
		assertThat(bf.containsSingleton("testBean")).isTrue();
		TestBean tb = (TestBean) ac.getBean("testBean");
		tb.setName("tb");
		assertThat(bean.getTestBean().getName()).isSameAs("tb");

		assertThat(ObjectUtils.containsElement(bf.getDependenciesForBean("annotatedBean"), "testBean")).isTrue();
		assertThat(ObjectUtils.containsElement(bf.getDependentBeans("testBean"), "annotatedBean")).isTrue();
		ac.close();
	}

	@Test
	void lazyResourceInjectionWithField() throws Exception {
		doTestLazyResourceInjection(FieldResourceInjectionBean.class);

		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
		abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("annotatedBean", abd);
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setLazyInit(true);
		ac.registerBeanDefinition("testBean", tbd);
		ac.refresh();

		FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
		assertThat(ac.getBeanFactory().containsSingleton("testBean")).isFalse();
		assertThat(bean.getTestBeans()).isNotEmpty();
		assertThat(bean.getTestBeans().get(0).getName()).isNull();
		assertThat(ac.getBeanFactory().containsSingleton("testBean")).isTrue();

		TestBean tb = (TestBean) ac.getBean("testBean");
		tb.setName("tb");
		assertThat(bean.getTestBean().getName()).isSameAs("tb");

		assertThat(bean.getTestBeans() instanceof Advised).isTrue();
		TargetSource targetSource = ((Advised) bean.getTestBeans()).getTargetSource();
		assertThat(targetSource.getTarget()).isSameAs(targetSource.getTarget());

		ac.close();
	}

	@Test
	void lazyResourceInjectionWithFieldForPrototype() {
		doTestLazyResourceInjection(FieldResourceInjectionBean.class);

		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
		abd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("annotatedBean", abd);
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		tbd.setLazyInit(true);
		ac.registerBeanDefinition("testBean", tbd);
		ac.refresh();

		FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
		assertThat(bean.getTestBeans()).isNotEmpty();
		TestBean tb = bean.getTestBeans().get(0);
		assertThat(bean.getTestBeans().get(0)).isNotSameAs(tb);
		ac.close();
	}

	@Test
	void lazyResourceInjectionWithFieldAndCustomAnnotation() {
		doTestLazyResourceInjection(FieldResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	void lazyResourceInjectionWithMethod() {
		doTestLazyResourceInjection(MethodResourceInjectionBean.class);
	}

	@Test
	void lazyResourceInjectionWithMethodLevelLazy() {
		doTestLazyResourceInjection(MethodResourceInjectionBeanWithMethodLevelLazy.class);
	}

	@Test
	void lazyResourceInjectionWithMethodAndCustomAnnotation() {
		doTestLazyResourceInjection(MethodResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	void lazyResourceInjectionWithConstructor() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBean.class);
	}

	@Test
	void lazyResourceInjectionWithConstructorLevelLazy() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithConstructorLevelLazy.class);
	}

	@Test
	void lazyResourceInjectionWithConstructorAndCustomAnnotation() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	void lazyResourceInjectionWithNonExistingTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(FieldResourceInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);

		FieldResourceInjectionBean bean = (FieldResourceInjectionBean) bf.getBean("annotatedBean");
		assertThat(bean.getTestBean()).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				bean.getTestBean().getName());
	}

	@Test
	void lazyOptionalResourceInjectionWithNonExistingTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(OptionalFieldResourceInjectionBean.class);
		bd.setScope(BeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);

		OptionalFieldResourceInjectionBean bean = (OptionalFieldResourceInjectionBean) bf.getBean("annotatedBean");
		assertThat(bean.getTestBean()).isNotNull();
		assertThat(bean.getTestBeans()).isNotNull();
		assertThat(bean.getTestBeans()).isEmpty();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class).isThrownBy(() ->
				bean.getTestBean().getName());
	}


	public interface TestBeanHolder {

		TestBean getTestBean();
	}


	public static class FieldResourceInjectionBean implements TestBeanHolder {

		@Autowired @Lazy
		private TestBean testBean;

		@Autowired @Lazy
		private List<TestBean> testBeans;

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}

		public List<TestBean> getTestBeans() {
			return testBeans;
		}
	}


	public static class OptionalFieldResourceInjectionBean implements TestBeanHolder {

		@Autowired(required = false) @Lazy
		private TestBean testBean;

		@Autowired(required = false) @Lazy
		private List<TestBean> testBeans;

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}

		public List<TestBean> getTestBeans() {
			return this.testBeans;
		}
	}


	public static class FieldResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

		@LazyInject
		private TestBean testBean;

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class MethodResourceInjectionBean implements TestBeanHolder {

		private TestBean testBean;

		@Autowired
		public void setTestBean(@Lazy TestBean testBean) {
			if (this.testBean != null) {
				throw new IllegalStateException("Already called");
			}
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class MethodResourceInjectionBeanWithMethodLevelLazy implements TestBeanHolder {

		private TestBean testBean;

		@Autowired @Lazy
		public void setTestBean(TestBean testBean) {
			if (this.testBean != null) {
				throw new IllegalStateException("Already called");
			}
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class MethodResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

		private TestBean testBean;

		@LazyInject
		public void setTestBean(TestBean testBean) {
			if (this.testBean != null) {
				throw new IllegalStateException("Already called");
			}
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class ConstructorResourceInjectionBean implements TestBeanHolder {

		private final TestBean testBean;

		@Autowired
		public ConstructorResourceInjectionBean(@Lazy TestBean testBean) {
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class ConstructorResourceInjectionBeanWithConstructorLevelLazy implements TestBeanHolder {

		private final TestBean testBean;

		@Autowired @Lazy
		public ConstructorResourceInjectionBeanWithConstructorLevelLazy(TestBean testBean) {
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class ConstructorResourceInjectionBeanWithCompositeAnnotation implements TestBeanHolder {

		private final TestBean testBean;

		@LazyInject
		public ConstructorResourceInjectionBeanWithCompositeAnnotation(TestBean testBean) {
			this.testBean = testBean;
		}

		@Override
		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	@Autowired @Lazy
	@Retention(RetentionPolicy.RUNTIME)
	public @interface LazyInject {
	}

}
