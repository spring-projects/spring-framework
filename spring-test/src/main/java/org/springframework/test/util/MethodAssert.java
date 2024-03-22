/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.util;

import java.lang.reflect.Method;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;

import org.springframework.lang.Nullable;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to a {@link Method}.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class MethodAssert extends AbstractObjectAssert<MethodAssert, Method> {

	public MethodAssert(@Nullable Method actual) {
		super(actual, MethodAssert.class);
		as("Method %s", actual);
	}

	/**
	 * Verify that the actual method has the given {@linkplain Method#getName()
	 * name}.
	 * @param name the expected method name
	 */
	public MethodAssert hasName(String name) {
		isNotNull();
		Assertions.assertThat(this.actual.getName()).as("Method name").isEqualTo(name);
		return this.myself;
	}

	/**
	 * Verify that the actual method is declared in the given {@code type}.
	 * @param type the expected declaring class
	 */
	public MethodAssert hasDeclaringClass(Class<?> type) {
		isNotNull();
		Assertions.assertThat(this.actual.getDeclaringClass())
				.as("Method declaring class").isEqualTo(type);
		return this.myself;
	}

}
