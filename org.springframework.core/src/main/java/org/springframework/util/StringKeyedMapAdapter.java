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

import java.util.AbstractSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * Base class for map adapters whose keys are String values.
 * Concrete classes need only implement the abstract hook methods defined by this class.
 * @author Keith Donald
 */
public abstract class StringKeyedMapAdapter<V> implements Map<String, V> {

	private Set<String> keySet;

	private Collection<V> values;

	private Set<Map.Entry<String, V>> entrySet;


	// implementing Map

	public void clear() {
		for (Iterator<String> it = getAttributeNames(); it.hasNext();) {
			removeAttribute((String) it.next());
		}
	}

	public boolean containsKey(Object key) {
		return getAttribute(key.toString()) != null;
	}

	public boolean containsValue(Object value) {
		if (value == null) {
			return false;
		}
		for (Iterator<String> it = getAttributeNames(); it.hasNext();) {
			Object aValue = getAttribute((String) it.next());
			if (value.equals(aValue)) {
				return true;
			}
		}
		return false;
	}

	public Set<Map.Entry<String, V>> entrySet() {
		return (entrySet != null) ? entrySet : (entrySet = new EntrySet());
	}

	public V get(Object key) {
		return getAttribute(key.toString());
	}

	public boolean isEmpty() {
		return !getAttributeNames().hasNext();
	}

	public Set<String> keySet() {
		return (keySet != null) ? keySet : (keySet = new KeySet());
	}

	public V put(String key, V value) {
		String stringKey = String.valueOf(key);
		V previousValue = getAttribute(stringKey);
		setAttribute(stringKey, value);
		return previousValue;
	}

	public void putAll(Map<? extends String, ? extends V> map) {
		for (Map.Entry<? extends String, ? extends V> entry : map.entrySet()) {
			setAttribute(entry.getKey().toString(), entry.getValue());
		}
	}

	public V remove(Object key) {
		String stringKey = key.toString();
		V retval = getAttribute(stringKey);
		removeAttribute(stringKey);
		return retval;
	}

	public int size() {
		int size = 0;
		for (Iterator<String> it = getAttributeNames(); it.hasNext();) {
			size++;
			it.next();
		}
		return size;
	}

	public Collection<V> values() {
		return (values != null) ? values : (values = new Values());
	}


	// hook methods

	/**
	 * Hook method that needs to be implemented by concrete subclasses. Gets a value associated with a key.
	 * @param key the key to lookup
	 * @return the associated value, or null if none
	 */
	protected abstract V getAttribute(String key);

	/**
	 * Hook method that needs to be implemented by concrete subclasses. Puts a key-value pair in the map, overwriting
	 * any possible earlier value associated with the same key.
	 * @param key the key to associate the value with
	 * @param value the value to associate with the key
	 */
	protected abstract void setAttribute(String key, V value);

	/**
	 * Hook method that needs to be implemented by concrete subclasses. Removes a key and its associated value from the
	 * map.
	 * @param key the key to remove
	 */
	protected abstract void removeAttribute(String key);

	/**
	 * Hook method that needs to be implemented by concrete subclasses. Returns an enumeration listing all keys known to
	 * the map.
	 * @return the key enumeration
	 */
	protected abstract Iterator<String> getAttributeNames();


	// internal helper classes

	private class KeySet extends AbstractSet<String> {

		public boolean isEmpty() {
			return StringKeyedMapAdapter.this.isEmpty();
		}

		public int size() {
			return StringKeyedMapAdapter.this.size();
		}

		public void clear() {
			StringKeyedMapAdapter.this.clear();
		}
		
		public Iterator<String> iterator() {
			return new KeyIterator();
		}

		public boolean contains(Object o) {
			return StringKeyedMapAdapter.this.containsKey(o);
		}

		public boolean remove(Object o) {
			return StringKeyedMapAdapter.this.remove(o) != null;
		}
	}


	private class KeyIterator implements Iterator<String> {
		
		private final Iterator<String> it = getAttributeNames();

		private String currentKey;

		public boolean hasNext() {
			return it.hasNext();
		}

		public String next() {
			return currentKey = it.next();
		}
		
		public void remove() {
			if (currentKey == null) {
				throw new NoSuchElementException("You must call next() at least once");
			}
			StringKeyedMapAdapter.this.remove(currentKey);
		}
	}


	private class Values extends AbstractSet<V> {

		public boolean isEmpty() {
			return StringKeyedMapAdapter.this.isEmpty();
		}

		public int size() {
			return StringKeyedMapAdapter.this.size();
		}

		public void clear() {
			StringKeyedMapAdapter.this.clear();
		}
		
		public Iterator<V> iterator() {
			return new ValuesIterator();
		}

		public boolean contains(Object o) {
			return StringKeyedMapAdapter.this.containsValue(o);
		}

		public boolean remove(Object o) {
			if (o == null) {
				return false;
			}
			for (Iterator<V> it = iterator(); it.hasNext();) {
				if (o.equals(it.next())) {
					it.remove();
					return true;
				}
			}
			return false;
		}
	}


	private class ValuesIterator implements Iterator<V> {
		
		private final Iterator<String> it = getAttributeNames();

		private String currentKey;

		public boolean hasNext() {
			return it.hasNext();
		}

		public V next() {
			currentKey = it.next();
			return StringKeyedMapAdapter.this.get(currentKey);
		}
		
		public void remove() {
			if (currentKey == null) {
				throw new NoSuchElementException("You must call next() at least once");
			}
			StringKeyedMapAdapter.this.remove(currentKey);
		}
	}


	private class EntrySet extends AbstractSet<Map.Entry<String, V>> {

		public boolean isEmpty() {
			return StringKeyedMapAdapter.this.isEmpty();
		}

		public int size() {
			return StringKeyedMapAdapter.this.size();
		}

		public void clear() {
			StringKeyedMapAdapter.this.clear();
		}
		
		public Iterator<Map.Entry<String, V>> iterator() {
			return new EntryIterator();
		}

		@SuppressWarnings("unchecked")
		public boolean contains(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry entry = (Entry) o;
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key == null || value == null) {
				return false;
			}
			return value.equals(StringKeyedMapAdapter.this.get(key));
		}

		@SuppressWarnings("unchecked")
		public boolean remove(Object o) {
			if (!(o instanceof Entry)) {
				return false;
			}
			Entry entry = (Entry) o;
			Object key = entry.getKey();
			Object value = entry.getValue();
			if (key == null || value == null || !value.equals(StringKeyedMapAdapter.this.get(key))) {
				return false;
			}
			return StringKeyedMapAdapter.this.remove(((Entry) o).getKey()) != null;
		}
	}


	private class EntryIterator implements Iterator<Map.Entry<String, V>> {
		
		private final Iterator<String> it = getAttributeNames();

		private String currentKey;

		public boolean hasNext() {
			return it.hasNext();
		}

		public Map.Entry<String, V> next() {
			currentKey = it.next();
			return new EntrySetEntry(currentKey);
		}
		
		public void remove() {
			if (currentKey == null) {
				throw new NoSuchElementException("You must call next() at least once");
			}
			StringKeyedMapAdapter.this.remove(currentKey);
		}
		
	}


	private class EntrySetEntry implements Entry<String, V> {
		
		private final String currentKey;

		public EntrySetEntry(String currentKey) {
			this.currentKey = currentKey;
		}

		public String getKey() {
			return currentKey;
		}

		public V getValue() {
			return StringKeyedMapAdapter.this.get(currentKey);
		}

		public V setValue(V value) {
			return StringKeyedMapAdapter.this.put(currentKey, value);
		}
	}

}
