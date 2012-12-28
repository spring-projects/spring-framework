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

package org.springframework.util;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A simple decorator for a Map, encapsulating the workflow for caching
 * expensive values in a target Map. Supports caching weak or strong keys.
 *
 * <p>This class is an abstract template. Caching Map implementations
 * should subclass and override the {@code create(key)} method which
 * encapsulates expensive creation of a new object.
 *
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 * @deprecated as of Spring 3.2, to be removed along with LabeledEnum support
 */
@Deprecated
@SuppressWarnings("serial")
public abstract class CachingMapDecorator<K, V> implements Map<K, V>, Serializable {

	private static Object NULL_VALUE = new Object();


	private final Map<K, Object> targetMap;

	private final boolean synchronize;

	private final boolean weak;


	/**
	 * Create a CachingMapDecorator with strong keys,
	 * using an underlying synchronized Map.
	 */
	public CachingMapDecorator() {
		this(false);
	}

	/**
	 * Create a CachingMapDecorator,
	 * using an underlying synchronized Map.
	 * @param weak whether to use weak references for keys and values
	 */
	public CachingMapDecorator(boolean weak) {
		Map<K, Object> internalMap = (weak ? new WeakHashMap<K, Object>() : new HashMap<K, Object>());
		this.targetMap = Collections.synchronizedMap(internalMap);
		this.synchronize = true;
		this.weak = weak;
	}

	/**
	 * Create a CachingMapDecorator with initial size,
	 * using an underlying synchronized Map.
	 * @param weak whether to use weak references for keys and values
	 * @param size the initial cache size
	 */
	public CachingMapDecorator(boolean weak, int size) {
		Map<K, Object> internalMap = weak ? new WeakHashMap<K, Object> (size) : new HashMap<K, Object>(size);
		this.targetMap = Collections.synchronizedMap(internalMap);
		this.synchronize = true;
		this.weak = weak;
	}

	/**
	 * Create a CachingMapDecorator for the given Map.
	 * <p>The passed-in Map won't get synchronized explicitly,
	 * so make sure to pass in a properly synchronized Map, if desired.
	 * @param targetMap the Map to decorate
	 */
	public CachingMapDecorator(Map<K, V> targetMap) {
		this(targetMap, false, false);
	}

	/**
	 * Create a CachingMapDecorator for the given Map.
	 * <p>The passed-in Map won't get synchronized explicitly unless
	 * you specify "synchronize" as "true".
	 * @param targetMap the Map to decorate
	 * @param synchronize whether to synchronize on the given Map
	 * @param weak whether to use weak references for values
	 */
	@SuppressWarnings("unchecked")
	public CachingMapDecorator(Map<K, V> targetMap, boolean synchronize, boolean weak) {
		Assert.notNull(targetMap, "'targetMap' must not be null");
		this.targetMap = (Map<K, Object>) (synchronize ? Collections.synchronizedMap(targetMap) : targetMap);
		this.synchronize = synchronize;
		this.weak = weak;
	}


	public int size() {
		return this.targetMap.size();
	}

	public boolean isEmpty() {
		return this.targetMap.isEmpty();
	}

	public boolean containsKey(Object key) {
		return this.targetMap.containsKey(key);
	}

