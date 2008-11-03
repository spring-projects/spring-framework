/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.beans.factory.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Map;

import junit.framework.TestCase;

import org.springframework.beans.ITestBean;
import org.springframework.beans.IndexedTestBean;
import org.springframework.beans.NestedTestBean;
import org.springframework.beans.TestBean;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;

/**
 * @author Juergen Hoeller
 * @author Mark Fisher
 * @author Sam Brannen
 */
public class AutowiredAnnotationBeanPostProcessorTests extends TestCase {

	public void testIncompleteBeanDefinition() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("testBean", new GenericBeanDefinition());
		try {
			bf.getBean("testBean");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getRootCause() instanceof IllegalStateException);
		}
	}

	public void testResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(ResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		ResourceInjectionBean bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());

		bean = (ResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
	}

	public void testExtendedResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerResolvableDependency(BeanFactory.class, bf);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(TypedExtendedResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb = new NestedTestBean();
		bf.registerSingleton("nestedTestBean", ntb);

		TypedExtendedResourceInjectionBean bean = (TypedExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());

		bean = (TypedExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());
	}

	public void testExtendedResourceInjectionWithOverriding() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerResolvableDependency(BeanFactory.class, bf);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition annotatedBd = new RootBeanDefinition(TypedExtendedResourceInjectionBean.class);
		TestBean tb2 = new TestBean();
		annotatedBd.getPropertyValues().addPropertyValue("testBean2", tb2);
		bf.registerBeanDefinition("annotatedBean", annotatedBd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb = new NestedTestBean();
		bf.registerSingleton("nestedTestBean", ntb);

		TypedExtendedResourceInjectionBean bean = (TypedExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb2, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());
		bf.destroySingletons();
	}

	public void testExtendedResourceInjectionWithAtRequired() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerResolvableDependency(BeanFactory.class, bf);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.addBeanPostProcessor(new RequiredAnnotationBeanPostProcessor());
		RootBeanDefinition bd = new RootBeanDefinition(TypedExtendedResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb = new NestedTestBean();
		bf.registerSingleton("nestedTestBean", ntb);

		TypedExtendedResourceInjectionBean bean = (TypedExtendedResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());
	}

	public void testOptionalResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(OptionalResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		IndexedTestBean itb = new IndexedTestBean();
		bf.registerSingleton("indexedTestBean", itb);
		NestedTestBean ntb1 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean1", ntb1);
		NestedTestBean ntb2 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean2", ntb2);

		OptionalResourceInjectionBean bean = (OptionalResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(itb, bean.getIndexedTestBean());
		assertEquals(2, bean.getNestedTestBeans().length);
		assertSame(ntb1, bean.getNestedTestBeans()[0]);
		assertSame(ntb2, bean.getNestedTestBeans()[1]);
		assertEquals(2, bean.nestedTestBeansField.length);
		assertSame(ntb1, bean.nestedTestBeansField[0]);
		assertSame(ntb2, bean.nestedTestBeansField[1]);
		bf.destroySingletons();
	}

	public void testOptionalCollectionResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition rbd = new RootBeanDefinition(OptionalCollectionResourceInjectionBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", rbd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		IndexedTestBean itb = new IndexedTestBean();
		bf.registerSingleton("indexedTestBean", itb);
		NestedTestBean ntb1 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean1", ntb1);
		NestedTestBean ntb2 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean2", ntb2);

		// Two calls to verify that caching doesn't break re-creation.
		OptionalCollectionResourceInjectionBean bean = (OptionalCollectionResourceInjectionBean) bf.getBean("annotatedBean");
		bean = (OptionalCollectionResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(itb, bean.getIndexedTestBean());
		assertEquals(2, bean.getNestedTestBeans().size());
		assertSame(ntb1, bean.getNestedTestBeans().get(0));
		assertSame(ntb2, bean.getNestedTestBeans().get(1));
		assertEquals(2, bean.nestedTestBeansSetter.size());
		assertSame(ntb1, bean.nestedTestBeansSetter.get(0));
		assertSame(ntb2, bean.nestedTestBeansSetter.get(1));
		assertEquals(2, bean.nestedTestBeansField.size());
		assertSame(ntb1, bean.nestedTestBeansField.get(0));
		assertSame(ntb2, bean.nestedTestBeansField.get(1));
		bf.destroySingletons();
	}

	public void testOptionalCollectionResourceInjectionWithSingleElement() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition rbd = new RootBeanDefinition(OptionalCollectionResourceInjectionBean.class);
		rbd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", rbd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		IndexedTestBean itb = new IndexedTestBean();
		bf.registerSingleton("indexedTestBean", itb);
		NestedTestBean ntb1 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean1", ntb1);

		// Two calls to verify that caching doesn't break re-creation.
		OptionalCollectionResourceInjectionBean bean = (OptionalCollectionResourceInjectionBean) bf.getBean("annotatedBean");
		bean = (OptionalCollectionResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(itb, bean.getIndexedTestBean());
		assertEquals(1, bean.getNestedTestBeans().size());
		assertSame(ntb1, bean.getNestedTestBeans().get(0));
		assertEquals(1, bean.nestedTestBeansSetter.size());
		assertSame(ntb1, bean.nestedTestBeansSetter.get(0));
		assertEquals(1, bean.nestedTestBeansField.size());
		assertSame(ntb1, bean.nestedTestBeansField.get(0));
		bf.destroySingletons();
	}

	public void testOptionalResourceInjectionWithIncompleteDependencies() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(OptionalResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		OptionalResourceInjectionBean bean = (OptionalResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertNull(bean.getTestBean4());
		assertNull(bean.getNestedTestBeans());
		bf.destroySingletons();
	}

	public void testOptionalResourceInjectionWithNoDependencies() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(OptionalResourceInjectionBean.class));

		OptionalResourceInjectionBean bean = (OptionalResourceInjectionBean) bf.getBean("annotatedBean");
		assertNull(bean.getTestBean());
		assertNull(bean.getTestBean2());
		assertNull(bean.getTestBean3());
		assertNull(bean.getTestBean4());
		assertNull(bean.getNestedTestBeans());
		bf.destroySingletons();
	}

	public void testConstructorResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		bf.registerResolvableDependency(BeanFactory.class, bf);
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(ConstructorResourceInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb = new NestedTestBean();
		bf.registerSingleton("nestedTestBean", ntb);

		ConstructorResourceInjectionBean bean = (ConstructorResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());

		bean = (ConstructorResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean());
		assertSame(tb, bean.getTestBean2());
		assertSame(tb, bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertSame(ntb, bean.getNestedTestBean());
		assertSame(bf, bean.getBeanFactory());
	}

	public void testConstructorResourceInjectionWithMultipleCandidates() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ConstructorsResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb1 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean1", ntb1);
		NestedTestBean ntb2 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean2", ntb2);

		ConstructorsResourceInjectionBean bean = (ConstructorsResourceInjectionBean) bf.getBean("annotatedBean");
		assertNull(bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertEquals(2, bean.getNestedTestBeans().length);
		assertSame(ntb1, bean.getNestedTestBeans()[0]);
		assertSame(ntb2, bean.getNestedTestBeans()[1]);
		bf.destroySingletons();
	}

	public void testConstructorResourceInjectionWithMultipleCandidatesAsCollection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(
				ConstructorsCollectionResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);
		NestedTestBean ntb1 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean1", ntb1);
		NestedTestBean ntb2 = new NestedTestBean();
		bf.registerSingleton("nestedTestBean2", ntb2);

		ConstructorsCollectionResourceInjectionBean bean = (ConstructorsCollectionResourceInjectionBean) bf.getBean("annotatedBean");
		assertNull(bean.getTestBean3());
		assertSame(tb, bean.getTestBean4());
		assertEquals(2, bean.getNestedTestBeans().size());
		assertSame(ntb1, bean.getNestedTestBeans().get(0));
		assertSame(ntb2, bean.getNestedTestBeans().get(1));
		bf.destroySingletons();
	}

	public void testConstructorResourceInjectionWithMultipleCandidatesAndFallback() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ConstructorsResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		ConstructorsResourceInjectionBean bean = (ConstructorsResourceInjectionBean) bf.getBean("annotatedBean");
		assertSame(tb, bean.getTestBean3());
		assertNull(bean.getTestBean4());
		bf.destroySingletons();
	}

	public void testConstructorResourceInjectionWithMultipleCandidatesAndDefaultFallback() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(ConstructorsResourceInjectionBean.class));

		ConstructorsResourceInjectionBean bean = (ConstructorsResourceInjectionBean) bf.getBean("annotatedBean");
		assertNull(bean.getTestBean3());
		assertNull(bean.getTestBean4());
		bf.destroySingletons();
	}

	public void testConstructorInjectionWithMap() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(MapConstructorInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		bf.registerSingleton("testBean2", tb1);

		MapConstructorInjectionBean bean = (MapConstructorInjectionBean) bf.getBean("annotatedBean");
		assertEquals(2, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean1"));
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean2"));
		assertTrue(bean.getTestBeanMap().values().contains(tb1));
		assertTrue(bean.getTestBeanMap().values().contains(tb2));

		bean = (MapConstructorInjectionBean) bf.getBean("annotatedBean");
		assertEquals(2, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean1"));
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean2"));
		assertTrue(bean.getTestBeanMap().values().contains(tb1));
		assertTrue(bean.getTestBeanMap().values().contains(tb2));
	}

	public void testFieldInjectionWithMap() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(MapFieldInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb1 = new TestBean();
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		bf.registerSingleton("testBean2", tb1);

		MapFieldInjectionBean bean = (MapFieldInjectionBean) bf.getBean("annotatedBean");
		assertEquals(2, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean1"));
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean2"));
		assertTrue(bean.getTestBeanMap().values().contains(tb1));
		assertTrue(bean.getTestBeanMap().values().contains(tb2));

		bean = (MapFieldInjectionBean) bf.getBean("annotatedBean");
		assertEquals(2, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean1"));
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean2"));
		assertTrue(bean.getTestBeanMap().values().contains(tb1));
		assertTrue(bean.getTestBeanMap().values().contains(tb2));
	}

	public void testMethodInjectionWithMap() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		RootBeanDefinition bd = new RootBeanDefinition(MapMethodInjectionBean.class);
		bd.setScope(RootBeanDefinition.SCOPE_PROTOTYPE);
		bf.registerBeanDefinition("annotatedBean", bd);
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		MapMethodInjectionBean bean = (MapMethodInjectionBean) bf.getBean("annotatedBean");
		assertEquals(1, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean"));
		assertTrue(bean.getTestBeanMap().values().contains(tb));
		assertSame(tb, bean.getTestBean());

		bean = (MapMethodInjectionBean) bf.getBean("annotatedBean");
		assertEquals(1, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean"));
		assertTrue(bean.getTestBeanMap().values().contains(tb));
		assertSame(tb, bean.getTestBean());
	}

	public void testMethodInjectionWithMapAndMultipleMatches() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(MapMethodInjectionBean.class));
		bf.registerBeanDefinition("testBean1", new RootBeanDefinition(TestBean.class));
		bf.registerBeanDefinition("testBean2", new RootBeanDefinition(TestBean.class));

		try {
			MapMethodInjectionBean bean = (MapMethodInjectionBean) bf.getBean("annotatedBean");
			fail("should have failed, more than one bean of type");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testMethodInjectionWithMapAndMultipleMatchesButOnlyOneAutowireCandidate() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(MapMethodInjectionBean.class));
		bf.registerBeanDefinition("testBean1", new RootBeanDefinition(TestBean.class));
		RootBeanDefinition rbd2 = new RootBeanDefinition(TestBean.class);
		rbd2.setAutowireCandidate(false);
		bf.registerBeanDefinition("testBean2", rbd2);

		MapMethodInjectionBean bean = (MapMethodInjectionBean) bf.getBean("annotatedBean");
		TestBean tb = (TestBean) bf.getBean("testBean1");
		assertEquals(1, bean.getTestBeanMap().size());
		assertTrue(bean.getTestBeanMap().keySet().contains("testBean1"));
		assertTrue(bean.getTestBeanMap().values().contains(tb));
		assertSame(tb, bean.getTestBean());
		bf.destroySingletons();
	}

	public void testMethodInjectionWithMapAndNoMatches() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("annotatedBean", new RootBeanDefinition(MapMethodInjectionBean.class));

		MapMethodInjectionBean bean = (MapMethodInjectionBean) bf.getBean("annotatedBean");
		assertNull(bean.getTestBeanMap());
		assertNull(bean.getTestBean());
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredFieldResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredFieldResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		CustomAnnotationRequiredFieldResourceInjectionBean bean = (CustomAnnotationRequiredFieldResourceInjectionBean) bf.getBean("customBean");
		assertSame(tb, bean.getTestBean());
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredFieldResourceInjectionFailsWhenNoDependencyFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredFieldResourceInjectionBean.class));

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; no dependency available for required field");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredFieldResourceInjectionFailsWhenMultipleDependenciesFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredFieldResourceInjectionBean.class));
		TestBean tb1 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; multiple beans of dependency type available");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredMethodResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredMethodResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		CustomAnnotationRequiredMethodResourceInjectionBean bean = (CustomAnnotationRequiredMethodResourceInjectionBean) bf.getBean("customBean");
		assertSame(tb, bean.getTestBean());
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredMethodResourceInjectionFailsWhenNoDependencyFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredMethodResourceInjectionBean.class));

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; no dependency available for required method");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testCustomAnnotationRequiredMethodResourceInjectionFailsWhenMultipleDependenciesFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationRequiredMethodResourceInjectionBean.class));
		TestBean tb1 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; multiple beans of dependency type available");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalFieldResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalFieldResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		CustomAnnotationOptionalFieldResourceInjectionBean bean = (CustomAnnotationOptionalFieldResourceInjectionBean) bf.getBean("customBean");
		assertSame(tb, bean.getTestBean3());
		assertNull(bean.getTestBean());
		assertNull(bean.getTestBean2());
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalFieldResourceInjectionWhenNoDependencyFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalFieldResourceInjectionBean.class));

		CustomAnnotationOptionalFieldResourceInjectionBean bean = (CustomAnnotationOptionalFieldResourceInjectionBean) bf.getBean("customBean");
		assertNull(bean.getTestBean3());
		assertNull(bean.getTestBean());
		assertNull(bean.getTestBean2());
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalFieldResourceInjectionWhenMultipleDependenciesFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalFieldResourceInjectionBean.class));
		TestBean tb1 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; multiple beans of dependency type available");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalMethodResourceInjection() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalMethodResourceInjectionBean.class));
		TestBean tb = new TestBean();
		bf.registerSingleton("testBean", tb);

		CustomAnnotationOptionalMethodResourceInjectionBean bean = (CustomAnnotationOptionalMethodResourceInjectionBean) bf.getBean("customBean");
		assertSame(tb, bean.getTestBean3());
		assertNull(bean.getTestBean());
		assertNull(bean.getTestBean2());
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalMethodResourceInjectionWhenNoDependencyFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalMethodResourceInjectionBean.class));

		CustomAnnotationOptionalMethodResourceInjectionBean bean = (CustomAnnotationOptionalMethodResourceInjectionBean) bf.getBean("customBean");
		assertNull(bean.getTestBean3());
		assertNull(bean.getTestBean());
		assertNull(bean.getTestBean2());
		bf.destroySingletons();
	}

	public void testCustomAnnotationOptionalMethodResourceInjectionWhenMultipleDependenciesFound() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setAutowiredAnnotationType(MyAutowired.class);
		bpp.setRequiredParameterName("optional");
		bpp.setRequiredParameterValue(false);
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("customBean", new RootBeanDefinition(
				CustomAnnotationOptionalMethodResourceInjectionBean.class));
		TestBean tb1 = new TestBean();
		bf.registerSingleton("testBean1", tb1);
		TestBean tb2 = new TestBean();
		bf.registerSingleton("testBean2", tb2);

		try {
			bf.getBean("customBean");
			fail("expected BeanCreationException; multiple beans of dependency type available");
		}
		catch (BeanCreationException e) {
			// expected
		}
		bf.destroySingletons();
	}

	/**
	 * Verifies that a dependency on a {@link FactoryBean} can be autowired via
	 * {@link Autowired @Autowired}, specifically addressing the JIRA issue
	 * raised in <a
	 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-4040"
	 * target="_blank">SPR-4040</a>.
	 */
	public void testBeanAutowiredWithFactoryBean() {
		DefaultListableBeanFactory bf = new DefaultListableBeanFactory();
		AutowiredAnnotationBeanPostProcessor bpp = new AutowiredAnnotationBeanPostProcessor();
		bpp.setBeanFactory(bf);
		bf.addBeanPostProcessor(bpp);
		bf.registerBeanDefinition("factoryBeanDependentBean", new RootBeanDefinition(FactoryBeanDependentBean.class));
		bf.registerSingleton("stringFactoryBean", new StringFactoryBean());

		final StringFactoryBean factoryBean = (StringFactoryBean) bf.getBean("&stringFactoryBean");
		final FactoryBeanDependentBean bean = (FactoryBeanDependentBean) bf.getBean("factoryBeanDependentBean");

		assertNotNull("The singleton StringFactoryBean should have been registered.", factoryBean);
		assertNotNull("The factoryBeanDependentBean should have been registered.", bean);
		assertEquals("The FactoryBeanDependentBean should have been autowired 'by type' with the StringFactoryBean.",
				factoryBean, bean.getFactoryBean());

		bf.destroySingletons();
	}


	public static class ResourceInjectionBean {

		@Autowired(required = false)
		private TestBean testBean;

		private TestBean testBean2;


		@Autowired
		public void setTestBean2(TestBean testBean2) {
			if (this.testBean2 != null) {
				throw new IllegalStateException("Already called");
			}
			this.testBean2 = testBean2;
		}

		public TestBean getTestBean() {
			return this.testBean;
		}

		public TestBean getTestBean2() {
			return this.testBean2;
		}
	}


	public static class ExtendedResourceInjectionBean<T> extends ResourceInjectionBean {

		@Autowired
		protected ITestBean testBean3;

		private T nestedTestBean;

		private ITestBean testBean4;

		private BeanFactory beanFactory;

		public ExtendedResourceInjectionBean() {
		}

		@Autowired @Required
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		@Autowired
		private void inject(ITestBean testBean4, T nestedTestBean) {
			this.testBean4 = testBean4;
			this.nestedTestBean = nestedTestBean;
		}

		@Autowired
		protected void initBeanFactory(BeanFactory beanFactory) {
			this.beanFactory = beanFactory;
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public T getNestedTestBean() {
			return this.nestedTestBean;
		}

		public BeanFactory getBeanFactory() {
			return this.beanFactory;
		}
	}


	public static class TypedExtendedResourceInjectionBean extends ExtendedResourceInjectionBean<NestedTestBean> {

	}


	public static class OptionalResourceInjectionBean extends ResourceInjectionBean {

		@Autowired(required = false)
		protected ITestBean testBean3;

		private IndexedTestBean indexedTestBean;

		private NestedTestBean[] nestedTestBeans;

		@Autowired(required = false)
		public NestedTestBean[] nestedTestBeansField;

		private ITestBean testBean4;

		@Autowired(required = false)
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		@Autowired(required = false)
		private void inject(ITestBean testBean4, NestedTestBean[] nestedTestBeans, IndexedTestBean indexedTestBean) {
			this.testBean4 = testBean4;
			this.indexedTestBean = indexedTestBean;
			this.nestedTestBeans = nestedTestBeans;
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public IndexedTestBean getIndexedTestBean() {
			return this.indexedTestBean;
		}

		public NestedTestBean[] getNestedTestBeans() {
			return this.nestedTestBeans;
		}
	}


	public static class OptionalCollectionResourceInjectionBean extends ResourceInjectionBean {

		@Autowired(required = false)
		protected ITestBean testBean3;

		private IndexedTestBean indexedTestBean;

		private List<NestedTestBean> nestedTestBeans;

		public List<NestedTestBean> nestedTestBeansSetter;

		@Autowired(required = false)
		public List<NestedTestBean> nestedTestBeansField;

		private ITestBean testBean4;

		@Autowired(required = false)
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		@Autowired(required = false)
		private void inject(ITestBean testBean4, List<NestedTestBean> nestedTestBeans, IndexedTestBean indexedTestBean) {
			this.testBean4 = testBean4;
			this.indexedTestBean = indexedTestBean;
			this.nestedTestBeans = nestedTestBeans;
		}

		@Autowired(required = false)
		public void setNestedTestBeans(List<NestedTestBean> nestedTestBeans) {
			this.nestedTestBeansSetter = nestedTestBeans;
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public IndexedTestBean getIndexedTestBean() {
			return this.indexedTestBean;
		}

		public List<NestedTestBean> getNestedTestBeans() {
			return this.nestedTestBeans;
		}
	}


	public static class ConstructorResourceInjectionBean extends ResourceInjectionBean {

		@Autowired
		protected ITestBean testBean3;

		private ITestBean testBean4;

		private NestedTestBean nestedTestBean;

		private ConfigurableListableBeanFactory beanFactory;


		public ConstructorResourceInjectionBean() {
			throw new UnsupportedOperationException();
		}

		public ConstructorResourceInjectionBean(ITestBean testBean3) {
			throw new UnsupportedOperationException();
		}

		@Autowired
		public ConstructorResourceInjectionBean(ITestBean testBean4, NestedTestBean nestedTestBean,
				ConfigurableListableBeanFactory beanFactory) {
			this.testBean4 = testBean4;
			this.nestedTestBean = nestedTestBean;
			this.beanFactory = beanFactory;
		}

		public ConstructorResourceInjectionBean(NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		public ConstructorResourceInjectionBean(ITestBean testBean3, ITestBean testBean4, NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		@Autowired
		public void setTestBean2(TestBean testBean2) {
			super.setTestBean2(testBean2);
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public NestedTestBean getNestedTestBean() {
			return this.nestedTestBean;
		}

		public ConfigurableListableBeanFactory getBeanFactory() {
			return this.beanFactory;
		}
	}


	public static class ConstructorsResourceInjectionBean {

		protected ITestBean testBean3;

		private ITestBean testBean4;

		private NestedTestBean[] nestedTestBeans;

		public ConstructorsResourceInjectionBean() {
		}

		@Autowired(required = false)
		public ConstructorsResourceInjectionBean(ITestBean testBean3) {
			this.testBean3 = testBean3;
		}

		@Autowired(required = false)
		public ConstructorsResourceInjectionBean(ITestBean testBean4, NestedTestBean[] nestedTestBeans) {
			this.testBean4 = testBean4;
			this.nestedTestBeans = nestedTestBeans;
		}

		public ConstructorsResourceInjectionBean(NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		public ConstructorsResourceInjectionBean(ITestBean testBean3, ITestBean testBean4, NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public NestedTestBean[] getNestedTestBeans() {
			return this.nestedTestBeans;
		}
	}


	public static class ConstructorsCollectionResourceInjectionBean {

		protected ITestBean testBean3;

		private ITestBean testBean4;

		private List<NestedTestBean> nestedTestBeans;

		public ConstructorsCollectionResourceInjectionBean() {
		}

		@Autowired(required = false)
		public ConstructorsCollectionResourceInjectionBean(ITestBean testBean3) {
			this.testBean3 = testBean3;
		}

		@Autowired(required = false)
		public ConstructorsCollectionResourceInjectionBean(ITestBean testBean4, List<NestedTestBean> nestedTestBeans) {
			this.testBean4 = testBean4;
			this.nestedTestBeans = nestedTestBeans;
		}

		public ConstructorsCollectionResourceInjectionBean(NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		public ConstructorsCollectionResourceInjectionBean(ITestBean testBean3, ITestBean testBean4,
				NestedTestBean nestedTestBean) {
			throw new UnsupportedOperationException();
		}

		public ITestBean getTestBean3() {
			return this.testBean3;
		}

		public ITestBean getTestBean4() {
			return this.testBean4;
		}

		public List<NestedTestBean> getNestedTestBeans() {
			return this.nestedTestBeans;
		}
	}


	public static class MapConstructorInjectionBean {

		private Map<String, TestBean> testBeanMap;

		@Autowired
		public MapConstructorInjectionBean(Map<String, TestBean> testBeanMap) {
			this.testBeanMap = testBeanMap;
		}

		public Map<String, TestBean> getTestBeanMap() {
			return this.testBeanMap;
		}
	}


	public static class MapFieldInjectionBean {

		@Autowired
		private Map<String, TestBean> testBeanMap;


		public Map<String, TestBean> getTestBeanMap() {
			return this.testBeanMap;
		}
	}


	public static class MapMethodInjectionBean {

		private TestBean testBean;

		private Map<String, TestBean> testBeanMap;

		@Autowired(required = false)
		public void setTestBeanMap(TestBean testBean, Map<String, TestBean> testBeanMap) {
			this.testBean = testBean;
			this.testBeanMap = testBeanMap;
		}

		public TestBean getTestBean() {
			return this.testBean;
		}

		public Map<String, TestBean> getTestBeanMap() {
			return this.testBeanMap;
		}
	}


	public static class CustomAnnotationRequiredFieldResourceInjectionBean {

		@MyAutowired(optional = false)
		private TestBean testBean;

		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class CustomAnnotationRequiredMethodResourceInjectionBean {

		private TestBean testBean;

		@MyAutowired(optional = false)
		public void setTestBean(TestBean testBean) {
			this.testBean = testBean;
		}

		public TestBean getTestBean() {
			return this.testBean;
		}
	}


	public static class CustomAnnotationOptionalFieldResourceInjectionBean extends ResourceInjectionBean {

		@MyAutowired(optional = true)
		private TestBean testBean3;

		public TestBean getTestBean3() {
			return this.testBean3;
		}
	}


	public static class CustomAnnotationOptionalMethodResourceInjectionBean extends ResourceInjectionBean {

		private TestBean testBean3;


		@MyAutowired(optional = true)
		protected void setTestBean3(TestBean testBean3) {
			this.testBean3 = testBean3;
		}

		public TestBean getTestBean3() {
			return this.testBean3;
		}
	}


	@Target({ElementType.METHOD, ElementType.FIELD})
	@Retention(RetentionPolicy.RUNTIME)
	public static @interface MyAutowired {

		boolean optional() default false;
	}


	/**
	 * Bean with a dependency on a {@link FactoryBean}.
	 */
	private static class FactoryBeanDependentBean {

		@Autowired
		private FactoryBean factoryBean;

		public final FactoryBean getFactoryBean() {
			return this.factoryBean;
		}
	}


	public static class StringFactoryBean implements FactoryBean {

		public Object getObject() throws Exception {
			return "";
		}

		public Class getObjectType() {
			return String.class;
		}

		public boolean isSingleton() {
			return true;
		}
	}

}
