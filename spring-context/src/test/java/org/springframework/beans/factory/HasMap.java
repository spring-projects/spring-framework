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

package org.springframework.beans.factory;

import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * Bean exposing a map. Used for bean factory tests.
 *
 * @author Rod Johnson
 * @since 05.06.2003
 */
public class HasMap {

	private Map<Object, Object> map;

	private Set<Object> set;

	private Properties props;

	private Object[] objectArray;

	private Class<?>[] classArray;

	private Integer[] intArray;

	private HasMap() {
	}

	public Map<Object, Object> getMap() {
		return map;
	}

	public void setMap(Map<Object, Object> map) {
		this.map = map;
	}

	public Set<Object> getSet() {
		return set;
	}

	public void setSet(Set<Object> set) {
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

	public Class<?>[] getClassArray() {
		return classArray;
	}

	public void setClassArray(Class<?>[] classArray) {
		this.classArray = classArray;
	}

	public Integer[] getIntegerArray() {
		return intArray;
	}

	public void setIntegerArray(Integer[] is) {
		intArray = is;
	}

}
