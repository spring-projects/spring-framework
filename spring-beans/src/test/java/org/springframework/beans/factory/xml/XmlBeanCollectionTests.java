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
import org.springframework.core.io.ClassPathResource;
import org.springframework.tests.sample.beans.HasMap;
import org.springframework.tests.sample.beans.TestBean;

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
public class XmlBeanCollectionTests {

	private final DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();


	@BeforeEach
	public void loadBeans() {
		new XmlBeanDefinitionReader(this.beanFactory).loadBeanDefinitions(
				new ClassPathResource("collections.xml", getClass()));
	}


	@Test
	public void testCollectionFactoryDefaults() throws Exception {
		ListFactoryBean listFactory = new ListFactoryBean();
		listFactory.setSourceList(new LinkedList());
		listFactory.afterPropertiesSet();
		boolean condition2 = listFactory.getObject() instanceof ArrayList;
		assertThat(condition2).isTrue();

		SetFactoryBean setFactory = new SetFactoryBean();
		setFactory.setSourceSet(new TreeSet());
		setFactory.afterPropertiesSet();
		boolean condition1 = setFactory.getObject() instanceof LinkedHashSet;
		assertThat(condition1).isTrue();

		MapFactoryBean mapFactory = new MapFactoryBean();
		mapFactory.setSourceMap(new TreeMap());
		mapFactory.afterPropertiesSet();
		boolean condition = mapFactory.getObject() instanceof LinkedHashMap;
		assertThat(condition).isTrue();
	}

	@Test
	public void testRefSubelement() throws Exception {
		//assertTrue("5 beans in reftypes, not " + this.beanFactory.getBeanDefinitionCount(), this.beanFactory.getBeanDefinitionCount() == 5);
		TestBean jen = (TestBean) this.beanFactory.getBean("jenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertThat(jen.getSpouse() == dave).isTrue();
	}

	@Test
	public void testPropertyWithLiteralValueSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose");
		assertThat(verbose.getName().equals("verbose")).isTrue();
	}

