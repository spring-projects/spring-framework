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

package org.springframework.beans.factory.xml;

import java.util.ArrayList;
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
import java.util.IdentityHashMap;
import java.util.HashSet;
import java.util.concurrent.CopyOnWriteArraySet;

import org.junit.Test;
import static org.junit.Assert.*;
import test.beans.TestBean;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ListFactoryBean;
import org.springframework.beans.factory.config.MapFactoryBean;
import org.springframework.beans.factory.config.SetFactoryBean;
import org.springframework.core.io.ClassPathResource;

/**
 * Tests for collections in XML bean definitions.
 *
 * @author Juergen Hoeller
 * @author Chris Beams
 * @since 19.12.2004
 */
public class XmlBeanCollectionTests {

	private final XmlBeanFactory beanFactory;

	public XmlBeanCollectionTests() {
		this.beanFactory = new XmlBeanFactory(new ClassPathResource("collections.xml", getClass()));
	}

	@Test
	public void testCollectionFactoryDefaults() throws Exception {
		ListFactoryBean listFactory = new ListFactoryBean();
		listFactory.setSourceList(new LinkedList());
		listFactory.afterPropertiesSet();
		assertTrue(listFactory.getObject() instanceof ArrayList);

		SetFactoryBean setFactory = new SetFactoryBean();
		setFactory.setSourceSet(new TreeSet());
		setFactory.afterPropertiesSet();
		assertTrue(setFactory.getObject() instanceof LinkedHashSet);

		MapFactoryBean mapFactory = new MapFactoryBean();
		mapFactory.setSourceMap(new TreeMap());
		mapFactory.afterPropertiesSet();
		assertTrue(mapFactory.getObject() instanceof LinkedHashMap);
	}

	@Test
	public void testRefSubelement() throws Exception {
		//assertTrue("5 beans in reftypes, not " + this.beanFactory.getBeanDefinitionCount(), this.beanFactory.getBeanDefinitionCount() == 5);
		TestBean jen = (TestBean) this.beanFactory.getBean("jenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertTrue(jen.getSpouse() == dave);
	}

	@Test
	public void testPropertyWithLiteralValueSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose");
		assertTrue(verbose.getName().equals("verbose"));
	}

