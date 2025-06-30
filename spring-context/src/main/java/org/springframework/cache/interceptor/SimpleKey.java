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

package org.springframework.cache.interceptor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.Arrays;

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;

/**
 * A simple key as returned from the {@link SimpleKeyGenerator}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @author Brian Clozel
 * @since 4.0
 * @see SimpleKeyGenerator
 */
@SuppressWarnings("serial")
public class SimpleKey implements Serializable {

	/**
	 * An empty key.
	 */
	public static final SimpleKey EMPTY = new SimpleKey();


	private final @Nullable Object[] params;

	// Effectively final, just re-calculated on deserialization
	private transient int hashCode;


	/**
	 * Create a new {@link SimpleKey} instance.
	 * @param elements the elements of the key
	 */
	public SimpleKey(@Nullable Object... elements) {
		Assert.notNull(elements, "Elements must not be null");
		this.params = elements.clone();
		// Pre-calculate hashCode field
		this.hashCode = calculateHash(this.params);
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpleKey that && Arrays.deepEquals(this.params, that.params)));
	}

	@Override
	public final int hashCode() {
		// Expose pre-calculated hashCode field
		return this.hashCode;
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + " " + Arrays.deepToString(this.params);
	}

	private void readObject(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		ois.defaultReadObject();
		// Re-calculate hashCode field on deserialization
		this.hashCode = calculateHash(this.params);
	}

	/**
	 * Calculate the hash of the key using its elements and
	 * mix the result with the finalising function of MurmurHash3.
	 */
	private static int calculateHash(@Nullable Object[] params) {
		int hash = Arrays.deepHashCode(params);
		hash = (hash ^ (hash >>> 16)) * 0x85ebca6b;
		hash = (hash ^ (hash >>> 13)) * 0xc2b2ae35;
		return hash ^ (hash >>> 16);
	}

}
