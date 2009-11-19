/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.context.annotation;

import java.util.Properties;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.ejb.EJB;

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.INestedTestBean;
import org.springframework.beans.ITestBean;
import org.springframework.beans.NestedTestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.InitDestroyAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jndi.support.SimpleJndiBeanFactory;
import org.springframework.mock.jndi.ExpectedLookupTemplate;
import org.springframework.util.SerializationTestUtils;

/**
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class CommonAnnotationBeanPostProcessorTests {

	@Test
	public void testPostConstructAndPreDestroy() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

		AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testPostConstructAndPreDestroyWithPostProcessor() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.addBeanPostProcessor(new InitDestroyBeanPostProcessor());
		bf.addBeanPostProcessor(new CommonAnnotationBeanPostProcessor());
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

		AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testPostConstructAndPreDestroyWithApplicationContextAndPostProcessor() {
		GenericApplicationContext ctx = new GenericApplicationContext();
		ctx.registerBeanDefinition("bpp1", new RootBeanDefinition(InitDestroyBeanPostProcessor.class));
		ctx.registerBeanDefinition("bpp2", new RootBeanDefinition(CommonAnnotationBeanPostProcessor.class));
		ctx.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));
		ctx.refresh();

		AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) ctx.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		ctx.close();
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testPostConstructAndPreDestroyWithManualConfiguration() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		InitDestroyAnnotationBeanPostProcessor bpp = new InitDestroyAnnotationBeanPostProcessor();
		bpp.setInitAnnotationType(PostConstruct.class);
		bpp.setDestroyAnnotationType(PreDestroy.class);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(AnnotatedInitDestroyBean.class));

		AnnotatedInitDestroyBean bean = (AnnotatedInitDestroyBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testSerialization() throws Exception {
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		CommonAnnotationBeanPostProcessor bpp2 = (CommonAnnotationBeanPostProcessor)
				SerializationTestUtils.serializeAndDeserialize(bpp);

		AnnotatedInitDestroyBean bean = new AnnotatedInitDestroyBean();
		bpp2.postProcessBeforeDestruction(bean, "annotatedBean");
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testSerializationWithManualConfiguration() throws Exception {
		InitDestroyAnnotationBeanPostProcessor bpp = new InitDestroyAnnotationBeanPostProcessor();
		bpp.setInitAnnotationType(PostConstruct.class);
		bpp.setDestroyAnnotationType(PreDestroy.class);
		InitDestroyAnnotationBeanPostProcessor bpp2 = (InitDestroyAnnotationBeanPostProcessor)
				SerializationTestUtils.serializeAndDeserialize(bpp);

		AnnotatedInitDestroyBean bean = new AnnotatedInitDestroyBean();
		bpp2.postProcessBeforeDestruction(bean, "annotatedBean");
		assertTrue(bean.destroyCalled);
	}

	@Test
	public void testResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		bpp.setResourceFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertTrue(bean.init3Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
		assertTrue(bean.destroy3Called);
	}

	@Test
	public void testResourceInjectionWithTwoProcessors() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		bpp.setResourceFactory(bf);
		bf.addBeanPostProcessor(bpp);
		CommonAnnotationBeanPostProcessor bpp2 = new CommonAnnotationBeanPostProcessor();
		bpp2.setResourceFactory(bf);
		bf.addBeanPostProcessor(bpp2);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
	}

	@Test
	public void testResourceInjectionFromJndi() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		SimpleJndiBeanFactory resourceFactory = new SimpleJndiBeanFactory();
		ExpectedLookupTemplate jndiTemplate = new ExpectedLookupTemplate();
		TestBean tb = new TestBean();
		jndiTemplate.addObject("java:comp/env/testBean", tb);
		TestBean tb2 = new TestBean();
		jndiTemplate.addObject("java:comp/env/testBean2", tb2);
		resourceFactory.setJndiTemplate(jndiTemplate);
		bpp.setResourceFactory(resourceFactory);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ResourceInjectionBean.class));

		ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
	}

	@Test
	public void testExtendedResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerResolvableDependency(BeanFactory.class, bf);

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("tb", "testBean3");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(bf);

		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ExtendedResourceInjectionBean.class));
		bf.registerBeanDefinition("annotatedBean2", new RootBeanDefinition(NamedResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);
		TestBean tb3 = new TestBean();
		bf.registerSingleton("testBean3", tb3);
		TestBean tb4 = new TestBean();
		bf.registerSingleton("testBean4", tb4);
		NestedTestBean tb6 = new NestedTestBean();
		bf.registerSingleton("xy", tb6);
		bf.registerAlias("xy", "testBean9");

		ExtendedResourceInjectionBean bean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		assertSame(tb4, bean.getTestBean3());
		assertSame(tb3, bean.getTestBean4());
		assertSame(tb6, bean.testBean5);
		assertSame(tb6, bean.testBean6);
		assertSame(bf, bean.beanFactory);

		NamedResourceInjectionBean bean2 = (NamedResourceInjectionBean) bf.getBean("annotatedBean2");
		assertSame(tb6, bean2.testBean);

		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
	}

	@Test
	public void testExtendedResourceInjectionWithOverriding() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerResolvableDependency(BeanFactory.class, bf);

		PropertyPlaceholderConfigurer ppc = new PropertyPlaceholderConfigurer();
		Properties props = new Properties();
		props.setProperty("tb", "testBean3");
		ppc.setProperties(props);
		ppc.postProcessBeanFactory(bf);

		RootBeanDefinition annotatedBd = new RootBeanDefinition(ExtendedResourceInjectionBean.class);
		TestBean tb5 = new TestBean();
		annotatedBd.getPropertyValues().add("testBean2", tb5);
		bf.registerBeanDefinition("annotatedBean", annotatedBd);
		bf.registerBeanDefinition("annotatedBean2", new RootBeanDefinition(NamedResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);
		TestBean tb3 = new TestBean();
		bf.registerSingleton("testBean3", tb3);
		TestBean tb4 = new TestBean();
		bf.registerSingleton("testBean4", tb4);
		NestedTestBean tb6 = new NestedTestBean();
		bf.registerSingleton("xy", tb6);

		ExtendedResourceInjectionBean bean = (ExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb5, bean.getTestBean2());
		assertSame(tb4, bean.getTestBean3());
		assertSame(tb3, bean.getTestBean4());
		assertSame(tb6, bean.testBean5);
		assertSame(tb6, bean.testBean6);
		assertSame(bf, bean.beanFactory);

		try {
			bf.getBean("annotatedBean2");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause() instanceof NoSuchBeanDefinitionException);
			NoSuchBeanDefinitionException innerEx = (NoSuchBeanDefinitionException) ex.getRootCause();
			assertEquals("testBean9", innerEx.getBeanName());
		}

		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
	}

	@Test
	public void testExtendedEjbInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		CommonAnnotationBeanPostProcessor bpp = new CommonAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerResolvableDependency(BeanFactory.class, bf);

		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ExtendedEjbInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);
		TestBean tb3 = new TestBean();
		bf.registerSingleton("testBean3", tb3);
		TestBean tb4 = new TestBean();
		bf.registerSingleton("testBean4", tb4);
		NestedTestBean tb6 = new NestedTestBean();
		bf.registerSingleton("xy", tb6);
		bf.registerAlias("xy", "testBean9");

		ExtendedEjbInjectionBean bean = (ExtendedEjbInjectionBean) bf.getBean("annotatedBean");
		assertTrue(bean.initCalled);
		assertTrue(bean.init2Called);
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		assertSame(tb4, bean.getTestBean3());
		assertSame(tb3, bean.getTestBean4());
		assertSame(tb6, bean.testBean5);
		assertSame(tb6, bean.testBean6);
		assertSame(bf, bean.beanFactory);

		bf.destroySingletons();
		assertTrue(bean.destroyCalled);
		assertTrue(bean.destroy2Called);
	}


	public static class AnnotatedInitDestroyBean {

		public boolean initCalled = false;

		public boolean destroyCalled = false;

		@PostConstruct
		private void init() {
			if (this.initCalled) {
				throw new IllegalStateException("Already called");
			}
			this.initCalled = true;
		}

		@PreDestroy
		private void destroy() {
			if (this.destroyCalled) {
				throw new IllegalStateException("Already called");
			}
			this.destroyCalled = true;
		}
	}


	public static class InitDestroyBeanPostProcessor implements DestructionAwareBeanPostProcessor {

		public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof AnnotatedInitDestroyBean) {
				assertFalse(((AnnotatedInitDestroyBean) bean).initCalled);
			}
			return bean;
		}

		public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
			if (bean instanceof AnnotatedInitDestroyBean) {
				assertTrue(((AnnotatedInitDestroyBean) bean).initCalled);
			}
			return bean;
		}

		public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
			if (bean instanceof AnnotatedInitDestroyBean) {
				assertFalse(((AnnotatedInitDestroyBean) bean).destroyCalled);
			}
		}
	}


	public static class ResourceInjectionBean extends AnnotatedInitDestroyBean {

		public boolean init2Called = false;

		public boolean init3Called = false;

		public boolean destroy2Called = false;

		public boolean destroy3Called = false;

		@Resource
		private TestBean testBean;

		private TestBean testBean2;

		@PostConstruct
		protected void init2() {
			if (this.testBean == null || this.testBean2 == null) {
				throw new IllegalStateException("Resources not injected");
			}
			if (!this.initCalled) {
				throw new IllegalStateException("Superclass init method not called yet");
			}
			if (this.init2Called) {
				throw new IllegalStateException("Already called");
			}
			this.init2Called = true;
		}

		@PostConstruct
		private void init() {
			if (this.init3Called) {
				throw new IllegalStateException("Already called");
			}
			this.init3Called = true;
		}

		@PreDestroy
		protected void destroy2() {
			if (this.destroyCalled) {
				throw new IllegalStateException("Superclass destroy called too soon");
			}
			if (this.destroy2Called) {
				throw new IllegalStateException("Already called");
			}
			this.destroy2Called = true;
		}

		@PreDestroy
		private void destroy() {
			if (this.destroyCalled) {
				throw new IllegalStateException("Superclass destroy called too soon");
			}
			if (this.destroy3Called) {
				throw new IllegalStateException("Already called");
			}
			this.destroy3Called = true;
		}

		@Resource
		public void setTestBean2(TestBean testBean2) {
			if (this.testBean2 != null) {
				throw new IllegalStateException("Already called");
			}
			this.testBean2 = testBean2;
		}

		public TestBean getTestBean() {
			return testBean;
		}

		public TestBean getTestBean2() {
			return testBean2;
		}
	}


	public static class ExtendedResourceInjectionBean extends ResourceInjectionBean {

		@Resource(name="testBean4", type=TestBean.class)
		protected ITestBean testBean3;

		private ITestBean testBean4;

		@Resource
		private INestedTestBean testBean5;

		private INestedTestBean testBean6;

		@Resource
		private BeanFactory beanFactory;

		@Resource
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		@Resource(name="${tb}", type=ITestBean.class)
		private void setTestBean4(ITestBean testBean4) {
			this.testBean4 = testBean4;
		}

		@Resource
		public void setTestBean6(INestedTestBean testBean6) {
			this.testBean6 = testBean6;
		}

		public ITestBean getTestBean3() {
			return testBean3;
		}

		public ITestBean getTestBean4() {
			return testBean4;
		}

		@PostConstruct
		protected void init2() {
			if (this.testBean3 == null || this.testBean4 == null) {
				throw new IllegalStateException("Resources not injected");
			}
			super.init2();
		}

		@PreDestroy
		protected void destroy2() {
			super.destroy2();
		}
	}


	public static class ExtendedEjbInjectionBean extends ResourceInjectionBean {

		@EJB(name="testBean4", beanInterface=TestBean.class)
		protected ITestBean testBean3;

		private ITestBean testBean4;

		@EJB
		private INestedTestBean testBean5;

		private INestedTestBean testBean6;

		@Resource
		private BeanFactory beanFactory;

		@EJB
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		@EJB(beanName="testBean3", beanInterface=ITestBean.class)
		private void setTestBean4(ITestBean testBean4) {
			this.testBean4 = testBean4;
		}

		@EJB
		public void setTestBean6(INestedTestBean testBean6) {
			this.testBean6 = testBean6;
		}

		public ITestBean getTestBean3() {
			return testBean3;
		}

		public ITestBean getTestBean4() {
			return testBean4;
		}

		@PostConstruct
		protected void init2() {
			if (this.testBean3 == null || this.testBean4 == null) {
				throw new IllegalStateException("Resources not injected");
			}
			super.init2();
		}

		@PreDestroy
		protected void destroy2() {
			super.destroy2();
		}
	}


	private static class NamedResourceInjectionBean {

		@Resource(name="testBean9")
		private INestedTestBean testBean;
	}

}
