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

package org.springframework.beans.testfixture.beans;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * @author Juergen Hoeller
 * @since 11.11.2003
 */
@SuppressWarnings("rawtypes")
public class IndexedTestBean {

	private TestBean[] array;

	private Collection<?> collection;

	private List list;

	private Set<? super Object> set;

	private SortedSet<? super Object> sortedSet;

	private Map map;

	private SortedMap sortedMap;

	private IterableMap iterableMap;

	private MyTestBeans myTestBeans;


	public IndexedTestBean() {
		this(true);
	}

	public IndexedTestBean(boolean populate) {
		if (populate) {
			populate();
		}
	}

	@SuppressWarnings("unchecked")
	public void populate() {
		TestBean tb0 = new TestBean("name0", 0);
		TestBean tb1 = new TestBean("name1", 0);
		TestBean tb2 = new TestBean("name2", 0);
		TestBean tb3 = new TestBean("name3", 0);
		TestBean tb4 = new TestBean("name4", 0);
		TestBean tb5 = new TestBean("name5", 0);
		TestBean tb6 = new TestBean("name6", 0);
		TestBean tb7 = new TestBean("name7", 0);
		TestBean tb8 = new TestBean("name8", 0);
		TestBean tbA = new TestBean("nameA", 0);
		TestBean tbB = new TestBean("nameB", 0);
		TestBean tbC = new TestBean("nameC", 0);
		TestBean tbX = new TestBean("nameX", 0);
		TestBean tbY = new TestBean("nameY", 0);
		TestBean tbZ = new TestBean("nameZ", 0);
		this.array = new TestBean[] {tb0, tb1};
		this.list = new ArrayList<>();
		this.list.add(tb2);
		this.list.add(tb3);
		this.set = new TreeSet<>();
		this.set.add(tb6);
		this.set.add(tb7);
		this.map = new HashMap<>();
		this.map.put("key1", tb4);
		this.map.put("key2", tb5);
		this.map.put("key.3", tb5);
		List list = new ArrayList();
		list.add(tbA);
		list.add(tbB);
		this.iterableMap = new IterableMap<>();
		this.iterableMap.put("key1", tbC);
		this.iterableMap.put("key2", list);
		list = new ArrayList();
		list.add(tbX);
		list.add(tbY);
		this.map.put("key4", list);
		this.map.put("key5[foo]", tb8);
		this.myTestBeans = new MyTestBeans(tbZ);
	}


	public TestBean[] getArray() {
		return array;
	}

	public void setArray(TestBean[] array) {
		this.array = array;
	}

	public Collection<?> getCollection() {
		return collection;
	}

	public void setCollection(Collection<?> collection) {
		this.collection = collection;
	}

	public List getList() {
		return list;
	}

	public void setList(List list) {
		this.list = list;
	}

	public Set<?> getSet() {
		return set;
	}

	public void setSet(Set<? super Object> set) {
		this.set = set;
	}

	public SortedSet<? super Object> getSortedSet() {
		return sortedSet;
	}

	public void setSortedSet(SortedSet<? super Object> sortedSet) {
		this.sortedSet = sortedSet;
	}

	public Map getMap() {
		return map;
	}

	public void setMap(Map map) {
		this.map = map;
	}

	public SortedMap getSortedMap() {
		return sortedMap;
	}

	public void setSortedMap(SortedMap sortedMap) {
		this.sortedMap = sortedMap;
	}

	public IterableMap getIterableMap() {
		return this.iterableMap;
	}

	public void setIterableMap(IterableMap iterableMap) {
		this.iterableMap = iterableMap;
	}

	public MyTestBeans getMyTestBeans() {
		return myTestBeans;
	}

	public void setMyTestBeans(MyTestBeans myTestBeans) {
		this.myTestBeans = myTestBeans;
	}


	@SuppressWarnings("serial")
	public static class IterableMap<K,V> extends LinkedHashMap<K,V> implements Iterable<V> {

		@Override
		public Iterator<V> iterator() {
			return values().iterator();
		}
	}

	public static class MyTestBeans implements Iterable<TestBean> {

		private final Collection<TestBean> testBeans;

		public MyTestBeans(TestBean... testBeans) {
			this.testBeans = Arrays.asList(testBeans);
		}

		@Override
		public Iterator<TestBean> iterator() {
			return this.testBeans.iterator();
		}
	}

}
