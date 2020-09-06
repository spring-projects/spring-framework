/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.beans.factory.xml;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.testfixture.beans.ITestBean;
import org.springframework.beans.testfixture.beans.LifecycleBean;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.beans.testfixture.beans.factory.DummyFactory;
import org.springframework.beans.testfixture.factory.xml.AbstractListableBeanFactoryTests;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
@SuppressWarnings({"rawtypes", "unchecked"})
public class XmlListableBeanFactoryTests extends AbstractListableBeanFactoryTests {

	private DefaultListableBeanFactory parent;

	private DefaultListableBeanFactory factory;


	@BeforeEach
	public void setup() {
		parent = new DefaultListableBeanFactory();

		Map map = new HashMap();
		map.put("name", "Albert");
		RootBeanDefinition bd1 = new RootBeanDefinition(TestBean.class);
		bd1.setPropertyValues(new MutablePropertyValues(map));
		parent.registerBeanDefinition("father", bd1);

		map = new HashMap();
		map.put("name", "Roderick");
		RootBeanDefinition bd2 = new RootBeanDefinition(TestBean.class);
		bd2.setPropertyValues(new MutablePropertyValues(map));
		parent.registerBeanDefinition("rod", bd2);

		this.factory = new DefaultListableBeanFactory(parent);
		new XmlBeanDefinitionReader(this.factory).loadBeanDefinitions(new ClassPathResource("test.xml", getClass()));

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
		// this.factory.preInstantiateSingletons();
	}

	@Override
	protected BeanFactory getBeanFactory() {
		return factory;
	}


	@Test
	@Override
	public void count() {
		assertCount(24);
	}

	@Test
	public void beanCount() {
		assertTestBeanCount(13);
	}

	@Test
	public void lifecycleMethods() {
		LifecycleBean bean = (LifecycleBean) getBeanFactory().getBean("lifecycle");
		bean.businessMethod();
	}

	@Test
	public void protectedLifecycleMethods() {
		ProtectedLifecycleBean bean = (ProtectedLifecycleBean) getBeanFactory().getBean("protectedLifecycle");
		bean.businessMethod();
	}

	@Test
	public void descriptionButNoProperties() {
		TestBean validEmpty = (TestBean) getBeanFactory().getBean("validEmptyWithDescription");
		assertThat(validEmpty.getAge()).isEqualTo(0);
	}

	/**
	 * Test that properties with name as well as id creating an alias up front.
	 */
	@Test
	public void autoAliasing() {
		List beanNames = Arrays.asList(getListableBeanFactory().getBeanDefinitionNames());

		TestBean tb1 = (TestBean) getBeanFactory().getBean("aliased");
		TestBean alias1 = (TestBean) getBeanFactory().getBean("myalias");
		assertThat(tb1 == alias1).isTrue();
		List tb1Aliases = Arrays.asList(getBeanFactory().getAliases("aliased"));
		assertThat(tb1Aliases.size()).isEqualTo(2);
		assertThat(tb1Aliases.contains("myalias")).isTrue();
		assertThat(tb1Aliases.contains("youralias")).isTrue();
		assertThat(beanNames.contains("aliased")).isTrue();
		assertThat(beanNames.contains("myalias")).isFalse();
		assertThat(beanNames.contains("youralias")).isFalse();

		TestBean tb2 = (TestBean) getBeanFactory().getBean("multiAliased");
		TestBean alias2 = (TestBean) getBeanFactory().getBean("alias1");
		TestBean alias3 = (TestBean) getBeanFactory().getBean("alias2");
		TestBean alias3a = (TestBean) getBeanFactory().getBean("alias3");
		TestBean alias3b = (TestBean) getBeanFactory().getBean("alias4");
		assertThat(tb2 == alias2).isTrue();
		assertThat(tb2 == alias3).isTrue();
		assertThat(tb2 == alias3a).isTrue();
		assertThat(tb2 == alias3b).isTrue();

		List tb2Aliases = Arrays.asList(getBeanFactory().getAliases("multiAliased"));
		assertThat(tb2Aliases.size()).isEqualTo(4);
		assertThat(tb2Aliases.contains("alias1")).isTrue();
		assertThat(tb2Aliases.contains("alias2")).isTrue();
		assertThat(tb2Aliases.contains("alias3")).isTrue();
		assertThat(tb2Aliases.contains("alias4")).isTrue();
		assertThat(beanNames.contains("multiAliased")).isTrue();
		assertThat(beanNames.contains("alias1")).isFalse();
		assertThat(beanNames.contains("alias2")).isFalse();
		assertThat(beanNames.contains("alias3")).isFalse();
		assertThat(beanNames.contains("alias4")).isFalse();

		TestBean tb3 = (TestBean) getBeanFactory().getBean("aliasWithoutId1");
		TestBean alias4 = (TestBean) getBeanFactory().getBean("aliasWithoutId2");
		TestBean alias5 = (TestBean) getBeanFactory().getBean("aliasWithoutId3");
		assertThat(tb3 == alias4).isTrue();
		assertThat(tb3 == alias5).isTrue();
		List tb3Aliases = Arrays.asList(getBeanFactory().getAliases("aliasWithoutId1"));
		assertThat(tb3Aliases.size()).isEqualTo(2);
		assertThat(tb3Aliases.contains("aliasWithoutId2")).isTrue();
		assertThat(tb3Aliases.contains("aliasWithoutId3")).isTrue();
		assertThat(beanNames.contains("aliasWithoutId1")).isTrue();
		assertThat(beanNames.contains("aliasWithoutId2")).isFalse();
		assertThat(beanNames.contains("aliasWithoutId3")).isFalse();

		TestBean tb4 = (TestBean) getBeanFactory().getBean(TestBean.class.getName() + "#0");
		assertThat(tb4.getName()).isEqualTo(null);

		Map drs = getListableBeanFactory().getBeansOfType(DummyReferencer.class, false, false);
		assertThat(drs.size()).isEqualTo(5);
		assertThat(drs.containsKey(DummyReferencer.class.getName() + "#0")).isTrue();
		assertThat(drs.containsKey(DummyReferencer.class.getName() + "#1")).isTrue();
		assertThat(drs.containsKey(DummyReferencer.class.getName() + "#2")).isTrue();
	}

