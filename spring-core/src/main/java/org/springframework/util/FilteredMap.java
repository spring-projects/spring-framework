/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.util;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

/**
 * Map that filters out values that do not match a predicate.
 * This type is used by {@link CompositeMap}.
 *
 * @author Arjen Poutsma
 * @author Yanming Zhou
 * @since 6.2
 * @param <K> the type of keys maintained by this map
 * @param <V> the type of mapped values
 */
final class FilteredMap<K, V extends @Nullable Object> extends AbstractMap<K, V> {

	private final Map<K, V> delegate;

	private final Predicate<K> filter;


	public FilteredMap(Map<K, V> delegate, Predicate<K> filter) {
		Assert.notNull(delegate, "Delegate must not be null");
		Assert.notNull(filter, "Filter must not be null");
		this.delegate = delegate;
		this.filter = filter;
	}


	@Override
	public Set<Entry<K, V>> entrySet() {
		return new FilteredSet<>(this.delegate.entrySet(), entry -> this.filter.test(entry.getKey()));
	}

	@Override
	public int size() {
		int size = 0;
		for (K k : this.delegate.keySet()) {
			if (this.filter.test(k)) {
				size++;
			}
		}
		return size;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean containsKey(Object key) {
		if (this.delegate.containsKey(key)) {
			return this.filter.test((K) key);
		}
		else {
			return false;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable V get(Object key) {
		V value = this.delegate.get(key);
		if (value != null && this.filter.test((K) key)) {
			return value;
		}
		else {
			return null;
		}
	}

	@Override
	public @Nullable V put(K key, V value) {
		V oldValue = this.delegate.put(key, value);
		if (oldValue != null && this.filter.test(key)) {
			return oldValue;
		}
		else {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public @Nullable V remove(Object key) {
		V oldValue = this.delegate.remove(key);
		if (oldValue != null && this.filter.test((K) key)) {
			return oldValue;
		}
		else {
			return null;
		}
	}

	@Override
	public void clear() {
		this.delegate.clear();
	}

	@Override
	public Set<K> keySet() {
		return new FilteredSet<>(this.delegate.keySet(), this.filter);
	}

}
