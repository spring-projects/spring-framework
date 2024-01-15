/*
 * Copyright 2002-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.config.SetFactoryBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.testfixture.beans.HasMap;
import org.springframework.beans.testfixture.beans.TestBean;
import org.springframework.core.io.ClassPathResource;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Tests for collections in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 19.12.2004
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
class XmlBeanCollectionTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	void loadBeans() {
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("collections.xml", getClass()));
	}


	@Test
	void testCollectionFactoryDefaults() throws Exception {
		ListFactoryBean listFactory = new ListFactoryBean();
		listFactory.setSourceList(new LinkedList());
		listFactory.afterPropertiesSet();
		assertThat(listFactory.getObject()).isInstanceOf(ArrayList.class);

		SetFactoryBean setFactory = new SetFactoryBean();
		setFactory.setSourceSet(new TreeSet());
		setFactory.afterPropertiesSet();
		assertThat(setFactory.getObject()).isInstanceOf(LinkedHashSet.class);

		MapFactoryBean mapFactory = new MapFactoryBean();
		mapFactory.setSourceMap(new TreeMap());
		mapFactory.afterPropertiesSet();
		assertThat(mapFactory.getObject()).isInstanceOf(LinkedHashMap.class);
	}

	@Test
	void testRefSubelement() {
		//assertTrue("5 beans in reftypes, not " + this.beanFactory.getBeanDefinitionCount(), this.beanFactory.getBeanDefinitionCount() == 5);
		TestBean jen = (TestBean) this.beanFactory.getBean("jenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertThat(jen.getSpouse()).isSameAs(dave);
	}

	@Test
	void testPropertyWithLiteralValueSubelement() {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose");
		assertThat(verbose.getName()).isEqualTo("verbose");
	}

	@Test
	void testPropertyWithIdRefLocalAttrSubelement() {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose2");
		assertThat(verbose.getName()).isEqualTo("verbose");
	}

	@Test
	void testPropertyWithIdRefBeanAttrSubelement() {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose3");
		assertThat(verbose.getName()).isEqualTo("verbose");
	}

	@Test
	void testRefSubelementsBuildCollection() {
		TestBean jen = (TestBean) this.beanFactory.getBean("jenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		TestBean rod = (TestBean) this.beanFactory.getBean("rod");

		// Must be a list to support ordering
		// Our bean doesn't modify the collection:
		// of course it could be a different copy in a real object.
		Object[] friends = rod.getFriends().toArray();
		assertThat(friends.length).isEqualTo(2);

		assertThat(friends[0]).as("First friend must be jen, not " + friends[0]).isSameAs(jen);
		assertThat(friends[1]).isSameAs(dave);
		// Should be ordered
	}

	@Test
	void testRefSubelementsBuildCollectionWithPrototypes() {
		TestBean jen = (TestBean) this.beanFactory.getBean("pJenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("pDavid");
		TestBean rod = (TestBean) this.beanFactory.getBean("pRod");

		Object[] friends = rod.getFriends().toArray();
		assertThat(friends.length).isEqualTo(2);
		assertThat(friends[0].toString()).as("First friend must be jen, not " + friends[0]).isEqualTo(jen.toString());
		assertThat(friends[0]).as("Jen not same instance").isNotSameAs(jen);
		assertThat(friends[1].toString()).isEqualTo(dave.toString());
		assertThat(friends[1]).as("Dave not same instance").isNotSameAs(dave);
		assertThat(dave.getSpouse().getName()).isEqualTo("Jen");

		TestBean rod2 = (TestBean) this.beanFactory.getBean("pRod");
		Object[] friends2 = rod2.getFriends().toArray();
		assertThat(friends2.length).isEqualTo(2);
		assertThat(friends2[0].toString()).as("First friend must be jen, not " + friends2[0]).isEqualTo(jen.toString());
		assertThat(friends2[0]).as("Jen not same instance").isNotSameAs(friends[0]);
		assertThat(friends2[1].toString()).isEqualTo(dave.toString());
		assertThat(friends2[1]).as("Dave not same instance").isNotSameAs(friends[1]);
	}

	@Test
	void testRefSubelementsBuildCollectionFromSingleElement() {
		TestBean loner = (TestBean) this.beanFactory.getBean("loner");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertThat(loner.getFriends()).containsOnly(dave);
	}

	@Test
	void testBuildCollectionFromMixtureOfReferencesAndValues() {
		MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
		assertThat(jumble.getJumble()).as("Expected 5 elements, not " + jumble.getJumble()).hasSize(5);
		List l = (List) jumble.getJumble();
		assertThat(l.get(0).equals(this.beanFactory.getBean("david"))).isTrue();
		assertThat(l.get(1).equals("literal")).isTrue();
		assertThat(l.get(2).equals(this.beanFactory.getBean("jenny"))).isTrue();
		assertThat(l.get(3).equals("rod")).isTrue();
		Object[] array = (Object[]) l.get(4);
		assertThat(array[0].equals(this.beanFactory.getBean("david"))).isTrue();
		assertThat(array[1].equals("literal2")).isTrue();
	}

	@Test
	void testInvalidBeanNameReference() {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				this.beanFactory.getBean("jumble2"))
			.withCauseInstanceOf(BeanDefinitionStoreException.class)
			.withMessageContaining("rod2");
	}

	@Test
	void testEmptyMap() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyMap");
		assertThat(hasMap.getMap()).hasSize(0);
	}

	@Test
	void testMapWithLiteralsOnly() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("literalMap");
		assertThat(hasMap.getMap()).hasSize(3);
		assertThat(hasMap.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getMap().get("fi").equals("fum")).isTrue();
		assertThat(hasMap.getMap().get("fa")).isNull();
	}

	@Test
	void testMapWithLiteralsAndReferences() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMap");
		assertThat(hasMap.getMap()).hasSize(5);
		assertThat(hasMap.getMap().get("foo")).isEqualTo(10);
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getMap().get("jenny")).isSameAs(jenny);
		assertThat(hasMap.getMap().get(5).equals("david")).isTrue();
		assertThat(hasMap.getMap().get("bar")).isInstanceOf(Long.class);
		assertThat(hasMap.getMap().get("bar")).isEqualTo(100L);
		assertThat(hasMap.getMap().get("baz")).isInstanceOf(Integer.class);
		assertThat(hasMap.getMap().get("baz")).isEqualTo(200);
	}

	@Test
	void testMapWithLiteralsAndPrototypeReferences() {
		TestBean jenny = (TestBean) this.beanFactory.getBean("pJenny");
		HasMap hasMap = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertThat(hasMap.getMap()).hasSize(2);
		assertThat(hasMap.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getMap().get("jenny").toString()).isEqualTo(jenny.toString());
		assertThat(hasMap.getMap().get("jenny")).as("Not same instance").isNotSameAs(jenny);

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertThat(hasMap2.getMap()).hasSize(2);
		assertThat(hasMap2.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap2.getMap().get("jenny").toString()).isEqualTo(jenny.toString());
		assertThat(hasMap2.getMap().get("jenny")).as("Not same instance").isNotSameAs(hasMap.getMap().get("jenny"));
	}

	@Test
	void testMapWithLiteralsReferencesAndList() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMapWithList");
		assertThat(hasMap.getMap()).hasSize(4);
		assertThat(hasMap.getMap().get(null).equals("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getMap().get("jenny")).isEqualTo(jenny);

		// Check list
		List l = (List) hasMap.getMap().get("list");
		assertThat(l).isNotNull();
		assertThat(l).hasSize(4);
		assertThat(l.get(0).equals("zero")).isTrue();
		assertThat(l).element(3).isNull();

		// Check nested map in list
		Map m = (Map) l.get(1);
		assertThat(m).isNotNull();
		assertThat(m).hasSize(2);
		assertThat(m.get("fo").equals("bar")).isTrue();
		assertThat(m.get("jen").equals(jenny)).as("Map element 'jenny' should be equal to jenny bean, not " + m.get("jen")).isTrue();

		// Check nested list in list
		l = (List) l.get(2);
		assertThat(l).isNotNull();
		assertThat(l).hasSize(2);
		assertThat(l.get(0)).isEqualTo(jenny);
		assertThat(l.get(1).equals("ba")).isTrue();

		// Check nested map
		m = (Map) hasMap.getMap().get("map");
		assertThat(m).isNotNull();
		assertThat(m).hasSize(2);
		assertThat(m.get("foo").equals("bar")).isTrue();
		assertThat(m.get("jenny").equals(jenny)).as("Map element 'jenny' should be equal to jenny bean, not " + m.get("jenny")).isTrue();
	}

	@Test
	void testEmptySet() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptySet");
		assertThat(hasMap.getSet()).hasSize(0);
	}

	@Test
	void testPopulatedSet() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("set");
		assertThat(hasMap.getSet()).hasSize(3);
		assertThat(hasMap.getSet().contains("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getSet().contains(jenny)).isTrue();
		assertThat(hasMap.getSet().contains(null)).isTrue();
		Iterator it = hasMap.getSet().iterator();
		assertThat(it.next()).isEqualTo("bar");
		assertThat(it.next()).isEqualTo(jenny);
		assertThat(it.next()).isNull();
	}

	@Test
	void testPopulatedConcurrentSet() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("concurrentSet");
		assertThat(hasMap.getConcurrentSet()).hasSize(3);
		assertThat(hasMap.getConcurrentSet().contains("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getConcurrentSet().contains(jenny)).isTrue();
		assertThat(hasMap.getConcurrentSet().contains(null)).isTrue();
	}

	@Test
	void testPopulatedIdentityMap() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("identityMap");
		assertThat(hasMap.getIdentityMap()).hasSize(2);
		HashSet set = new HashSet(hasMap.getIdentityMap().keySet());
		assertThat(set).contains("foo");
		assertThat(set).contains("jenny");
	}

	@Test
	void testEmptyProps() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyProps");
		assertThat(hasMap.getProps()).hasSize(0);
		assertThat(Properties.class).isEqualTo(hasMap.getProps().getClass());
	}

	@Test
	void testPopulatedProps() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertThat(hasMap.getProps()).hasSize(2);
		assertThat(hasMap.getProps().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getProps().get("2").equals("TWO")).isTrue();
	}

	@Test
	void testObjectArray() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("objectArray");
		assertThat(hasMap.getObjectArray().length).isEqualTo(2);
		assertThat(hasMap.getObjectArray()[0].equals("one")).isTrue();
		assertThat(hasMap.getObjectArray()[1].equals(this.beanFactory.getBean("jenny"))).isTrue();
	}

	@Test
	void testIntegerArray() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("integerArray");
		assertThat(hasMap.getIntegerArray().length).isEqualTo(3);
		assertThat(hasMap.getIntegerArray()[0]).isEqualTo(0);
		assertThat(hasMap.getIntegerArray()[1]).isEqualTo(1);
		assertThat(hasMap.getIntegerArray()[2]).isEqualTo(2);
	}

	@Test
	void testClassArray() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("classArray");
		assertThat(hasMap.getClassArray().length).isEqualTo(2);
		assertThat(hasMap.getClassArray()[0].equals(String.class)).isTrue();
		assertThat(hasMap.getClassArray()[1].equals(Exception.class)).isTrue();
	}

	@Test
	void testClassList() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("classList");
		assertThat(hasMap.getClassList()).hasSize(2);
		assertThat(hasMap.getClassList().get(0).equals(String.class)).isTrue();
		assertThat(hasMap.getClassList().get(1).equals(Exception.class)).isTrue();
	}

	@Test
	void testProps() {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertThat(hasMap.getProps()).hasSize(2);
		assertThat(hasMap.getProps().getProperty("foo")).isEqualTo("bar");
		assertThat(hasMap.getProps().getProperty("2")).isEqualTo("TWO");

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("propsViaMap");
		assertThat(hasMap2.getProps()).hasSize(2);
		assertThat(hasMap2.getProps().getProperty("foo")).isEqualTo("bar");
		assertThat(hasMap2.getProps().getProperty("2")).isEqualTo("TWO");
	}

	@Test
	void testListFactory() {
		List list = (List) this.beanFactory.getBean("listFactory");
		assertThat(list).isInstanceOf(LinkedList.class).containsExactly("bar", "jenny");
	}

	@Test
	void testPrototypeListFactory() {
		List list = (List) this.beanFactory.getBean("pListFactory");
		assertThat(list).isInstanceOf(LinkedList.class).containsExactly("bar", "jenny");
	}

	@Test
	void testSetFactory() {
		Set set = (Set) this.beanFactory.getBean("setFactory");
		assertThat(set).isInstanceOf(TreeSet.class).containsOnly("bar", "jenny");
	}

	@Test
	void testPrototypeSetFactory() {
		Set set = (Set) this.beanFactory.getBean("pSetFactory");
		assertThat(set).isInstanceOf(TreeSet.class).containsOnly("bar", "jenny");
	}

	@Test
	void testMapFactory() {
		Map map = (Map) this.beanFactory.getBean("mapFactory");
		assertThat(map).isInstanceOf(TreeMap.class).containsOnly(
				entry("foo", "bar"), entry("jen", "jenny"));
	}

	@Test
	void testPrototypeMapFactory() {
		Map map = (Map) this.beanFactory.getBean("pMapFactory");
		assertThat(map).isInstanceOf(TreeMap.class).containsOnly(
				entry("foo", "bar"), entry("jen", "jenny"));
	}

	@Test
	void testChoiceBetweenSetAndMap() {
		MapAndSet sam = (MapAndSet) this.beanFactory.getBean("setAndMap");
		assertThat(sam.getObject() instanceof Map).as("Didn't choose constructor with Map argument").isTrue();
		Map map = (Map) sam.getObject();
		assertThat(map).containsOnly(entry("key1", "val1"), entry("key2", "val2"), entry("key3", "val3"));
	}

	@Test
	void testEnumSetFactory() {
		Set set = (Set) this.beanFactory.getBean("enumSetFactory");
		assertThat(set).containsOnly("ONE", "TWO");
	}


	public static class MapAndSet {

		private Object obj;

		public MapAndSet(Map map) {
			this.obj = map;
		}

		public MapAndSet(Set set) {
			this.obj = set;
		}

		public Object getObject() {
			return obj;
		}
	}

}
