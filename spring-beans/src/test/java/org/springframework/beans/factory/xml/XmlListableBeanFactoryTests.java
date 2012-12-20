/*
 * Copyright 2002-2012 the original author or authors.
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

import junit.framework.Assert;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.ClassPathResource;

import test.beans.DummyFactory;
import test.beans.ITestBean;
import test.beans.LifecycleBean;
import test.beans.TestBean;

/**
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
public class XmlListableBeanFactoryTests extends AbstractListableBeanFactoryTests {

	private DefaultListableBeanFactory parent;

	private DefaultListableBeanFactory factory;

	protected void setUp() {
		parent = new DefaultListableBeanFactory();
		Map<Object, Object> m = new HashMap<Object, Object>();
		m.put("name", "Albert");
		RootBeanDefinition fatherBeanDefinition = new RootBeanDefinition(TestBean.class);
		fatherBeanDefinition.setPropertyValues(new MutablePropertyValues(m));
		parent.registerBeanDefinition("father", fatherBeanDefinition);
		m = new HashMap<Object, Object>();
		m.put("name", "Roderick");
		RootBeanDefinition rodBeanDefinition = new RootBeanDefinition(TestBean.class);
		rodBeanDefinition.setPropertyValues(new MutablePropertyValues(m));
		parent.registerBeanDefinition("rod", rodBeanDefinition);

		this.factory = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(new ClassPathResource("test.xml", getClass()));
		this.factory.addBeanPostProcessor(new BeanPostProcessor() {
			public Object postProcessBeforeInitialization(Object bean, String name) throws BeansException {
				if (bean instanceof TestBean) {
					((TestBean) bean).setPostProcessed(true);
				}
				if (bean instanceof DummyFactory) {
					((DummyFactory) bean).setPostProcessed(true);
				}
				return bean;
			}
			public Object postProcessAfterInitialization(Object bean, String name) throws BeansException {
				return bean;
			}
		});
		this.factory.addBeanPostProcessor(new LifecycleBean.PostProcessor());
		this.factory.addBeanPostProcessor(new ProtectedLifecycleBean.PostProcessor());
		//this.factory.preInstantiateSingletons();
	}

	protected BeanFactory getBeanFactory() {
		return factory;
	}

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
		Assert.assertEquals(0, validEmpty.getAge());
	}

	/**
	 * Test that properties with name as well as id creating an alias up front.
	 */
	public void testAutoAliasing() throws Exception {
		List<String> beanNames = Arrays.asList(getListableBeanFactory().getBeanDefinitionNames());

		TestBean tb1 = (TestBean) getBeanFactory().getBean("aliased");
		TestBean alias1 = (TestBean) getBeanFactory().getBean("myalias");
		Assert.assertTrue(tb1 == alias1);
		List<String> tb1Aliases = Arrays.asList(getBeanFactory().getAliases("aliased"));
		Assert.assertEquals(2, tb1Aliases.size());
		Assert.assertTrue(tb1Aliases.contains("myalias"));
		Assert.assertTrue(tb1Aliases.contains("youralias"));
		Assert.assertTrue(beanNames.contains("aliased"));
		Assert.assertFalse(beanNames.contains("myalias"));
		Assert.assertFalse(beanNames.contains("youralias"));

		TestBean tb2 = (TestBean) getBeanFactory().getBean("multiAliased");
		TestBean alias2 = (TestBean) getBeanFactory().getBean("alias1");
		TestBean alias3 = (TestBean) getBeanFactory().getBean("alias2");
		TestBean alias3a = (TestBean) getBeanFactory().getBean("alias3");
		TestBean alias3b = (TestBean) getBeanFactory().getBean("alias4");
		Assert.assertTrue(tb2 == alias2);
		Assert.assertTrue(tb2 == alias3);
		Assert.assertTrue(tb2 == alias3a);
		Assert.assertTrue(tb2 == alias3b);

		List<String> tb2Aliases = Arrays.asList(getBeanFactory().getAliases("multiAliased"));
		Assert.assertEquals(4, tb2Aliases.size());
		Assert.assertTrue(tb2Aliases.contains("alias1"));
		Assert.assertTrue(tb2Aliases.contains("alias2"));
		Assert.assertTrue(tb2Aliases.contains("alias3"));
		Assert.assertTrue(tb2Aliases.contains("alias4"));
		Assert.assertTrue(beanNames.contains("multiAliased"));
		Assert.assertFalse(beanNames.contains("alias1"));
		Assert.assertFalse(beanNames.contains("alias2"));
		Assert.assertFalse(beanNames.contains("alias3"));
		Assert.assertFalse(beanNames.contains("alias4"));

		TestBean tb3 = (TestBean) getBeanFactory().getBean("aliasWithoutId1");
		TestBean alias4 = (TestBean) getBeanFactory().getBean("aliasWithoutId2");
		TestBean alias5 = (TestBean) getBeanFactory().getBean("aliasWithoutId3");
		Assert.assertTrue(tb3 == alias4);
		Assert.assertTrue(tb3 == alias5);
		List<String> tb3Aliases = Arrays.asList(getBeanFactory().getAliases("aliasWithoutId1"));
		Assert.assertEquals(2, tb3Aliases.size());
		Assert.assertTrue(tb3Aliases.contains("aliasWithoutId2"));
		Assert.assertTrue(tb3Aliases.contains("aliasWithoutId3"));
		Assert.assertTrue(beanNames.contains("aliasWithoutId1"));
		Assert.assertFalse(beanNames.contains("aliasWithoutId2"));
		Assert.assertFalse(beanNames.contains("aliasWithoutId3"));

		TestBean tb4 = (TestBean) getBeanFactory().getBean(TestBean.class.getName() + "#0");
		Assert.assertEquals(null, tb4.getName());

		Map<String, DummyReferencer> drs = getListableBeanFactory().getBeansOfType(DummyReferencer.class, false, false);
		Assert.assertEquals(5, drs.size());
		Assert.assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#0"));
		Assert.assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#1"));
		Assert.assertTrue(drs.containsKey(DummyReferencer.class.getName() + "#2"));
	}

	public void testFactoryNesting() {
		ITestBean father = (ITestBean) getBeanFactory().getBean("father");
		Assert.assertTrue("Bean from root context", father != null);

		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		Assert.assertTrue("Bean from child context", "Rod".equals(rod.getName()));
		Assert.assertTrue("Bean has external reference", rod.getSpouse() == father);

		rod = (TestBean) parent.getBean("rod");
		Assert.assertTrue("Bean from root context", "Roderick".equals(rod.getName()));
	}

	public void testFactoryReferences() {
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");

		DummyReferencer ref = (DummyReferencer) getBeanFactory().getBean("factoryReferencer");
		Assert.assertTrue(ref.getTestBean1() == ref.getTestBean2());
		Assert.assertTrue(ref.getDummyFactory() == factory);

		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("factoryReferencerWithConstructor");
		Assert.assertTrue(ref2.getTestBean1() == ref2.getTestBean2());
		Assert.assertTrue(ref2.getDummyFactory() == factory);
	}

	public void testPrototypeReferences() {
		// check that not broken by circular reference resolution mechanism
		DummyReferencer ref1 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		Assert.assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref1.getTestBean2());
		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		Assert.assertTrue("Not the same referencer", ref1 != ref2);
		Assert.assertTrue("Not referencing same bean twice", ref2.getTestBean1() != ref2.getTestBean2());
		Assert.assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref2.getTestBean1());
		Assert.assertTrue("Not referencing same bean twice", ref1.getTestBean2() != ref2.getTestBean2());
		Assert.assertTrue("Not referencing same bean twice", ref1.getTestBean1() != ref2.getTestBean2());
	}

	public void testBeanPostProcessor() throws Exception {
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		TestBean kathy = (TestBean) getBeanFactory().getBean("kathy");
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean factoryCreated = (TestBean) getBeanFactory().getBean("singletonFactory");
		Assert.assertTrue(kerry.isPostProcessed());
		Assert.assertTrue(kathy.isPostProcessed());
		Assert.assertTrue(factory.isPostProcessed());
		Assert.assertTrue(factoryCreated.isPostProcessed());
	}

	public void testEmptyValues() {
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		Assert.assertTrue("Touchy is empty", "".equals(rod.getTouchy()));
		Assert.assertTrue("Touchy is empty", "".equals(kerry.getTouchy()));
	}

	public void testCommentsAndCdataInValue() {
		TestBean bean = (TestBean) getBeanFactory().getBean("commentsInValue");
		Assert.assertEquals("Failed to handle comments and CDATA properly", "this is a <!--comment-->", bean.getName());
	}

}
