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

package org.springframework.r2dbc.core;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

/**
 * A database value that can be set in a statement.
 *
 * @author Mark Paluch
 * @author Juergen Hoeller
 * @since 5.3
 * @deprecated since 6.0, use {@code io.r2dbc.spi.Parameter} instead.
 */
@Deprecated(since = "6.0")
public final class Parameter {

	private final @Nullable Object value;

	private final Class<?> type;


	private Parameter(@Nullable Object value, Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		this.value = value;
		this.type = type;
	}


	/**
	 * Create a new {@link Parameter} from {@code value}.
	 * @param value the parameter value
	 * @return the {@link Parameter} value for {@code value}
	 */
	public static Parameter from(Object value) {
		Assert.notNull(value, "Value must not be null");
		return new Parameter(value, ClassUtils.getUserClass(value));
	}

	/**
	 * Create a new {@link Parameter} from {@code value} and {@code type}.
	 * @param value the parameter value (can be {@code null})
	 * @param type the parameter type
	 * @return the {@link Parameter} value for {@code value}
	 */
	public static Parameter fromOrEmpty(@Nullable Object value, Class<?> type) {
		return (value == null ? empty(type) : new Parameter(value, ClassUtils.getUserClass(value)));
	}

	/**
	 * Create a new empty {@link Parameter} for {@code type}.
	 * @return the empty {@link Parameter} value for {@code type}
	 */
	public static Parameter empty(Class<?> type) {
		Assert.notNull(type, "Type must not be null");
		return new Parameter(null, type);
	}


	/**
	 * Return the column value (can be {@code null}).
	 * @see #hasValue()
	 */
	public @Nullable Object getValue() {
		return this.value;
	}

	/**
	 * Return the column value type. Must be also present if the {@code value} is {@code null}.
	 */
	public Class<?> getType() {
		return this.type;
	}

	/**
	 * Return whether this {@link Parameter} has a value.
	 * @return {@code false} if {@link #getValue()} is {@code null}
	 */
	public boolean hasValue() {
		return (this.value != null);
	}

	/**
	 * Return whether this {@link Parameter} has an empty value.
	 * @return {@code true} if {@link #getValue()} is {@code null}
	 */
	public boolean isEmpty() {
		return (this.value == null);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof Parameter that &&
				ObjectUtils.nullSafeEquals(this.value, that.value) &&
				ObjectUtils.nullSafeEquals(this.type, that.type)));
	}

	@Override
	public int hashCode() {
		return ObjectUtils.nullSafeHash(this.value, this.type);
	}

	@Override
	public String toString() {
		return "Parameter[value=" + this.value + ",type=" + this.type.getName() + "]";
	}

}