	public boolean containsValue(Object value) {
		Object valueToCheck = (value != null ? value : NULL_VALUE);
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return containsValueOrReference(valueToCheck);
			}
		}
		else {
			return containsValueOrReference(valueToCheck);
		}
	}

	private boolean containsValueOrReference(Object value) {
		if (this.targetMap.containsValue(value)) {
			return true;
		}
		for (Object mapVal : this.targetMap.values()) {
			if (mapVal instanceof Reference && value.equals(((Reference) mapVal).get())) {
				return true;
			}
		}
		return false;
	}

	public V remove(Object key) {
		return unwrapReturnValue(this.targetMap.remove(key));
	}

	@SuppressWarnings("unchecked")
	private V unwrapReturnValue(Object value) {
		Object returnValue = value;
		if (returnValue instanceof Reference) {
			returnValue = ((Reference) returnValue).get();
		}
		return (returnValue == NULL_VALUE ? null : (V) returnValue);
	}

	public void putAll(Map<? extends K, ? extends V> map) {
		this.targetMap.putAll(map);
	}

	public void clear() {
		this.targetMap.clear();
	}

	public Set<K> keySet() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return new LinkedHashSet<K>(this.targetMap.keySet());
			}
		}
		else {
			return new LinkedHashSet<K>(this.targetMap.keySet());
		}
	}

	public Collection<V> values() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return valuesCopy();
			}
		}
		else {
			return valuesCopy();
		}
	}

	@SuppressWarnings("unchecked")
	private Collection<V> valuesCopy() {
		LinkedList<V> values = new LinkedList<V>();
		for (Iterator<Object> it = this.targetMap.values().iterator(); it.hasNext();) {
			Object value = it.next();
			if (value instanceof Reference) {
				value = ((Reference) value).get();
				if (value == null) {
					it.remove();
					continue;
				}
			}
			values.add(value == NULL_VALUE ? null : (V) value);
		}
		return values;
	}

	public Set<Map.Entry<K, V>> entrySet() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return entryCopy();
			}
		}
		else {
			return entryCopy();
		}
	}

	@SuppressWarnings("unchecked")
	private Set<Map.Entry<K, V>> entryCopy() {
		Map<K,V> entries = new LinkedHashMap<K, V>();
		for (Iterator<Entry<K, Object>> it = this.targetMap.entrySet().iterator(); it.hasNext();) {
			Entry<K, Object> entry = it.next();
			Object value = entry.getValue();
			if (value instanceof Reference) {
				value = ((Reference) value).get();
				if (value == null) {
					it.remove();
					continue;
				}
			}
			entries.put(entry.getKey(), value == NULL_VALUE ? null : (V) value);
		}
		return entries.entrySet();
	}


	/**
	 * Put an object into the cache, possibly wrapping it with a weak
	 * reference.
	 * @see #useWeakValue(Object, Object)
	 */
	public V put(K key, V value) {
		Object newValue = value;
		if (value == null) {
			newValue = NULL_VALUE;
		}
		else if (useWeakValue(key, value)) {
			newValue = new WeakReference<Object>(newValue);
		}
		return unwrapReturnValue(this.targetMap.put(key, newValue));
	}

	/**
	 * Decide whether to use a weak reference for the value of
	 * the given key-value pair.
	 * @param key the candidate key
	 * @param value the candidate value
	 * @return {@code true} in order to use a weak reference;
	 * {@code false} otherwise.
	 */
	protected boolean useWeakValue(K key, V value) {
		return this.weak;
	}

	/**
	 * Get value for key.
	 * Creates and caches value if it doesn't already exist in the cache.
	 * <p>This implementation is <i>not</i> synchronized: This is highly
	 * concurrent but does not guarantee unique instances in the cache,
	 * as multiple values for the same key could get created in parallel.
	 * Consider overriding this method to synchronize it, if desired.
	 * @see #create(Object)
	 */
	@SuppressWarnings("unchecked")
	public V get(Object key) {
		Object value = this.targetMap.get(key);
		if (value instanceof Reference) {
			value = ((Reference) value).get();
		}
		if (value == null) {
			V newValue = create((K) key);
			put((K) key, newValue);
			return newValue;
		}
		return (value == NULL_VALUE ? null : (V) value);
	}

	/**
	 * Create a value to cache for the given key.
	 * Called by {@code get} if there is no value cached already.
	 * @param key the cache key
	 * @see #get(Object)
	 */
	protected abstract V create(K key);


	@Override
	public String toString() {
		return "CachingMapDecorator [" + getClass().getName() + "]:" + this.targetMap;
	}

}
