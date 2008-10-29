/*
 * Copyright 2002-2005 the original author or authors.
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

import org.springframework.beans.IndexedTestBean;
import org.springframework.beans.TestBean;

/**
 * Simple bean used to check constructor dependency checking.
 *
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
class DerivedConstructorDependenciesBean extends ConstructorDependenciesBean {

	boolean initialized;
	boolean destroyed;

	DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other) {
		super(spouse1, spouse2, other);
	}

	private DerivedConstructorDependenciesBean(TestBean spouse1, Object spouse2, IndexedTestBean other) {
		super(spouse1, null, other);
	}

	protected DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other, int age, int otherAge) {
		super(spouse1, spouse2, other);
	}

	public DerivedConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other, int age, String name) {
		super(spouse1, spouse2, other);
		setAge(age);
		setName(name);
	}

	private void init() {
		this.initialized = true;
	}

	private void destroy() {
		this.destroyed = true;
	}

}
