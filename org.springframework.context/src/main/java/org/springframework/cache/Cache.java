/*
 * Copyright 2002-2009 the original author or authors.
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
	 * Returns the cache name.
	 * 
	 * @return the cache name.
	 */
	String getName();

	/**
	 * Returns the the native, underlying cache provider.
	 * 
	 * @return
	 */
	Object getNativeCache();

	/**
	 * Returns <tt>true</tt> if this cache contains a mapping for the specified
	 * key.  More formally, returns <tt>true</tt> if and only if
	 * this cache contains a mapping for a key <tt>k</tt> such that
	 * <tt>(key==null ? k==null : key.equals(k))</tt>.  (There can be
	 * at most one such mapping.)
	 *
	 * @param key key whose presence in this cache is to be tested.
	 * @return <tt>true</tt> if this cache contains a mapping for the specified
	 *         key.
	 */
	boolean containsKey(Object key);

	/**
	 * Returns the value to which this cache maps the specified key.  Returns
	 * <tt>null</tt> if the cache contains no mapping for this key.  A return
	 * value of <tt>null</tt> does not <i>necessarily</i> indicate that the
	 * cache contains no mapping for the key; it's also possible that the cache
	 * explicitly maps the key to <tt>null</tt>.  The <tt>containsKey</tt>
	 * operation may be used to distinguish these two cases.
	 *
	 * <p>More formally, if this cache contains a mapping from a key
	 * <tt>k</tt> to a value <tt>v</tt> such that <tt>(key==null ? k==null :
	 * key.equals(k))</tt>, then this method returns <tt>v</tt>; otherwise
	 * it returns <tt>null</tt>.  (There can be at most one such mapping.)
	 *
	 * @param key key whose associated value is to be returned.
	 * @return the value to which this cache maps the specified key, or
	 *	       <tt>null</tt> if the cache contains no mapping for this key.
	 * 
	 * @see #containsKey(Object)
	 */
	V get(Object key);


	/**
	 * Associates the specified value with the specified key in this cache
	 * (optional operation).  If the cache previously contained a mapping for
	 * this key, the old value is replaced by the specified value.  (A cache
	 * <tt>m</tt> is said to contain a mapping for a key <tt>k</tt> if and only
	 * if {@link #containsKey(Object) m.containsKey(k)} would return
	 * <tt>true</tt>.)) 
	 *
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.  A <tt>null</tt> return can
	 *	       also indicate that the cache previously associated <tt>null</tt>
	 *	       with the specified key, if the implementation supports
	 *	       <tt>null</tt> values.
	 */
	V put(K key, V value);


	/**
	 * If the specified key is not already associated with a value, associate it with the given value.
	 * 
	 * This is equivalent to:
	 * <pre>
	 *  if (!cache.containsKey(key)) 
	 *     return cache.put(key, value);
	 *  else
	 *     return cache.get(key);
	 *	</pre>
	 * 
	 * @param key key with which the specified value is to be associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *         if there was no mapping for key.  A <tt>null</tt> return can
	 *         also indicate that the cache previously associated <tt>null</tt>
	 *         with the specified key, if the implementation supports
	 *         <tt>null</tt> values.
	 */
	V putIfAbsent(K key, V value);


	/**
	 * Removes the mapping for this key from this cache if it is present
	 * (optional operation).   More formally, if this cache contains a mapping
	 * from key <tt>k</tt> to value <tt>v</tt> such that
	 * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
	 * is removed.  (The cache can contain at most one such mapping.)
	 *
	 * <p>Returns the value to which the cache previously associated the key, or
	 * <tt>null</tt> if the cache contained no mapping for this key.  (A
	 * <tt>null</tt> return can also indicate that the cache previously
	 * associated <tt>null</tt> with the specified key if the implementation
	 * supports <tt>null</tt> values.)  The cache will not contain a mapping for
	 * the specified  key once the call returns.
	 *
	 * @param key key whose mapping is to be removed from the cache.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *	       if there was no mapping for key.
	 */
	V remove(Object key);


	/**
	 * Remove entry for key only if currently mapped to given value.
	 * 
	 * Similar to:
	 * <pre>
	 *   if ((cache.containsKey(key) && cache.get(key).equals(value)) {
	 *      cache.remove(key);
	 *      return true;
	 *   } 
	 *   else 
	 *      return false;
	 * </pre> 
	 * 
	 * @param key key with which the specified value is associated.
	 * @param value value associated with the specified key.
	 * @return true if the value was removed, false otherwise
	 */
	boolean remove(Object key, Object value);


	/**
	 * Replace entry for key only if currently mapped to given value.
	 * 
	 * Similar to:
	 * <pre> 
	 *  if ((cache.containsKey(key) && cache.get(key).equals(oldValue)) {
	 *     cache.put(key, newValue);
	 *     return true;
	 * } else return false;
	 * </pre>

	 * @param key key with which the specified value is associated.
	 * @param oldValue value expected to be associated with the specified key.
	 * @param newValue value to be associated with the specified key.
	 * @return true if the value was replaced
	 */
	boolean replace(K key, V oldValue, V newValue);

	/**
	 * Replace entry for key only if currently mapped to some value.
	 * Acts as
	 * <pre> 
	 *  if ((cache.containsKey(key)) {
	 *     return cache.put(key, value);
	 * } else return null;
	 * </pre>
	 * except that the action is performed atomically.
	 * @param key key with which the specified value is associated.
	 * @param value value to be associated with the specified key.
	 * @return previous value associated with specified key, or <tt>null</tt>
	 *         if there was no mapping for key.  A <tt>null</tt> return can
	 *         also indicate that the cache previously associated <tt>null</tt>
	 *         with the specified key, if the implementation supports
	 *         <tt>null</tt> values.
	 */
	V replace(K key, V value);


	/**
	 * Removes all mappings from the cache.
	 */
	void clear();
}