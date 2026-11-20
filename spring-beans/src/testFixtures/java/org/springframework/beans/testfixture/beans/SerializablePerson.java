/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.beans.testfixture.beans;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.util.ObjectUtils;

/**
 * Serializable implementation of the Person interface.
 *
 * @author Rod Johnson
 */
@SuppressWarnings("serial")
public class SerializablePerson implements Person, Serializable {

	private String name;

	private int age;


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
		return age;
	}

	@Override
	public void setAge(int age) {
		this.age = age;
	}

	@Override
	public Object echo(Object o) throws Throwable {
		if (o instanceof Throwable) {
			throw (Throwable) o;
		}
		return o;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SerializablePerson that &&
				ObjectUtils.nullSafeEquals(this.name, that.name) && this.age == that.age));
	}

	@Override
	public int hashCode() {
		return SerializablePerson.class.hashCode();
	}

}