	@Test
	public void testPropertyWithIdRefLocalAttrSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose2");
		assertThat(verbose.getName().equals("verbose")).isTrue();
	}

	@Test
	public void testPropertyWithIdRefBeanAttrSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose3");
		assertThat(verbose.getName().equals("verbose")).isTrue();
	}

	@Test
	public void testRefSubelementsBuildCollection() throws Exception {
		TestBean jen = (TestBean) this.beanFactory.getBean("jenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		TestBean rod = (TestBean) this.beanFactory.getBean("rod");

		// Must be a list to support ordering
		// Our bean doesn't modify the collection:
		// of course it could be a different copy in a real object.
		Object[] friends = rod.getFriends().toArray();
		assertThat(friends.length == 2).isTrue();

		assertThat(friends[0] == jen).as("First friend must be jen, not " + friends[0]).isTrue();
		assertThat(friends[1] == dave).isTrue();
		// Should be ordered
	}

	@Test
	public void testRefSubelementsBuildCollectionWithPrototypes() throws Exception {
		TestBean jen = (TestBean) this.beanFactory.getBean("pJenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("pDavid");
		TestBean rod = (TestBean) this.beanFactory.getBean("pRod");

		Object[] friends = rod.getFriends().toArray();
		assertThat(friends.length == 2).isTrue();
		assertThat(friends[0].toString().equals(jen.toString())).as("First friend must be jen, not " + friends[0]).isTrue();
		assertThat(friends[0] != jen).as("Jen not same instance").isTrue();
		assertThat(friends[1].toString().equals(dave.toString())).isTrue();
		assertThat(friends[1] != dave).as("Dave not same instance").isTrue();
		assertThat(dave.getSpouse().getName()).isEqualTo("Jen");

		TestBean rod2 = (TestBean) this.beanFactory.getBean("pRod");
		Object[] friends2 = rod2.getFriends().toArray();
		assertThat(friends2.length == 2).isTrue();
		assertThat(friends2[0].toString().equals(jen.toString())).as("First friend must be jen, not " + friends2[0]).isTrue();
		assertThat(friends2[0] != friends[0]).as("Jen not same instance").isTrue();
		assertThat(friends2[1].toString().equals(dave.toString())).isTrue();
		assertThat(friends2[1] != friends[1]).as("Dave not same instance").isTrue();
	}

	@Test
	public void testRefSubelementsBuildCollectionFromSingleElement() throws Exception {
		TestBean loner = (TestBean) this.beanFactory.getBean("loner");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertThat(loner.getFriends().size() == 1).isTrue();
		assertThat(loner.getFriends().contains(dave)).isTrue();
	}

	@Test
	public void testBuildCollectionFromMixtureOfReferencesAndValues() throws Exception {
		MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
		assertThat(jumble.getJumble().size() == 5).as("Expected 5 elements, not " + jumble.getJumble().size()).isTrue();
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
	public void testInvalidBeanNameReference() throws Exception {
		assertThatExceptionOfType(BeanCreationException.class).isThrownBy(() ->
				this.beanFactory.getBean("jumble2"))
			.withCauseInstanceOf(BeanDefinitionStoreException.class)
			.withMessageContaining("rod2");
	}

	@Test
	public void testEmptyMap() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyMap");
		assertThat(hasMap.getMap().size() == 0).isTrue();
	}

	@Test
	public void testMapWithLiteralsOnly() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("literalMap");
		assertThat(hasMap.getMap().size() == 3).isTrue();
		assertThat(hasMap.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getMap().get("fi").equals("fum")).isTrue();
		assertThat(hasMap.getMap().get("fa") == null).isTrue();
	}

	@Test
	public void testMapWithLiteralsAndReferences() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMap");
		assertThat(hasMap.getMap().size() == 5).isTrue();
		assertThat(hasMap.getMap().get("foo").equals(new Integer(10))).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getMap().get("jenny") == jenny).isTrue();
		assertThat(hasMap.getMap().get(new Integer(5)).equals("david")).isTrue();
		boolean condition1 = hasMap.getMap().get("bar") instanceof Long;
		assertThat(condition1).isTrue();
		assertThat(hasMap.getMap().get("bar").equals(new Long(100))).isTrue();
		boolean condition = hasMap.getMap().get("baz") instanceof Integer;
		assertThat(condition).isTrue();
		assertThat(hasMap.getMap().get("baz").equals(new Integer(200))).isTrue();
	}

	@Test
	public void testMapWithLiteralsAndPrototypeReferences() throws Exception {
		TestBean jenny = (TestBean) this.beanFactory.getBean("pJenny");
		HasMap hasMap = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertThat(hasMap.getMap().size() == 2).isTrue();
		assertThat(hasMap.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getMap().get("jenny").toString().equals(jenny.toString())).isTrue();
		assertThat(hasMap.getMap().get("jenny") != jenny).as("Not same instance").isTrue();

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertThat(hasMap2.getMap().size() == 2).isTrue();
		assertThat(hasMap2.getMap().get("foo").equals("bar")).isTrue();
		assertThat(hasMap2.getMap().get("jenny").toString().equals(jenny.toString())).isTrue();
		assertThat(hasMap2.getMap().get("jenny") != hasMap.getMap().get("jenny")).as("Not same instance").isTrue();
	}

	@Test
	public void testMapWithLiteralsReferencesAndList() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMapWithList");
		assertThat(hasMap.getMap().size() == 4).isTrue();
		assertThat(hasMap.getMap().get(null).equals("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getMap().get("jenny").equals(jenny)).isTrue();

		// Check list
		List l = (List) hasMap.getMap().get("list");
		assertThat(l).isNotNull();
		assertThat(l.size() == 4).isTrue();
		assertThat(l.get(0).equals("zero")).isTrue();
		assertThat(l.get(3) == null).isTrue();

		// Check nested map in list
		Map m = (Map) l.get(1);
		assertThat(m).isNotNull();
		assertThat(m.size() == 2).isTrue();
		assertThat(m.get("fo").equals("bar")).isTrue();
		assertThat(m.get("jen").equals(jenny)).as("Map element 'jenny' should be equal to jenny bean, not " + m.get("jen")).isTrue();

		// Check nested list in list
		l = (List) l.get(2);
		assertThat(l).isNotNull();
		assertThat(l.size() == 2).isTrue();
		assertThat(l.get(0).equals(jenny)).isTrue();
		assertThat(l.get(1).equals("ba")).isTrue();

		// Check nested map
		m = (Map) hasMap.getMap().get("map");
		assertThat(m).isNotNull();
		assertThat(m.size() == 2).isTrue();
		assertThat(m.get("foo").equals("bar")).isTrue();
		assertThat(m.get("jenny").equals(jenny)).as("Map element 'jenny' should be equal to jenny bean, not " + m.get("jenny")).isTrue();
	}

	@Test
	public void testEmptySet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptySet");
		assertThat(hasMap.getSet().size() == 0).isTrue();
	}

	@Test
	public void testPopulatedSet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("set");
		assertThat(hasMap.getSet().size() == 3).isTrue();
		assertThat(hasMap.getSet().contains("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getSet().contains(jenny)).isTrue();
		assertThat(hasMap.getSet().contains(null)).isTrue();
		Iterator it = hasMap.getSet().iterator();
		assertThat(it.next()).isEqualTo("bar");
		assertThat(it.next()).isEqualTo(jenny);
		assertThat(it.next()).isEqualTo(null);
	}

	@Test
	public void testPopulatedConcurrentSet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("concurrentSet");
		assertThat(hasMap.getConcurrentSet().size() == 3).isTrue();
		assertThat(hasMap.getConcurrentSet().contains("bar")).isTrue();
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertThat(hasMap.getConcurrentSet().contains(jenny)).isTrue();
		assertThat(hasMap.getConcurrentSet().contains(null)).isTrue();
	}

	@Test
	public void testPopulatedIdentityMap() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("identityMap");
		assertThat(hasMap.getIdentityMap().size() == 2).isTrue();
		HashSet set = new HashSet(hasMap.getIdentityMap().keySet());
		assertThat(set.contains("foo")).isTrue();
		assertThat(set.contains("jenny")).isTrue();
	}

	@Test
	public void testEmptyProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyProps");
		assertThat(hasMap.getProps().size() == 0).isTrue();
		assertThat(Properties.class).isEqualTo(hasMap.getProps().getClass());
	}

	@Test
	public void testPopulatedProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertThat(hasMap.getProps().size() == 2).isTrue();
		assertThat(hasMap.getProps().get("foo").equals("bar")).isTrue();
		assertThat(hasMap.getProps().get("2").equals("TWO")).isTrue();
	}

	@Test
	public void testObjectArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("objectArray");
		assertThat(hasMap.getObjectArray().length == 2).isTrue();
		assertThat(hasMap.getObjectArray()[0].equals("one")).isTrue();
		assertThat(hasMap.getObjectArray()[1].equals(this.beanFactory.getBean("jenny"))).isTrue();
	}

	@Test
	public void testIntegerArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("integerArray");
		assertThat(hasMap.getIntegerArray().length == 3).isTrue();
		assertThat(hasMap.getIntegerArray()[0].intValue() == 0).isTrue();
		assertThat(hasMap.getIntegerArray()[1].intValue() == 1).isTrue();
		assertThat(hasMap.getIntegerArray()[2].intValue() == 2).isTrue();
	}

	@Test
	public void testClassArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("classArray");
		assertThat(hasMap.getClassArray().length == 2).isTrue();
		assertThat(hasMap.getClassArray()[0].equals(String.class)).isTrue();
		assertThat(hasMap.getClassArray()[1].equals(Exception.class)).isTrue();
	}

	@Test
	public void testClassList() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("classList");
		assertThat(hasMap.getClassList().size()== 2).isTrue();
		assertThat(hasMap.getClassList().get(0).equals(String.class)).isTrue();
		assertThat(hasMap.getClassList().get(1).equals(Exception.class)).isTrue();
	}

	@Test
	public void testProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertThat(hasMap.getProps().size()).isEqualTo(2);
		assertThat(hasMap.getProps().getProperty("foo")).isEqualTo("bar");
		assertThat(hasMap.getProps().getProperty("2")).isEqualTo("TWO");

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("propsViaMap");
		assertThat(hasMap2.getProps().size()).isEqualTo(2);
		assertThat(hasMap2.getProps().getProperty("foo")).isEqualTo("bar");
		assertThat(hasMap2.getProps().getProperty("2")).isEqualTo("TWO");
	}

	@Test
	public void testListFactory() throws Exception {
		List list = (List) this.beanFactory.getBean("listFactory");
		boolean condition = list instanceof LinkedList;
		assertThat(condition).isTrue();
		assertThat(list.size() == 2).isTrue();
		assertThat(list.get(0)).isEqualTo("bar");
		assertThat(list.get(1)).isEqualTo("jenny");
	}

	@Test
	public void testPrototypeListFactory() throws Exception {
		List list = (List) this.beanFactory.getBean("pListFactory");
		boolean condition = list instanceof LinkedList;
		assertThat(condition).isTrue();
		assertThat(list.size() == 2).isTrue();
		assertThat(list.get(0)).isEqualTo("bar");
		assertThat(list.get(1)).isEqualTo("jenny");
	}

	@Test
	public void testSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("setFactory");
		boolean condition = set instanceof TreeSet;
		assertThat(condition).isTrue();
		assertThat(set.size() == 2).isTrue();
		assertThat(set.contains("bar")).isTrue();
		assertThat(set.contains("jenny")).isTrue();
	}

	@Test
	public void testPrototypeSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("pSetFactory");
		boolean condition = set instanceof TreeSet;
		assertThat(condition).isTrue();
		assertThat(set.size() == 2).isTrue();
		assertThat(set.contains("bar")).isTrue();
		assertThat(set.contains("jenny")).isTrue();
	}

	@Test
	public void testMapFactory() throws Exception {
		Map map = (Map) this.beanFactory.getBean("mapFactory");
		boolean condition = map instanceof TreeMap;
		assertThat(condition).isTrue();
		assertThat(map.size() == 2).isTrue();
		assertThat(map.get("foo")).isEqualTo("bar");
		assertThat(map.get("jen")).isEqualTo("jenny");
	}

	@Test
	public void testPrototypeMapFactory() throws Exception {
		Map map = (Map) this.beanFactory.getBean("pMapFactory");
		boolean condition = map instanceof TreeMap;
		assertThat(condition).isTrue();
		assertThat(map.size() == 2).isTrue();
		assertThat(map.get("foo")).isEqualTo("bar");
		assertThat(map.get("jen")).isEqualTo("jenny");
	}

	@Test
	public void testChoiceBetweenSetAndMap() {
		MapAndSet sam = (MapAndSet) this.beanFactory.getBean("setAndMap");
		boolean condition = sam.getObject() instanceof Map;
		assertThat(condition).as("Didn't choose constructor with Map argument").isTrue();
		Map map = (Map) sam.getObject();
		assertThat(map.size()).isEqualTo(3);
		assertThat(map.get("key1")).isEqualTo("val1");
		assertThat(map.get("key2")).isEqualTo("val2");
		assertThat(map.get("key3")).isEqualTo("val3");
	}

	@Test
	public void testEnumSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("enumSetFactory");
		assertThat(set.size() == 2).isTrue();
		assertThat(set.contains("ONE")).isTrue();
		assertThat(set.contains("TWO")).isTrue();
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
