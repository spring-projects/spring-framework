/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.cache.support;

import java.io.Serializable;

import org.jspecify.annotations.Nullable;

/**
 * Simple serializable class that serves as a {@code null} replacement
 * for cache stores which otherwise do not support {@code null} values.
 *
 * @author Juergen Hoeller
 * @since 4.2.2
 * @see AbstractValueAdaptingCache
 */
public final class NullValue implements Serializable {

	/**
	 * The canonical representation of a {@code null} replacement, as used by the
	 * default implementation of {@link AbstractValueAdaptingCache#toStoreValue}/
	 * {@link AbstractValueAdaptingCache#fromStoreValue}.
	 * @since 4.3.10
	 */
	public static final Object INSTANCE = new NullValue();

	private static final long serialVersionUID = 1L;


	private NullValue() {
	}

	private Object readResolve() {
		return INSTANCE;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || other == null);
	}

	@Override
	public int hashCode() {
		return NullValue.class.hashCode();
	}

	@Override
	public String toString() {
		return "null";
	}

}
