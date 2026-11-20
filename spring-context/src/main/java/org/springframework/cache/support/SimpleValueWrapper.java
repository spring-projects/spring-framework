/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
