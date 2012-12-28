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

package org.springframework.beans.factory.access;

import java.util.List;

/**
 * Scrap bean for use in tests.
 *
 * @author Colin Sampaleanu
 */
public class TestBean {

	private String name;

	private List<?> list;

	private Object objRef;

	/**
	 * @return Returns the name.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name The name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return Returns the list.
	 */
	public List<?> getList() {
		return list;
	}

	/**
	 * @param list The list to set.
	 */
	public void setList(List<?> list) {
		this.list = list;
	}

	/**
	 * @return Returns the object.
	 */
	public Object getObjRef() {
		return objRef;
	}

	/**
	 * @param object The object to set.
	 */
	public void setObjRef(Object object) {
		this.objRef = object;
	}
}
