/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.event.test;

import org.jspecify.annotations.Nullable;

import org.springframework.core.ResolvableType;
import org.springframework.core.ResolvableTypeProvider;

/**
 * A simple POJO that implements {@link ResolvableTypeProvider}.
 *
 * @author Stephane Nicoll
 */
public class GenericEventPojo<T> implements ResolvableTypeProvider {
	private final T value;

	public GenericEventPojo(T value) {
		this.value = value;
	}

	@Override
	public ResolvableType getResolvableType() {
		return ResolvableType.forClassWithGenerics(getClass(), ResolvableType.forInstance(this.value));
	}

	@Override
	public boolean equals(@Nullable Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		GenericEventPojo<?> that = (GenericEventPojo<?>) o;

		return this.value.equals(that.value);
	}

	@Override
	public int hashCode() {
		return this.value.hashCode();
	}
}
