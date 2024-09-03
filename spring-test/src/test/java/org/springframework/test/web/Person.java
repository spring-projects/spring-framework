/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.test.web;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

@XmlRootElement
public class Person {

	@NotNull
	private String name;

	private double someDouble;

	private boolean someBoolean;

	public Person() {
	}

	public Person(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public Person setName(String name) {
		this.name = name;
		return this;
	}

	public double getSomeDouble() {
		return someDouble;
	}

	public Person setSomeDouble(double someDouble) {
		this.someDouble = someDouble;
		return this;
	}

	public boolean isSomeBoolean() {
		return someBoolean;
	}

	public Person setSomeBoolean(boolean someBoolean) {
		this.someBoolean = someBoolean;
		return this;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Person that &&
				ObjectUtils.nullSafeEquals(this.name, that.name) &&
				ObjectUtils.nullSafeEquals(this.someDouble, that.someDouble) &&
				ObjectUtils.nullSafeEquals(this.someBoolean, that.someBoolean)));
	}

	@Override
	public int hashCode() {
		return Person.class.hashCode();
	}

	@Override
	public String toString() {
		return "Person [name=" + this.name + ", someDouble=" + this.someDouble
				+ ", someBoolean=" + this.someBoolean + "]";
	}

}
