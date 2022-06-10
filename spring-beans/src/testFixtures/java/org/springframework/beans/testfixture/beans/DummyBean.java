/*
 * Copyright 2002-2021 the original author or authors.
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

/**
 * @author Costin Leau
 */
public class DummyBean {

	private Object value;
	private String name;
	private int age;
	private TestBean spouse;

	public DummyBean(Object value) {
		this.value = value;
	}

	public DummyBean(String name, int age) {
		this.name = name;
		this.age = age;
	}

	public DummyBean(int ageRef, String nameRef) {
		this.name = nameRef;
		this.age = ageRef;
	}

	public DummyBean(String name, TestBean spouse) {
		this.name = name;
		this.spouse = spouse;
	}

	public DummyBean(String name, Object value, int age) {
		this.name = name;
		this.value = value;
		this.age = age;
	}

	public Object getValue() {
		return value;
	}

	public String getName() {
		return name;
	}

	public int getAge() {
		return age;
	}

	public TestBean getSpouse() {
		return spouse;
	}

}
