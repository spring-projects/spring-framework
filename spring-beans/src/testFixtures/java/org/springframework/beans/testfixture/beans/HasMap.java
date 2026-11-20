/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Bean exposing a map. Used for bean factory tests.
 *
 * @author Rod Johnson
 * @since 05.06.2003
 */
public class HasMap {

	private Map<?, ?> map;

	private Set<?> set;

	private Properties props;

	private Object[] objectArray;

	private Integer[] intArray;

	private Class<?>[] classArray;

	private List<Class<?>> classList;

	private IdentityHashMap<?, ?> identityMap;

	private CopyOnWriteArraySet<?> concurrentSet;

	private HasMap() {
	}

	public Map<?, ?> getMap() {
		return map;
	}

	public void setMap(Map<?, ?> map) {
		this.map = map;
	}

	public Set<?> getSet() {
		return set;
	}

	public void setSet(Set<?> set) {
		this.set = set;
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

	public Integer[] getIntegerArray() {
		return intArray;
	}

	public void setIntegerArray(Integer[] is) {
		intArray = is;
	}

	public Class<?>[] getClassArray() {
		return classArray;
	}

	public void setClassArray(Class<?>[] classArray) {
		this.classArray = classArray;
	}

	public List<Class<?>> getClassList() {
		return classList;
	}

	public void setClassList(List<Class<?>> classList) {
		this.classList = classList;
	}

	public IdentityHashMap<?, ?> getIdentityMap() {
		return identityMap;
	}

	public void setIdentityMap(IdentityHashMap<?, ?> identityMap) {
		this.identityMap = identityMap;
	}

	public CopyOnWriteArraySet<?> getConcurrentSet() {
		return concurrentSet;
	}

	public void setConcurrentSet(CopyOnWriteArraySet<?> concurrentSet) {
		this.concurrentSet = concurrentSet;
	}

}
