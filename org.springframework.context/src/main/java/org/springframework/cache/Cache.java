/*
 * Copyright 2010-2011 the original author or authors.
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
 * cache methods that return null).
 * 
 * @author Costin Leau
 */
public interface Cache<K, V> {

	/**
	 * A (wrapper) object representing a cache value.
	 */
	interface ValueWrapper<V> {
		/**
		 * Returns the actual value in the cache.
		 *  
		 * @return cache value
		 */
		V get();
	}

	/**
	 * Returns the cache name.
	 * 
	 * @return the cache name.
	 */
	String getName();

	/**
	 * Returns the the native, underlying cache provider.
	 * 
	 * @return the underlying native cache provider.
	 */
	Object getNativeCache();

	/**
	 * Returns the value to which this cache maps the specified key. Returns
	 * <tt>null</tt> if the cache contains no mapping for this key.
	 * 
	 * @param key key whose associated value is to be returned.
	 * @return the value to which this cache maps the specified key, or
	 *	       <tt>null</tt> if the cache contains no mapping for this key.
	 */
	ValueWrapper<V> get(Object key);

	/**
	 * Associates the specified value with the specified key in this cache.
	 * If the cache previously contained a mapping for this key, the old 
	 * value is replaced by the specified value.
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 */
	void put(K key, V value);

	/**
	 * Evicts the mapping for this key from this cache if it is present.
	 *
	 * @param key key whose mapping is to be removed from the cache.
	 */
	void evict(Object key);

	/**
	 * Removes all mappings from the cache.
	 */
	void clear();
}