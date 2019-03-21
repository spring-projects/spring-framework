/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.tests.sample.objects;

public class TestObject implements ITestObject, ITestInterface, Comparable<Object> {

	private String name;

	private int age;

	private TestObject spouse;

	public TestObject() {
	}

	public TestObject(String name, int age) {
		this.name = name;
		this.age = age;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public int getAge() {
		return this.age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public TestObject getSpouse() {
		return this.spouse;
	}

	@Override
	public void setSpouse(TestObject spouse) {
		this.spouse = spouse;
	}

	@Override
	public void absquatulate() {
	}

	@Override
	public int compareTo(Object o) {
		if (this.name != null && o instanceof TestObject) {
			return this.name.compareTo(((TestObject) o).getName());
		}
		else {
			return 1;
		}
	}
}
