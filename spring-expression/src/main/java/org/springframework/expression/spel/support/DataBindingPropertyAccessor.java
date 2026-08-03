/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.expression.spel.support;

import java.lang.reflect.Method;

/**
 * An {@link org.springframework.expression.PropertyAccessor} variant for data binding
 * purposes, using reflection to access properties for reading and possibly writing.
 *
 * <p>A property can be referenced through a public getter method (when being read)
 * or a public setter method (when being written), and also as a public field.
 *
 * <p>This accessor is explicitly designed for user-declared properties and does not
 * resolve technical properties on {@code java.lang.Object} or {@code java.lang.Class}.
 * For unrestricted resolution, choose {@link ReflectivePropertyAccessor} instead.
 *
 * <p><strong>WARNING</strong>: Configuring this accessor for read-only access &mdash;
 * for example, via {@link #forReadOnlyAccess()} or
 * {@link SimpleEvaluationContext#forReadOnlyDataBinding()} &mdash; disallows
 * <em>assignment</em> to a property but does not verify that reading a property is
 * free of side effects. See the
 * <a href="https://docs.spring.io/spring-framework/reference/core/expressions/evaluation.html#expressions-evaluation-context-security"
 * >Security Considerations</a> section of the Spring Framework reference
 * documentation, as well as the class-level documentation for
 * {@link ReflectivePropertyAccessor}, for details.
 *
 * @author Juergen Hoeller
 * @since 4.3.15
 * @see #forReadOnlyAccess()
 * @see #forReadWriteAccess()
 * @see SimpleEvaluationContext
 * @see StandardEvaluationContext
 * @see ReflectivePropertyAccessor
 */
public final class DataBindingPropertyAccessor extends ReflectivePropertyAccessor {

	/**
	 * Create a new property accessor for reading and possibly also writing.
	 * @param allowWrite whether to also allow for write operations
	 * @see #canWrite
	 */
	private DataBindingPropertyAccessor(boolean allowWrite) {
		super(allowWrite);
	}

	@Override
	protected boolean isCandidateForProperty(Method method, Class<?> targetClass) {
		Class<?> clazz = method.getDeclaringClass();
		return (clazz != Object.class && clazz != Class.class && !ClassLoader.class.isAssignableFrom(targetClass));
	}


	/**
	 * Create a new data-binding property accessor for read-only operations.
	 */
	public static DataBindingPropertyAccessor forReadOnlyAccess() {
		return new DataBindingPropertyAccessor(false);
	}

	/**
	 * Create a new data-binding property accessor for read-write operations.
	 */
	public static DataBindingPropertyAccessor forReadWriteAccess() {
		return new DataBindingPropertyAccessor(true);
	}

}
