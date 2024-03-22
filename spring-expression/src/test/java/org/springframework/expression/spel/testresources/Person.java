/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.expression.spel.testresources;

public class Person {

	private String privateName;

	private int age;

	Company company;


	public Person(int age) {
		this.age = age;
	}

	public Person(String name) {
		this.privateName = name;
	}

	public Person(String name, Company company) {
		this.privateName = name;
		this.company = company;
	}


	public String getName() {
		return privateName;
	}

	public void setName(String n) {
		this.privateName = n;
	}

	public int getAge() {
		return age;
	}

	public void setAge(int age) {
		this.age = age;
	}

	public Company getCompany() {
		return company;
	}

}
