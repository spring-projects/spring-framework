/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.util;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

/**
 * Simple LRU (Least Recently Used) cache, bounded by a specified cache limit.
 *
 * <p>This implementation is backed by a {@code ConcurrentHashMap} for storing
 * the cached values and a {@code ConcurrentLinkedDeque} for ordering the keys
 * and choosing the least recently used key when the cache is at full capacity.
 *
 * @author Brian Clozel
 * @author Juergen Hoeller
 * @since 5.3
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values
 * @see #get
 */
public class ConcurrentLruCache<K, V> {

	private final int sizeLimit;

	private final Function<K, V> generator;

	private final ConcurrentHashMap<K, V> cache = new ConcurrentHashMap<>();

	private final ConcurrentLinkedDeque<K> queue = new ConcurrentLinkedDeque<>();

	private final ReadWriteLock lock = new ReentrantReadWriteLock();

	private volatile int size;


	/**
	 * Create a new cache instance with the given limit and generator function.
	 * @param sizeLimit the maximum number of entries in the cache
	 * (0 indicates no caching, always generating a new value)
	 * @param generator a function to generate a new value for a given key
	 */
	public ConcurrentLruCache(int sizeLimit, Function<K, V> generator) {
		Assert.isTrue(sizeLimit >= 0, "Cache size limit must not be negative");
		Assert.notNull(generator, "Generator function must not be null");
		this.sizeLimit = sizeLimit;
		this.generator = generator;
	}


	/**
	 * Retrieve an entry from the cache, potentially triggering generation
	 * of the value.
	 * @param key the key to retrieve the entry for
	 * @return the cached or newly generated value
	 */
	public V get(K key) {
		if (this.sizeLimit == 0) {
			return this.generator.apply(key);
		}

		V cached = this.cache.get(key);
		if (cached != null) {
			if (this.size < this.sizeLimit) {
				return cached;
			}
			this.lock.readLock().lock();
			try {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			finally {
				this.lock.readLock().unlock();
			}
		}

		this.lock.writeLock().lock();
		try {
			// Retrying in case of concurrent reads on the same key
			cached = this.cache.get(key);
			if (cached != null) {
				if (this.queue.removeLastOccurrence(key)) {
					this.queue.offer(key);
				}
				return cached;
			}
			// Generate value first, to prevent size inconsistency
			V value = this.generator.apply(key);
			if (this.size == this.sizeLimit) {
				K leastUsed = this.queue.poll();
				if (leastUsed != null) {
					this.cache.remove(leastUsed);
				}
			}
			this.queue.offer(key);
			this.cache.put(key, value);
			this.size = this.cache.size();
			return value;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Determine whether the given key is present in this cache.
	 * @param key the key to check for
	 * @return {@code true} if the key is present,
	 * {@code false} if there was no matching key
	 */
	public boolean contains(K key) {
		return this.cache.containsKey(key);
	}

	/**
	 * Immediately remove the given key and any associated value.
	 * @param key the key to evict the entry for
	 * @return {@code true} if the key was present before,
	 * {@code false} if there was no matching key
	 */
	public boolean remove(K key) {
		this.lock.writeLock().lock();
		try {
			boolean wasPresent = (this.cache.remove(key) != null);
			this.queue.remove(key);
			this.size = this.cache.size();
			return wasPresent;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Immediately remove all entries from this cache.
	 */
	public void clear() {
		this.lock.writeLock().lock();
		try {
			this.cache.clear();
			this.queue.clear();
			this.size = 0;
		}
		finally {
			this.lock.writeLock().unlock();
		}
	}

	/**
	 * Return the current size of the cache.
	 * @see #sizeLimit()
	 */
	public int size() {
		return this.size;
	}

	/**
	 * Return the maximum number of entries in the cache
	 * (0 indicates no caching, always generating a new value).
	 * @see #size()
	 */
	public int sizeLimit() {
		return this.sizeLimit;
	}

}