	@Test
	public void factoryNesting() {
		ITestBean father = (ITestBean) getBeanFactory().getBean("father");
		assertThat(father != null).as("Bean from root context").isTrue();

		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		assertThat("Rod".equals(rod.getName())).as("Bean from child context").isTrue();
		assertThat(rod.getSpouse() == father).as("Bean has external reference").isTrue();

		rod = (TestBean) parent.getBean("rod");
		assertThat("Roderick".equals(rod.getName())).as("Bean from root context").isTrue();
	}

	@Test
	public void factoryReferences() {
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");

		DummyReferencer ref = (DummyReferencer) getBeanFactory().getBean("factoryReferencer");
		assertThat(ref.getTestBean1() == ref.getTestBean2()).isTrue();
		assertThat(ref.getDummyFactory() == factory).isTrue();

		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("factoryReferencerWithConstructor");
		assertThat(ref2.getTestBean1() == ref2.getTestBean2()).isTrue();
		assertThat(ref2.getDummyFactory() == factory).isTrue();
	}

	@Test
	public void prototypeReferences() {
		// check that not broken by circular reference resolution mechanism
		DummyReferencer ref1 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		assertThat(ref1.getTestBean1() != ref1.getTestBean2()).as("Not referencing same bean twice").isTrue();
		DummyReferencer ref2 = (DummyReferencer) getBeanFactory().getBean("prototypeReferencer");
		assertThat(ref1 != ref2).as("Not the same referencer").isTrue();
		assertThat(ref2.getTestBean1() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
		assertThat(ref1.getTestBean1() != ref2.getTestBean1()).as("Not referencing same bean twice").isTrue();
		assertThat(ref1.getTestBean2() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
		assertThat(ref1.getTestBean1() != ref2.getTestBean2()).as("Not referencing same bean twice").isTrue();
	}

	@Test
	public void beanPostProcessor() {
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		TestBean kathy = (TestBean) getBeanFactory().getBean("kathy");
		DummyFactory factory = (DummyFactory) getBeanFactory().getBean("&singletonFactory");
		TestBean factoryCreated = (TestBean) getBeanFactory().getBean("singletonFactory");
		assertThat(kerry.isPostProcessed()).isTrue();
		assertThat(kathy.isPostProcessed()).isTrue();
		assertThat(factory.isPostProcessed()).isTrue();
		assertThat(factoryCreated.isPostProcessed()).isTrue();
	}

	@Test
	public void emptyValues() {
		TestBean rod = (TestBean) getBeanFactory().getBean("rod");
		TestBean kerry = (TestBean) getBeanFactory().getBean("kerry");
		assertThat("".equals(rod.getTouchy())).as("Touchy is empty").isTrue();
		assertThat("".equals(kerry.getTouchy())).as("Touchy is empty").isTrue();
	}

	@Test
	public void commentsAndCdataInValue() {
		TestBean bean = (TestBean) getBeanFactory().getBean("commentsInValue");
		assertThat(bean.getName()).as("Failed to handle comments and CDATA properly").isEqualTo("this is a <!--comment-->");
	}

}
