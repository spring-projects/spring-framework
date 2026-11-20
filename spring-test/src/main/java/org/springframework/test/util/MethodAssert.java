/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.util;

import java.lang.reflect.Method;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;

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
