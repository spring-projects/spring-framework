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

package org.springframework.beans.factory.xml;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.FieldRetrievingFactoryBean;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.beans.factory.parsing.ComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.CollectingReaderEventListener;
import org.springframework.beans.testfixture.beans.CustomEnum;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.LinkedCaseInsensitiveMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Rob Harrop
 * @author Juergen Hoeller
 * @author Mark Fisher
 */
@SuppressWarnings("rawtypes")
class UtilNamespaceHandlerTests {

	private DefaultListableBeanFactory beanFactory;

	private CollectingReaderEventListener listener = new CollectingReaderEventListener();


	@BeforeEach
	void setUp() {
		this.beanFactory = new DefaultListableBeanFactory();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader(this.beanFactory);
		reader.setEventListener(this.listener);
		reader.loadBeanDefinitions(new ClassPathResource("testUtilNamespace.xml", getClass()));
	}


	@Test
	void testConstant() {
		Integer min = (Integer) this.beanFactory.getBean("min");
		assertThat(min).isEqualTo(Integer.MIN_VALUE);
	}

	@Test
	void testConstantWithDefaultName() {
		Integer max = (Integer) this.beanFactory.getBean("java.lang.Integer.MAX_VALUE");
		assertThat(max).isEqualTo(Integer.MAX_VALUE);
	}

	@Test
	void testEvents() {
		ComponentDefinition propertiesComponent = this.listener.getComponentDefinition("myProperties");
		assertThat(propertiesComponent).as("Event for 'myProperties' not sent").isNotNull();
		AbstractBeanDefinition propertiesBean = (AbstractBeanDefinition) propertiesComponent.getBeanDefinitions()[0];
		assertThat(propertiesBean.getBeanClass()).as("Incorrect BeanDefinition").isEqualTo(PropertiesFactoryBean.class);

		ComponentDefinition constantComponent = this.listener.getComponentDefinition("min");
		assertThat(propertiesComponent).as("Event for 'min' not sent").isNotNull();
		AbstractBeanDefinition constantBean = (AbstractBeanDefinition) constantComponent.getBeanDefinitions()[0];
		assertThat(constantBean.getBeanClass()).as("Incorrect BeanDefinition").isEqualTo(FieldRetrievingFactoryBean.class);
	}

