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

package org.springframework.beans.testfixture.beans;

import java.io.Serializable;

import org.springframework.lang.Nullable;
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
