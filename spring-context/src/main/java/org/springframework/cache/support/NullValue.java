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

package org.springframework.cache.support;

import java.io.Serializable;

import org.springframework.lang.Nullable;

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