	@Test
	public void testPropertyWithIdRefLocalAttrSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose2");
		assertTrue(verbose.getName().equals("verbose"));
	}

	@Test
	public void testPropertyWithIdRefBeanAttrSubelement() throws Exception {
		TestBean verbose = (TestBean) this.beanFactory.getBean("verbose3");
		assertTrue(verbose.getName().equals("verbose"));
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
		assertTrue(friends.length == 2);

		assertTrue("First friend must be jen, not " + friends[0], friends[0] == jen);
		assertTrue(friends[1] == dave);
		// Should be ordered
	}

	@Test
	public void testRefSubelementsBuildCollectionWithPrototypes() throws Exception {
		TestBean jen = (TestBean) this.beanFactory.getBean("pJenny");
		TestBean dave = (TestBean) this.beanFactory.getBean("pDavid");
		TestBean rod = (TestBean) this.beanFactory.getBean("pRod");

		Object[] friends = rod.getFriends().toArray();
		assertTrue(friends.length == 2);
		assertTrue("First friend must be jen, not " + friends[0],
				friends[0].toString().equals(jen.toString()));
		assertTrue("Jen not same instance", friends[0] != jen);
		assertTrue(friends[1].toString().equals(dave.toString()));
		assertTrue("Dave not same instance", friends[1] != dave);
		assertEquals("Jen", dave.getSpouse().getName());

		TestBean rod2 = (TestBean) this.beanFactory.getBean("pRod");
		Object[] friends2 = rod2.getFriends().toArray();
		assertTrue(friends2.length == 2);
		assertTrue("First friend must be jen, not " + friends2[0],
				friends2[0].toString().equals(jen.toString()));
		assertTrue("Jen not same instance", friends2[0] != friends[0]);
		assertTrue(friends2[1].toString().equals(dave.toString()));
		assertTrue("Dave not same instance", friends2[1] != friends[1]);
	}

	@Test
	public void testRefSubelementsBuildCollectionFromSingleElement() throws Exception {
		TestBean loner = (TestBean) this.beanFactory.getBean("loner");
		TestBean dave = (TestBean) this.beanFactory.getBean("david");
		assertTrue(loner.getFriends().size() == 1);
		assertTrue(loner.getFriends().contains(dave));
	}

	@Test
	public void testBuildCollectionFromMixtureOfReferencesAndValues() throws Exception {
		MixedCollectionBean jumble = (MixedCollectionBean) this.beanFactory.getBean("jumble");
		assertTrue("Expected 5 elements, not " + jumble.getJumble().size(),
				jumble.getJumble().size() == 5);
		List l = (List) jumble.getJumble();
		assertTrue(l.get(0).equals(this.beanFactory.getBean("david")));
		assertTrue(l.get(1).equals("literal"));
		assertTrue(l.get(2).equals(this.beanFactory.getBean("jenny")));
		assertTrue(l.get(3).equals("rod"));
		Object[] array = (Object[]) l.get(4);
		assertTrue(array[0].equals(this.beanFactory.getBean("david")));
		assertTrue(array[1].equals("literal2"));
	}

	@Test
	public void testInvalidBeanNameReference() throws Exception {
		try {
			this.beanFactory.getBean("jumble2");
			fail("Should have thrown BeanCreationException");
		}
		catch (BeanCreationException ex) {
			assertTrue(ex.getCause() instanceof BeanDefinitionStoreException);
			assertTrue(ex.getCause().getMessage().contains("rod2"));
		}
	}

	@Test
	public void testEmptyMap() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyMap");
		assertTrue(hasMap.getMap().size() == 0);
	}

	@Test
	public void testMapWithLiteralsOnly() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("literalMap");
		assertTrue(hasMap.getMap().size() == 3);
		assertTrue(hasMap.getMap().get("foo").equals("bar"));
		assertTrue(hasMap.getMap().get("fi").equals("fum"));
		assertTrue(hasMap.getMap().get("fa") == null);
	}

	@Test
	public void testMapWithLiteralsAndReferences() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMap");
		assertTrue(hasMap.getMap().size() == 3);
		assertTrue(hasMap.getMap().get("foo").equals(new Integer(10)));
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertTrue(hasMap.getMap().get("jenny") == jenny);
		assertTrue(hasMap.getMap().get(new Integer(5)).equals("david"));
	}

	@Test
	public void testMapWithLiteralsAndPrototypeReferences() throws Exception {
		TestBean jenny = (TestBean) this.beanFactory.getBean("pJenny");
		HasMap hasMap = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertTrue(hasMap.getMap().size() == 2);
		assertTrue(hasMap.getMap().get("foo").equals("bar"));
		assertTrue(hasMap.getMap().get("jenny").toString().equals(jenny.toString()));
		assertTrue("Not same instance", hasMap.getMap().get("jenny") != jenny);

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("pMixedMap");
		assertTrue(hasMap2.getMap().size() == 2);
		assertTrue(hasMap2.getMap().get("foo").equals("bar"));
		assertTrue(hasMap2.getMap().get("jenny").toString().equals(jenny.toString()));
		assertTrue("Not same instance", hasMap2.getMap().get("jenny") != hasMap.getMap().get("jenny"));
	}

	@Test
	public void testMapWithLiteralsReferencesAndList() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("mixedMapWithList");
		assertTrue(hasMap.getMap().size() == 4);
		assertTrue(hasMap.getMap().get(null).equals("bar"));
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertTrue(hasMap.getMap().get("jenny").equals(jenny));

		// Check list
		List l = (List) hasMap.getMap().get("list");
		assertNotNull(l);
		assertTrue(l.size() == 4);
		assertTrue(l.get(0).equals("zero"));
		assertTrue(l.get(3) == null);

		// Check nested map in list
		Map m = (Map) l.get(1);
		assertNotNull(m);
		assertTrue(m.size() == 2);
		assertTrue(m.get("fo").equals("bar"));
		assertTrue("Map element 'jenny' should be equal to jenny bean, not " + m.get("jen"),
				m.get("jen").equals(jenny));

		// Check nested list in list
		l = (List) l.get(2);
		assertNotNull(l);
		assertTrue(l.size() == 2);
		assertTrue(l.get(0).equals(jenny));
		assertTrue(l.get(1).equals("ba"));

		// Check nested map
		m = (Map) hasMap.getMap().get("map");
		assertNotNull(m);
		assertTrue(m.size() == 2);
		assertTrue(m.get("foo").equals("bar"));
		assertTrue("Map element 'jenny' should be equal to jenny bean, not " + m.get("jenny"),
				m.get("jenny").equals(jenny));
	}

	@Test
	public void testEmptySet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptySet");
		assertTrue(hasMap.getSet().size() == 0);
	}

	@Test
	public void testPopulatedSet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("set");
		assertTrue(hasMap.getSet().size() == 3);
		assertTrue(hasMap.getSet().contains("bar"));
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertTrue(hasMap.getSet().contains(jenny));
		assertTrue(hasMap.getSet().contains(null));
		Iterator it = hasMap.getSet().iterator();
		assertEquals("bar", it.next());
		assertEquals(jenny, it.next());
		assertEquals(null, it.next());
	}

	@Test
	public void testPopulatedConcurrentSet() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("concurrentSet");
		assertTrue(hasMap.getConcurrentSet().size() == 3);
		assertTrue(hasMap.getConcurrentSet().contains("bar"));
		TestBean jenny = (TestBean) this.beanFactory.getBean("jenny");
		assertTrue(hasMap.getConcurrentSet().contains(jenny));
		assertTrue(hasMap.getConcurrentSet().contains(null));
	}

	@Test
	public void testPopulatedIdentityMap() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("identityMap");
		assertTrue(hasMap.getIdentityMap().size() == 2);
		HashSet set = new HashSet(hasMap.getIdentityMap().keySet());
		assertTrue(set.contains("foo"));
		assertTrue(set.contains("jenny"));
	}

	@Test
	public void testEmptyProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("emptyProps");
		assertTrue(hasMap.getProps().size() == 0);
		assertEquals(hasMap.getProps().getClass(), Properties.class);
	}

	@Test
	public void testPopulatedProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertTrue(hasMap.getProps().size() == 2);
		assertTrue(hasMap.getProps().get("foo").equals("bar"));
		assertTrue(hasMap.getProps().get("2").equals("TWO"));
	}

	@Test
	public void testObjectArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("objectArray");
		assertTrue(hasMap.getObjectArray().length == 2);
		assertTrue(hasMap.getObjectArray()[0].equals("one"));
		assertTrue(hasMap.getObjectArray()[1].equals(this.beanFactory.getBean("jenny")));
	}

	@Test
	public void testClassArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("classArray");
		assertTrue(hasMap.getClassArray().length == 2);
		assertTrue(hasMap.getClassArray()[0].equals(String.class));
		assertTrue(hasMap.getClassArray()[1].equals(Exception.class));
	}

	@Test
	public void testIntegerArray() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("integerArray");
		assertTrue(hasMap.getIntegerArray().length == 3);
		assertTrue(hasMap.getIntegerArray()[0].intValue() == 0);
		assertTrue(hasMap.getIntegerArray()[1].intValue() == 1);
		assertTrue(hasMap.getIntegerArray()[2].intValue() == 2);
	}

	@Test
	public void testProps() throws Exception {
		HasMap hasMap = (HasMap) this.beanFactory.getBean("props");
		assertEquals(2, hasMap.getProps().size());
		assertEquals("bar", hasMap.getProps().getProperty("foo"));
		assertEquals("TWO", hasMap.getProps().getProperty("2"));

		HasMap hasMap2 = (HasMap) this.beanFactory.getBean("propsViaMap");
		assertEquals(2, hasMap2.getProps().size());
		assertEquals("bar", hasMap2.getProps().getProperty("foo"));
		assertEquals("TWO", hasMap2.getProps().getProperty("2"));
	}

	@Test
	public void testListFactory() throws Exception {
		List list = (List) this.beanFactory.getBean("listFactory");
		assertTrue(list instanceof LinkedList);
		assertTrue(list.size() == 2);
		assertEquals("bar", list.get(0));
		assertEquals("jenny", list.get(1));
	}

	@Test
	public void testPrototypeListFactory() throws Exception {
		List list = (List) this.beanFactory.getBean("pListFactory");
		assertTrue(list instanceof LinkedList);
		assertTrue(list.size() == 2);
		assertEquals("bar", list.get(0));
		assertEquals("jenny", list.get(1));
	}

	@Test
	public void testSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("setFactory");
		assertTrue(set instanceof TreeSet);
		assertTrue(set.size() == 2);
		assertTrue(set.contains("bar"));
		assertTrue(set.contains("jenny"));
	}

	@Test
	public void testPrototypeSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("pSetFactory");
		assertTrue(set instanceof TreeSet);
		assertTrue(set.size() == 2);
		assertTrue(set.contains("bar"));
		assertTrue(set.contains("jenny"));
	}

	@Test
	public void testMapFactory() throws Exception {
		Map map = (Map) this.beanFactory.getBean("mapFactory");
		assertTrue(map instanceof TreeMap);
		assertTrue(map.size() == 2);
		assertEquals("bar", map.get("foo"));
		assertEquals("jenny", map.get("jen"));
	}

	@Test
	public void testPrototypeMapFactory() throws Exception {
		Map map = (Map) this.beanFactory.getBean("pMapFactory");
		assertTrue(map instanceof TreeMap);
		assertTrue(map.size() == 2);
		assertEquals("bar", map.get("foo"));
		assertEquals("jenny", map.get("jen"));
	}

	@Test
	public void testChoiceBetweenSetAndMap() {
		MapAndSet sam = (MapAndSet) this.beanFactory.getBean("setAndMap");
		assertTrue("Didn't choose constructor with Map argument", sam.getObject() instanceof Map);
		Map map = (Map) sam.getObject();
		assertEquals(3, map.size());
		assertEquals("val1", map.get("key1"));
		assertEquals("val2", map.get("key2"));
		assertEquals("val3", map.get("key3"));
	}

	@Test
	public void testEnumSetFactory() throws Exception {
		Set set = (Set) this.beanFactory.getBean("enumSetFactory");
		assertTrue(set.size() == 2);
		assertTrue(set.contains("ONE"));
		assertTrue(set.contains("TWO"));
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


/**
 * Bean exposing a map. Used for bean factory tests.
 *
 * @author Rod Johnson
 * @since 05.06.2003
 */
class HasMap {
	
	private Map map;

	private IdentityHashMap identityMap;

	private Set set;

	private CopyOnWriteArraySet concurrentSet;

	private Properties props;
	
	private Object[] objectArray;
	
	private Class[] classArray;
	
	private Integer[] intArray;

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

	public IdentityHashMap getIdentityMap() {
		return identityMap;
	}

	public void setIdentityMap(IdentityHashMap identityMap) {
		this.identityMap = identityMap;
	}

	public Set getSet() {
		return set;
	}

	public void setSet(Set set) {
		this.set = set;
	}

	public CopyOnWriteArraySet getConcurrentSet() {
		return concurrentSet;
	}

	public void setConcurrentSet(CopyOnWriteArraySet concurrentSet) {
		this.concurrentSet = concurrentSet;
	}

	public Properties getProps() {
		return props;
	}

	public void setProps(Properties props) {
		this.props = props;
	}

	public Object[] getObjectArray() {
		return objectArray;
	}

	public void setObjectArray(Object[] objectArray) {
		this.objectArray = objectArray;
	}

	public Class[] getClassArray() {
		return classArray;
	}

	public void setClassArray(Class[] classArray) {
		this.classArray = classArray;
	}

	public Integer[] getIntegerArray() {
		return intArray;
	}

	public void setIntegerArray(Integer[] is) {
		intArray = is;
	}

}