	@Test
	void testNestedProperties() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		Properties props = bean.getSomeProperties();
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
	}

	@Test
	void testPropertyPath() {
		String name = (String) this.beanFactory.getBean("name");
		assertThat(name).isEqualTo("Rob Harrop");
	}

	@Test
	void testNestedPropertyPath() {
		TestBean bean = (TestBean) this.beanFactory.getBean("testBean");
		assertThat(bean.getName()).isEqualTo("Rob Harrop");
	}

	@Test
	void testSimpleMap() {
		Map<?, ?> map = (Map) this.beanFactory.getBean("simpleMap");
		assertThat(map.get("foo")).isEqualTo("bar");
		Map<?, ?> map2 = (Map) this.beanFactory.getBean("simpleMap");
		assertThat(map).isSameAs(map2);
	}

	@Test
	void testScopedMap() {
		Map<?, ?> map = (Map) this.beanFactory.getBean("scopedMap");
		assertThat(map.get("foo")).isEqualTo("bar");
		Map<?, ?> map2 = (Map) this.beanFactory.getBean("scopedMap");
		assertThat(map2.get("foo")).isEqualTo("bar");
		assertThat(map).isNotSameAs(map2);
	}

	@Test
	void testSimpleList() {
		assertThat(this.beanFactory.getBean("simpleList"))
				.asInstanceOf(InstanceOfAssertFactories.LIST).element(0).isEqualTo("Rob Harrop");
		assertThat(this.beanFactory.getBean("simpleList"))
				.isSameAs(this.beanFactory.getBean("simpleList"));
	}

	@Test
	void testScopedList() {
		assertThat(this.beanFactory.getBean("scopedList"))
				.asInstanceOf(InstanceOfAssertFactories.LIST).element(0).isEqualTo("Rob Harrop");
		assertThat(this.beanFactory.getBean("scopedList"))
				.asInstanceOf(InstanceOfAssertFactories.LIST).element(0).isEqualTo("Rob Harrop");
		assertThat(this.beanFactory.getBean("scopedList"))
				.isNotSameAs(this.beanFactory.getBean("scopedList"));
	}

	@Test
	void testSimpleSet() {
		assertThat(this.beanFactory.getBean("simpleSet")).isInstanceOf(Set.class)
				.asInstanceOf(InstanceOfAssertFactories.collection(String.class))
				.containsOnly("Rob Harrop");
		assertThat(this.beanFactory.getBean("simpleSet"))
				.isSameAs(this.beanFactory.getBean("simpleSet"));
	}

	@Test
	void testScopedSet() {
		assertThat(this.beanFactory.getBean("scopedSet")).isInstanceOf(Set.class)
				.asInstanceOf(InstanceOfAssertFactories.collection(String.class))
				.containsOnly("Rob Harrop");
		assertThat(this.beanFactory.getBean("scopedSet")).isInstanceOf(Set.class)
				.asInstanceOf(InstanceOfAssertFactories.collection(String.class))
				.containsOnly("Rob Harrop");
		assertThat(this.beanFactory.getBean("scopedSet"))
				.isNotSameAs(this.beanFactory.getBean("scopedSet"));
	}

	@Test
	void testMapWithRef() {
		Map<?, ?> map = (Map) this.beanFactory.getBean("mapWithRef");
		assertThat(map).isInstanceOf(TreeMap.class);
		assertThat(map.get("bean")).isEqualTo(this.beanFactory.getBean("testBean"));
	}

	@Test
	void testMapWithTypes() {
		Map<?, ?> map = (Map) this.beanFactory.getBean("mapWithTypes");
		assertThat(map).isInstanceOf(LinkedCaseInsensitiveMap.class);
		assertThat(map.get("bean")).isEqualTo(this.beanFactory.getBean("testBean"));
	}

	@Test
	void testNestedCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");

		assertThat(bean.getSomeList()).singleElement().isEqualTo("foo");
		assertThat(bean.getSomeSet()).singleElement().isEqualTo("bar");
		assertThat(bean.getSomeMap()).hasSize(1).allSatisfy((key, nested) -> {
			assertThat(key).isEqualTo("foo");
			assertThat(nested).isInstanceOf(Set.class).asInstanceOf(
					InstanceOfAssertFactories.collection(String.class)).containsOnly("bar");
		});

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCollectionsBean");
		assertThat(bean2.getSomeList()).isEqualTo(bean.getSomeList());
		assertThat(bean2.getSomeSet()).isEqualTo(bean.getSomeSet());
		assertThat(bean2.getSomeMap()).isEqualTo(bean.getSomeMap());
		assertThat(bean.getSomeList()).isNotSameAs(bean2.getSomeList());
		assertThat(bean.getSomeSet()).isNotSameAs(bean2.getSomeSet());
		assertThat(bean.getSomeMap()).isNotSameAs(bean2.getSomeMap());
	}

	@Test
	void testNestedShortcutCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");

		assertThat(bean.getStringArray()).containsExactly("fooStr");
		assertThat(bean.getSomeList()).singleElement().isEqualTo("foo");
		assertThat(bean.getSomeSet()).singleElement().isEqualTo("bar");

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedShortcutCollections");
		assertThat(Arrays.equals(bean.getStringArray(), bean2.getStringArray())).isTrue();
		assertThat(bean.getStringArray()).isNotSameAs(bean2.getStringArray());
		assertThat(bean2.getSomeList()).isEqualTo(bean.getSomeList());
		assertThat(bean2.getSomeSet()).isEqualTo(bean.getSomeSet());
		assertThat(bean.getSomeList()).isNotSameAs(bean2.getSomeList());
		assertThat(bean.getSomeSet()).isNotSameAs(bean2.getSomeSet());
	}

	@Test
	void testNestedInCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");

		assertThat(bean.getSomeList()).singleElement().isEqualTo(Integer.MIN_VALUE);
		assertThat(bean.getSomeSet()).asInstanceOf(InstanceOfAssertFactories.collection(Thread.State.class))
				.containsOnly(Thread.State.NEW,Thread.State.RUNNABLE);
		assertThat(bean.getSomeMap()).hasSize(1).allSatisfy((key, value) -> {
			assertThat(key).isEqualTo("min");
			assertThat(value).isEqualTo(CustomEnum.VALUE_1);
		});

		TestBean bean2 = (TestBean) this.beanFactory.getBean("nestedCustomTagBean");
		assertThat(bean2.getSomeList()).isEqualTo(bean.getSomeList());
		assertThat(bean2.getSomeSet()).isEqualTo(bean.getSomeSet());
		assertThat(bean2.getSomeMap()).isEqualTo(bean.getSomeMap());
		assertThat(bean.getSomeList()).isNotSameAs(bean2.getSomeList());
		assertThat(bean.getSomeSet()).isNotSameAs(bean2.getSomeSet());
		assertThat(bean.getSomeMap()).isNotSameAs(bean2.getSomeMap());
	}

	@Test
	void testCircularCollections() {
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionsBean");

		assertThat(bean.getSomeList()).singleElement().isEqualTo(bean);
		assertThat(bean.getSomeSet()).singleElement().isEqualTo(bean);
		assertThat(bean.getSomeMap()).hasSize(1).allSatisfy((key, value) -> {
			assertThat(key).isEqualTo("foo");
			assertThat(value).isEqualTo(bean);
		});
	}

	@Test
	void testCircularCollectionBeansStartingWithList() {
		this.beanFactory.getBean("circularList");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List<?> list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isTrue();
		assertThat(list).singleElement().isEqualTo(bean);

		Set<?> set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
		assertThat(set).singleElement().isEqualTo(bean);

		Map<?, ?> map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
		assertThat(map).hasSize(1).allSatisfy((key, value) -> {
			assertThat(key).isEqualTo("foo");
			assertThat(value).isEqualTo(bean);
		});
	}

	@Test
	void testCircularCollectionBeansStartingWithSet() {
		this.beanFactory.getBean("circularSet");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List<?> list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
		assertThat(list).singleElement().isEqualTo(bean);

		Set<?> set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isTrue();
		assertThat(set).singleElement().isEqualTo(bean);

		Map<?, ?> map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isFalse();
		assertThat(map).hasSize(1).allSatisfy((key, value) -> {
			assertThat(key).isEqualTo("foo");
			assertThat(value).isEqualTo(bean);
		});
	}

	@Test
	void testCircularCollectionBeansStartingWithMap() {
		this.beanFactory.getBean("circularMap");
		TestBean bean = (TestBean) this.beanFactory.getBean("circularCollectionBeansBean");

		List<?> list = bean.getSomeList();
		assertThat(Proxy.isProxyClass(list.getClass())).isFalse();
		assertThat(list).singleElement().isEqualTo(bean);

		Set<?> set = bean.getSomeSet();
		assertThat(Proxy.isProxyClass(set.getClass())).isFalse();
		assertThat(set).singleElement().isEqualTo(bean);

		Map<?, ?> map = bean.getSomeMap();
		assertThat(Proxy.isProxyClass(map.getClass())).isTrue();
		assertThat(map).hasSize(1).allSatisfy((key, value) -> {
			assertThat(key).isEqualTo("foo");
			assertThat(value).isEqualTo(bean);
		});
	}

	@Test
	void testNestedInConstructor() {
		TestBean bean = (TestBean) this.beanFactory.getBean("constructedTestBean");
		assertThat(bean.getName()).isEqualTo("Rob Harrop");
	}

	@Test
	void testLoadProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").doesNotContainKey("foo2");
		Properties props2 = (Properties) this.beanFactory.getBean("myProperties");
		assertThat(props).isSameAs(props2);
	}

	@Test
	void testScopedProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").doesNotContainKey("foo2");
		Properties props2 = (Properties) this.beanFactory.getBean("myScopedProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").doesNotContainKey("foo2");
		assertThat(props).isNotSameAs(props2);
	}

	@Test
	void testLocalProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myLocalProperties");
		assertThat(props).as("Incorrect property value").doesNotContainKey("foo");
		assertThat(props).as("Incorrect property value").containsEntry("foo2", "bar2");
	}

	@Test
	void testMergedProperties() {
		Properties props = (Properties) this.beanFactory.getBean("myMergedProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").containsEntry("foo2", "bar2");
	}

	@Test
	void testLocalOverrideDefault() {
		Properties props = (Properties) this.beanFactory.getBean("defaultLocalOverrideProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").containsEntry("foo2", "local2");
	}

	@Test
	void testLocalOverrideFalse() {
		Properties props = (Properties) this.beanFactory.getBean("falseLocalOverrideProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "bar");
		assertThat(props).as("Incorrect property value").containsEntry("foo2", "local2");
	}

	@Test
	void testLocalOverrideTrue() {
		Properties props = (Properties) this.beanFactory.getBean("trueLocalOverrideProperties");
		assertThat(props).as("Incorrect property value").containsEntry("foo", "local");
		assertThat(props).as("Incorrect property value").containsEntry("foo2", "local2");
	}

}
