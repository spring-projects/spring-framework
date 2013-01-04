/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.ITestBean;
import org.springframework.tests.sample.beans.LifecycleBean;
import org.springframework.tests.sample.beans.TestBean;
import org.springframework.tests.sample.beans.factory.DummyFactory;


/**
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
public class XmlListableBeanFactoryTests extends AbstractListableBeanFactoryTests {

	private DefaultListableBeanFactory parent;

	private DefaultListableBeanFactory factory;

	@Override
	protected void setUp() {
		parent = new DefaultListableBeanFactory();
		Map m = new HashMap();
		m.put("name", "Albert");
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setPropertyValues(new MutablePropertyValues(m));
		parent.registerBeanDefinition("father", bd1);
		m = new HashMap();
		m.put("name", "Roderick");
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPropertyValues(new MutablePropertyValues(m));
		parent.registerBeanDefinition("rod", bd2);

		this.factory = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(
				new ClassPathResource("test.xml", getClass()));
		this.factory.addBeanPostProcessor(new BeanPostProcessor() {
			@Override
			public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
				if (bean instanceof TestBean) {
					((TestBean) bean).setPostProcessed(true);
				}
				if (bean instanceof DummyFactory) {
					((DummyFactory) bean).setPostProcessed(true);
				}
				return bean;
			}
			@Override
			public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
				return bean;
			}
		});
		this.factory.addBeanPostProcessor(new LifecycleBean.PostProcessor());
		this.factory.addBeanPostProcessor(new ProtectedLifecycleBean.PostProcessor());
		//this.factory.preInstantiateSingletons();
	}

	@Override
	protected BeanFactory getBeanFactory() {
		return factory;
	}

	@Override
	public void testCount() {
		assertCount(24);
	}

	public void testTestBeanCount() {
		assertTestBeanCount(13);
	}

	public void testLifecycleMethods() throws Exception {
		LifecycleBean bean = (LifecycleBean) getBeanFactory().getBean("lifecycle");
		bean.businessMethod();
	}

	public void testProtectedLifecycleMethods() throws Exception {
		ProtectedLifecycleBean bean = (ProtectedLifecycleBean) getBeanFactory().getBean("protectedLifecycle");
		bean.businessMethod();
	}

	public void testDescriptionButNoProperties() throws Exception {
		TestBean validEmpty = (TestBean) getBeanFactory().getBean("validEmptyWithDescription");
		assertEquals(0, validEmpty.getAge());
	}

	/**
	 * Test that properties with name as well as id creating an alias up front.
	 */
	public void testAutoAliasing() throws Exception {
		List beanNames = Arrays.asList(getListableBeanFactory().getBeanDefinitionNames());

		TestBean tb1 = (TestBean) getBeanFactory().getBean("aliased");
		TestBean alias1 = (TestBean) getBeanFactory().getBean("myalias");
		assertTrue(tb1 == alias1);
		List tb1Aliases = Arrays.asList(getBeanFactory().getAliases("aliased"));
		assertEquals(2, tb1Aliases.size());
		assertTrue(tb1Aliases.contains("myalias"));
		assertTrue(tb1Aliases.contains("youralias"));
		assertTrue(beanNames.contains("aliased"));
		assertFalse(beanNames.contains("myalias"));
		assertFalse(beanNames.contains("youralias"));

		TestBean tb2 = (TestBean) getBeanFactory().getBean("multiAliased");
		TestBean alias2 = (TestBean) getBeanFactory().getBean("alias1");
		TestBean alias3 = (TestBean) getBeanFactory().getBean("alias2");
		TestBean alias3a = (TestBean) getBeanFactory().getBean("alias3");
		TestBean alias3b = (TestBean) getBeanFactory().getBean("alias4");
		assertTrue(tb2 == alias2);
		assertTrue(tb2 == alias3);
		assertTrue(tb2 == alias3a);
		assertTrue(tb2 == alias3b);

		List tb2Aliases = Arrays.asList(getBeanFactory().getAliases("multiAliased"));
		assertEquals(4, tb2Aliases.size());
		assertTrue(tb2Aliases.contains("alias1"));
		assertTrue(tb2Aliases.contains("alias2"));
		assertTrue(tb2Aliases.contains("alias3"));
		assertTrue(tb2Aliases.contains("alias4"));
		assertTrue(beanNames.contains("multiAliased"));
		assertFalse(beanNames.contains("alias1"));
		assertFalse(beanNames.contains("alias2"));
		assertFalse(beanNames.contains("alias3"));
		assertFalse(beanNames.contains("alias4"));

		TestBean tb3 = (TestBean) getBeanFactory().getBean("aliasWithoutId1");
		TestBean alias4 = (TestBean) getBeanFactory().getBean("aliasWithoutId2");
		TestBean alias5 = (TestBean) getBeanFactory().getBean("aliasWithoutId3");
		assertTrue(tb3 == alias4);
		assertTrue(tb3 == alias5);
		List tb3Aliases = Arrays.asList(getBeanFactory().getAliases("aliasWithoutId1"));
		assertEquals(2, tb3Aliases.size());
		assertTrue(tb3Aliases.contains("aliasWithoutId2"));
		assertTrue(tb3Aliases.contains("aliasWithoutId3"));
		assertTrue(beanNames.contains("aliasWithoutId1"));
		assertFalse(beanNames.contains("aliasWithoutId2"));
		assertFalse(beanNames.contains("aliasWithoutId3"));

		TestBean tb4 = (TestBean) getBeanFactory().getBean(TestBean.class.getName() + "#0");
		assertEquals(null, tb4.getName());

		Map drs = getListableBeanFactory().getBeansOfType(DummyReferencer.class, false, false);
		assertEquals(5, drs.size());
		assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#0"));
		assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#1"));
		assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#2"));
	}

	public void testFactoryNesting() {
		ITestBean father = (ITestBean) getBeanFactory().getBean("father");
		assertTrue("Bean from root context", father != null);

		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		assertTrue("Bean from child context", "Rod".equals(rod.getName()));
		assertTrue("Bean has external reference", rod.getSpouse() == father);

		rod = (TestBean) parent.getBean("rod");
		assertTrue("Bean from root context", "Roderick".equals(rod.getName()));
	}

	public void testFactoryReferences() {
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");

		DummyReferencer ref = (DummyReferencer) getBeanFactory().getBean("factoryReferencer");
		assertTrue(ref.getTestBean1() == ref.getTestBean2());
		assertTrue(ref.getDummyFactory() == factory);

		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("factoryReferencerWithConstructor");
		assertTrue(ref2.getTestBean1() == ref2.getTestBean2());
		assertTrue(ref2.getDummyFactory() == factory);
	}

	public void testPrototypeReferences() {
		// check that not broken by circular reference resolution mechanism
		DummyReferencer ref1 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref1.getTestBean2());
		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		assertTrue("Not the same referencer", ref1 != ref2);
		assertTrue("Not referencing same bean twice", ref2.getTestBean1() != ref2.getTestBean2());
		assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref2.getTestBean1());
		assertTrue("Not referencing same bean twice", ref1.getTestBean2() != ref2.getTestBean2());
		assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref2.getTestBean2());
	}

	public void testBeanPostProcessor() throws Exception {
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		TestBean kathy = (TestBean) getBeanFactory().getBean("kathy");
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean factoryCreated = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertTrue(kerry.isPostProcessed());
		assertTrue(kathy.isPostProcessed());
		assertTrue(factory.isPostProcessed());
		assertTrue(factoryCreated.isPostProcessed());
	}

	public void testEmptyValues() {
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		assertTrue("Touchy is empty", "".equals(rod.getTouchy()));
		assertTrue("Touchy is empty", "".equals(kerry.getTouchy()));
	}

	public void testCommentsAndCdataInValue() {
		TestBean bean = (TestBean) getBeanFactory().getBean("commentsInValue");
		assertEquals("Failed to handle comments and CDATA properly", "this is a <!--comment-->", bean.getName());
	}

}
