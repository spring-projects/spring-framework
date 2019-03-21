/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.test.context.junit4.orm.domain;

/**
 * Person POJO.
 *
 * @author Sam Brannen
 * @since 3.0
 */
public class Person {

	private Long id;
	private String name;
	private DriversLicense driversLicense;


	public Person() {
	}

	public Person(Long id) {
		this(id, null, null);
	}

	public Person(String name) {
		this(name, null);
	}

	public Person(String name, DriversLicense driversLicense) {
		this(null, name, driversLicense);
	}

	public Person(Long id, String name, DriversLicense driversLicense) {
		this.id = id;
		this.name = name;
		this.driversLicense = driversLicense;
	}

	public Long getId() {
		return this.id;
	}

	protected void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public DriversLicense getDriversLicense() {
		return this.driversLicense;
	}

	public void setDriversLicense(DriversLicense driversLicense) {
		this.driversLicense = driversLicense;
	}
}
