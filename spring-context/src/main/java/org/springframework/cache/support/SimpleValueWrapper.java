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

package org.springframework.cache.support;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.cache.Cache.ValueWrapper;

/**
 * Straightforward implementation of {@link org.springframework.cache.Cache.ValueWrapper},
 * simply holding the value as given at construction and returning it from {@link #get()}.
 *
 * @author Costin Leau
 * @since 3.1
 */
public class SimpleValueWrapper implements ValueWrapper {

	private final @Nullable Object value;


	/**
	 * Create a new SimpleValueWrapper instance for exposing the given value.
	 * @param value the value to expose (may be {@code null})
	 */
	public SimpleValueWrapper(@Nullable Object value) {
		this.value = value;
	}


	/**
	 * Simply returns the value as given at construction time.
	 */
	@Override
	public @Nullable Object get() {
		return this.value;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ValueWrapper wrapper && Objects.equals(get(), wrapper.get())));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.value);
	}

	@Override
	public String toString() {
		return "ValueWrapper for [" + this.value + "]";
	}

}
