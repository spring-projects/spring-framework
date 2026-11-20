/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web;

import jakarta.validation.constraints.NotNull;
import jakarta.xml.bind.annotation.XmlRootElement;
import org.jspecify.annotations.Nullable;

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
		return "Person [name=" + this.name + ", someDouble=" + this.someDouble +
				", someBoolean=" + this.someBoolean + "]";
	}

}
