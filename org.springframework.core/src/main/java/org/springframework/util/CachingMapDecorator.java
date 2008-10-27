/*
 * Copyright 2002-2008 the original author or authors.
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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * A simple decorator for a Map, encapsulating the workflow for caching
 * expensive values in a target Map. Supports caching weak or strong keys.
 *
 * <p>This class is also an abstract template. Caching Map implementations
 * should subclass and override the <code>create(key)</code> method which
 * encapsulates expensive creation of a new object.
 * 
 * @author Keith Donald
 * @author Juergen Hoeller
 * @since 1.2.2
 */
public class CachingMapDecorator implements Map, Serializable {

	protected static Object NULL_VALUE = new Object();


	private final Map targetMap;

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
		Map internalMap = weak ? (Map) new WeakHashMap() : new HashMap();
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
		Map internalMap = weak ? (Map) new WeakHashMap(size) : new HashMap(size);
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
	public CachingMapDecorator(Map targetMap) {
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
	public CachingMapDecorator(Map targetMap, boolean synchronize, boolean weak) {
		Assert.notNull(targetMap, "Target Map is required");
		this.targetMap = (synchronize ? Collections.synchronizedMap(targetMap) : targetMap);
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
		Object valueToCheck = value;
		if (valueToCheck == null) {
			valueToCheck = NULL_VALUE;
		}
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
		for (Iterator it = this.targetMap.values().iterator(); it.hasNext();) {
			Object mapVal = it.next();
			if (mapVal instanceof Reference && value.equals(((Reference) mapVal).get())) {
				return true;
			}
		}
		return false;
	}

	public Object remove(Object key) {
		return this.targetMap.remove(key);
	}

	public void putAll(Map map) {
		this.targetMap.putAll(map);
	}

	public void clear() {
		this.targetMap.clear();
	}

	public Set keySet() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return new LinkedHashSet(this.targetMap.keySet());
			}
		}
		else {
			return new LinkedHashSet(this.targetMap.keySet());
		}
	}

	public Collection values() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return valuesCopy();
			}
		}
		else {
			return valuesCopy();
		}
	}

	private Collection valuesCopy() {
		LinkedList values = new LinkedList();
		for (Iterator it = this.targetMap.values().iterator(); it.hasNext();) {
			Object value = it.next();
			values.add(value instanceof Reference ? ((Reference) value).get() : value);
		}
		return values;
	}

	public Set entrySet() {
		if (this.synchronize) {
			synchronized (this.targetMap) {
				return new LinkedHashSet(this.targetMap.entrySet());
			}
		}
		else {
			return new LinkedHashSet(this.targetMap.entrySet());
		}
	}


	/**
	 * Put an object into the cache, possibly wrapping it with a weak
	 * reference.
	 * @see #useWeakValue(Object, Object)
	 */
	public Object put(Object key, Object value) {
		Object newValue = value;
		if (newValue == null) {
			newValue = NULL_VALUE;
		}
		if (useWeakValue(key, newValue)) {
			newValue = new WeakReference(newValue);
		}
		return this.targetMap.put(key, newValue);
	}

	/**
	 * Decide whether use a weak reference for the value of
	 * the given key-value pair.
	 * @param key the candidate key
	 * @param value the candidate value
	 * @return <code>true</code> in order to use a weak reference;
	 * <code>false</code> otherwise.
	 */
	protected boolean useWeakValue(Object key, Object value) {
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
	public Object get(Object key) {
		Object value = this.targetMap.get(key);
		if (value instanceof Reference) {
			value = ((Reference) value).get();
		}
		if (value == null) {
			value = create(key);
			if (value != null) {
				put(key, value);
			}
		}
		return (value == NULL_VALUE ? null : value);
	}

	/**
	 * Create a value to cache for the given key.
	 * Called by <code>get</code> if there is no value cached already.
	 * @param key the cache key
	 * @see #get(Object)
	 */
	protected Object create(Object key) {
		return null;
	}


	public String toString() {
		return "CachingMapDecorator [" + getClass().getName() + "]:" + this.targetMap;
	}

}
