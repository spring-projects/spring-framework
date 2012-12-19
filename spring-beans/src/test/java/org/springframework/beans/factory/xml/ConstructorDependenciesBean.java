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

import java.io.Serializable;

import test.beans.IndexedTestBean;
import test.beans.TestBean;

/**
 * Simple bean used to check constructor dependency checking.
 *
 * @author Juergen Hoeller
 * @since 09.11.2003
 */
@SuppressWarnings("serial")
public class ConstructorDependenciesBean implements Serializable {

	private int age;

	private String name;

	private TestBean spouse1;

	private TestBean spouse2;

	private IndexedTestBean other;

	public ConstructorDependenciesBean(int age) {
		this.age = age;
	}

	public ConstructorDependenciesBean(String name) {
		this.name = name;
	}

	public ConstructorDependenciesBean(TestBean spouse1) {
		this.spouse1 = spouse1;
	}

	public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2) {
		this.spouse1 = spouse1;
		this.spouse2 = spouse2;
	}

	public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, int age) {
		this.spouse1 = spouse1;
		this.spouse2 = spouse2;
		this.age = age;
	}

	public ConstructorDependenciesBean(TestBean spouse1, TestBean spouse2, IndexedTestBean other) {
		this.spouse1 = spouse1;
		this.spouse2 = spouse2;
		this.other = other;
	}

	public int getAge() {
		return age;
	}

	public String getName() {
		return name;
	}

	public TestBean getSpouse1() {
		return spouse1;
	}

	public TestBean getSpouse2() {
		return spouse2;
	}

	public IndexedTestBean getOther() {
		return other;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public void setName(String name) {
		this.name = name;
	}

}
