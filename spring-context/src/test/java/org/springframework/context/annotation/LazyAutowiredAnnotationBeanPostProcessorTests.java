/*
 * Copyright 2002-2017 the original author or authors.
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

import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.tests.sample.beans.TestBean;

import static org.junit.Assert.*;

/**
 * @author Juergen Hoeller
 * @since 4.0
 */
public class LazyAutowiredAnnotationBeanPostProcessorTests {

	private void doTestLazyResourceInjection(Class<? extends TestBeanHolder> annotatedBeanClass) {
		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition abd = new RootBeanDefinition(annotatedBeanClass);
		abd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("annotatedBean", abd);
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setLazyInit(true);
		ac.registerBeanDefinition("testBean", tbd);
		ac.refresh();

		TestBeanHolder bean = ac.getBean("annotatedBean", TestBeanHolder.class);
		assertFalse(ac.getBeanFactory().containsSingleton("testBean"));
		assertNotNull(bean.getTestBean());
		assertNull(bean.getTestBean().getName());
		assertTrue(ac.getBeanFactory().containsSingleton("testBean"));
		TestBean tb = (TestBean) ac.getBean("testBean");
		tb.setName("tb");
		assertSame("tb", bean.getTestBean().getName());
	}

	@Test
	public void testLazyResourceInjectionWithField() {
		doTestLazyResourceInjection(FieldResourceInjectionBean.class);

		AnnotationConfigApplicationContext ac = new AnnotationConfigApplicationContext();
		RootBeanDefinition abd = new RootBeanDefinition(FieldResourceInjectionBean.class);
		abd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		ac.registerBeanDefinition("annotatedBean", abd);
		RootBeanDefinition tbd = new RootBeanDefinition(TestBean.class);
		tbd.setLazyInit(true);
		ac.registerBeanDefinition("testBean", tbd);
		ac.refresh();

		FieldResourceInjectionBean bean = ac.getBean("annotatedBean", FieldResourceInjectionBean.class);
		assertFalse(ac.getBeanFactory().containsSingleton("testBean"));
		assertFalse(bean.getTestBeans().isEmpty());
		assertNull(bean.getTestBeans().get(0).getName());
		assertTrue(ac.getBeanFactory().containsSingleton("testBean"));
		TestBean tb = (TestBean) ac.getBean("testBean");
		tb.setName("tb");
		assertSame("tb", bean.getTestBean().getName());
	}

	@Test
	public void testLazyResourceInjectionWithFieldAndCustomAnnotation() {
		doTestLazyResourceInjection(FieldResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	public void testLazyResourceInjectionWithMethod() {
		doTestLazyResourceInjection(MethodResourceInjectionBean.class);
	}

	@Test
	public void testLazyResourceInjectionWithMethodLevelLazy() {
		doTestLazyResourceInjection(MethodResourceInjectionBeanWithMethodLevelLazy.class);
	}

	@Test
	public void testLazyResourceInjectionWithMethodAndCustomAnnotation() {
		doTestLazyResourceInjection(MethodResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	public void testLazyResourceInjectionWithConstructor() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBean.class);
	}

	@Test
	public void testLazyResourceInjectionWithConstructorLevelLazy() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithConstructorLevelLazy.class);
	}

	@Test
	public void testLazyResourceInjectionWithConstructorAndCustomAnnotation() {
		doTestLazyResourceInjection(ConstructorResourceInjectionBeanWithCompositeAnnotation.class);
	}

	@Test
	public void testLazyResourceInjectionWithNonExistingTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(FieldResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);

		FieldResourceInjectionBean bean = (FieldResourceInjectionBean) bf.getBean("annotatedBean");
		assertNotNull(bean.getTestBean());
		try {
			bean.getTestBean().getName();
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}

	@Test
	public void testLazyOptionalResourceInjectionWithNonExistingTarget() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(OptionalFieldResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);

		OptionalFieldResourceInjectionBean bean = (OptionalFieldResourceInjectionBean) bf.getBean("annotatedBean");
		assertNotNull(bean.getTestBean());
		assertNotNull(bean.getTestBeans());
		assertTrue(bean.getTestBeans().isEmpty());
		try {
			bean.getTestBean().getName();
			fail("Should have thrown NoSuchBeanDefinitionException");
		}
		catch (NoSuchBeanDefinitionException ex) {
			// expected
		}
	}


	public interface TestBeanHolder {

		TestBean getTestBean();
	}


	public static class FieldResourceInjectionBean implements TestBeanHolder {

		@Autowired @Lazy
		private TestBean testBean;

		@Autowired @Lazy
		private List<TestBean> testBeans;

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

		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	@Autowired @Lazy
	@Retention(RetentionPolicy.RUNTIME)
	public @interface LazyInject {
	}

}
