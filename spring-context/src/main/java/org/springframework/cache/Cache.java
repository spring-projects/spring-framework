/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cache;

/**
 * Interface that defines the common cache operations.
 *
 * <b>Note:</b> Due to the generic use of caching, it is recommended that
 * implementations allow storage of <tt>null</tt> values (for example to
 * cache methods that return {@code null}).
 *
 * @author Costin Leau
 * @since 3.1
 */
public interface Cache {

	/**
	 * Return the cache name.
	 */
	String getName();

	/**
	 * Return the the underlying native cache provider.
	 */
	Object getNativeCache();

	/**
	 * Return the value to which this cache maps the specified key. Returns
	 * <code>null</code> if the cache contains no mapping for this key.
	 * @param key key whose associated value is to be returned.
	 * @return the value to which this cache maps the specified key,
	 * or <code>null</code> if the cache contains no mapping for this key
	 */
	ValueWrapper get(Object key);

	/**
	 * Associate the specified value with the specified key in this cache.
	 * <p>If the cache previously contained a mapping for this key, the old
	 * value is replaced by the specified value.
	 * @param key the key with which the specified value is to be associated
	 * @param value the value to be associated with the specified key
	 */
	void put(Object key, Object value);

	/**
	 * Evict the mapping for this key from this cache if it is present.
	 * @param key the key whose mapping is to be removed from the cache
	 */
	void evict(Object key);

	/**
	 * Remove all mappings from the cache.
	 */
	void clear();


	/**
	 * A (wrapper) object representing a cache value.
	 */
	interface ValueWrapper {

		/**
		 * Return the actual value in the cache.
		 */
		Object get();
	}

}
