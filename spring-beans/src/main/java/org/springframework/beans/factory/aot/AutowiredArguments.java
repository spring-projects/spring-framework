/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.beans.factory.aot;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Resolved arguments to be autowired.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 * @since 6.0
 * @see BeanInstanceSupplier
 * @see AutowiredMethodArgumentsResolver
 */
@FunctionalInterface
public interface AutowiredArguments {

	/**
	 * Return the resolved argument at the specified index.
	 * @param <T> the type of the argument
	 * @param index the argument index
	 * @param requiredType the required argument type
	 * @return the argument
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T get(int index, Class<T> requiredType) {
		Object value = getObject(index);
		if (!ClassUtils.isAssignableValue(requiredType, value)) {
			throw new IllegalArgumentException("Argument type mismatch: expected '" +
					ClassUtils.getQualifiedName(requiredType) + "' for value [" + value + "]");
		}
		return (T) value;
	}

	/**
	 * Return the resolved argument at the specified index.
	 * @param <T> the type of the argument
	 * @param index the argument index
	 * @return the argument
	 */
	@SuppressWarnings("unchecked")
	@Nullable
	default <T> T get(int index) {
		return (T) getObject(index);
	}

	/**
	 * Return the resolved argument at the specified index.
	 * @param index the argument index
	 * @return the argument
	 */
	@Nullable
	default Object getObject(int index) {
		return toArray()[index];
	}

	/**
	 * Return the arguments as an object array.
	 * @return the arguments as an object array
	 */
	Object[] toArray();

	/**
	 * Factory method to create a new {@link AutowiredArguments} instance from
	 * the given object array.
	 * @param arguments the arguments
	 * @return a new {@link AutowiredArguments} instance
	 */
	static AutowiredArguments of(Object[] arguments) {
		Assert.notNull(arguments, "'arguments' must not be null");
		return () -> arguments;
	}

}
