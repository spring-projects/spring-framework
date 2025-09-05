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

package org.springframework.util;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicLongArray;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

import org.jspecify.annotations.Nullable;

/**
 * Simple LRU (Least Recently Used) cache, bounded by a specified cache capacity.
 * <p>This is a simplified, opinionated implementation of an LRU cache for internal
 * use in Spring Framework. It is inspired from
 * <a href="https://github.com/ben-manes/concurrentlinkedhashmap">ConcurrentLinkedHashMap</a>.
 * <p>Read and write operations are internally recorded in dedicated buffers,
 * then drained at chosen times to avoid contention. The implementation includes
 * performance monitoring and statistics collection capabilities.
 *
 * @author Brian Clozel
 * @author Ben Manes
 * @author JongJun Kim
 * @since 5.3
 * @param <K> the type of the key used for cache retrieval
 * @param <V> the type of the cached values, does not allow null values
 * @see #get(Object)
 */
@SuppressWarnings({"unchecked", "NullAway"})
public final class ConcurrentLruCache<K, V> {

	private final int capacity;
	
	private final boolean statsEnabled;

	private final AtomicInteger currentSize = new AtomicInteger();

	private final ConcurrentMap<K, Node<K, V>> cache;

	private final Function<K, V> generator;

	private final ReadOperations<K, V> readOperations;

	private final WriteOperations writeOperations;

	private final Lock evictionLock = new ReentrantLock();

	/*
	 * Queue that contains all ACTIVE cache entries, ordered with least recently used entries first.
	 * Read and write operations are buffered and periodically processed to reorder the queue.
	 */
	private final EvictionQueue<K, V> evictionQueue = new EvictionQueue<>();

	private final AtomicReference<DrainStatus> drainStatus = new AtomicReference<>(DrainStatus.IDLE);

	// Performance statistics (null if stats disabled)
	@Nullable
	private final AtomicLong hits;
	@Nullable
	private final AtomicLong misses;
	@Nullable
	private final AtomicLong evictions;
	

	/**
	 * Create a new cache instance with the given capacity and generator function.
	 * Statistics collection is enabled by default for backward compatibility.
	 * @param capacity the maximum number of entries in the cache
	 * (0 indicates no caching, always generating a new value)
	 * @param generator a function to generate a new value for a given key
	 */
	public ConcurrentLruCache(int capacity, Function<K, V> generator) {
		this(capacity, generator, true);
	}

	/**
	 * Create a new cache instance with the given capacity, generator function, and statistics setting.
	 * @param capacity the maximum number of entries in the cache
	 * (0 indicates no caching, always generating a new value)
	 * @param generator a function to generate a new value for a given key
	 * @param enableStatistics whether to collect performance statistics
	 */
	public ConcurrentLruCache(int capacity, Function<K, V> generator, boolean enableStatistics) {
		this(capacity, generator, enableStatistics, 16);
	}

	private ConcurrentLruCache(int capacity, Function<K, V> generator, boolean enableStatistics, int concurrencyLevel) {
		Assert.isTrue(capacity >= 0, "Capacity must be >= 0");
		this.capacity = capacity;
		this.statsEnabled = enableStatistics;
		this.cache = new ConcurrentHashMap<>(16, 0.75f, concurrencyLevel);
		this.generator = generator;
		this.readOperations = new ReadOperations<>(this.evictionQueue);
		this.writeOperations = new WriteOperations();
		
		// Initialize statistics fields only if enabled
		if (enableStatistics) {
			this.hits = new AtomicLong();
			this.misses = new AtomicLong();
			this.evictions = new AtomicLong();
		} else {
			this.hits = null;
			this.misses = null;
			this.evictions = null;
		}
	}

	/**
	 * Retrieve an entry from the cache, potentially triggering generation of the value.
	 * @param key the key to retrieve the entry for
	 * @return the cached or newly generated value
	 */
	public V get(K key) {
		if (this.capacity == 0) {
			return this.generator.apply(key);
		}
		final Node<K, V> node = this.cache.get(key);
		if (node == null) {
			if (this.statsEnabled && this.misses != null) {
				this.misses.incrementAndGet();
			}
			V value = this.generator.apply(key);
			put(key, value);
			return value;
		}

		if (this.statsEnabled && this.hits != null) {
			this.hits.incrementAndGet();
		}
		processRead(node);
		return node.getValue();
	}

	private void put(K key, V value) {
		Assert.notNull(key, "key must not be null");
		Assert.notNull(value, "value must not be null");
		final CacheEntry<V> cacheEntry = new CacheEntry<>(value, CacheEntryState.ACTIVE);
		final Node<K, V> node = new Node<>(key, cacheEntry);
		final Node<K, V> prior = this.cache.putIfAbsent(node.key, node);
		if (prior == null) {
			processWrite(new AddTask(node));
		}
		else {
			processRead(prior);
		}
	}

	private void processRead(Node<K, V> node) {
		boolean drainRequested = this.readOperations.recordRead(node);
		final DrainStatus status = this.drainStatus.get();
		if (status.shouldDrainBuffers(drainRequested)) {
			drainOperations();
		}
	}

	private void processWrite(Runnable task) {
		this.writeOperations.add(task);
		this.drainStatus.lazySet(DrainStatus.REQUIRED);
		drainOperations();
	}

	private void drainOperations() {
		if (this.evictionLock.tryLock()) {
			try {
				this.drainStatus.lazySet(DrainStatus.PROCESSING);
				this.readOperations.drain();
				this.writeOperations.drain();
			}
			finally {
				this.drainStatus.compareAndSet(DrainStatus.PROCESSING, DrainStatus.IDLE);
				this.evictionLock.unlock();
			}
		}
	}

	/**
	 * Return the maximum number of entries in the cache.
	 * @see #size()
	 */
	public int capacity() {
		return this.capacity;
	}

	/**
	 * Return the maximum number of entries in the cache.
	 * @deprecated in favor of {@link #capacity()} as of 6.0.
	 */
	@Deprecated(since = "6.0")
	public int sizeLimit() {
		return this.capacity;
	}

	/**
	 * Return the current size of the cache.
	 * @see #capacity()
	 */
	public int size() {
		return this.cache.size();
	}

	/**
	 * Return cache performance statistics.
	 * @return statistics including hit ratio, hits, misses, and evictions
	 * @throws IllegalStateException if statistics collection is disabled
	 */
	public CacheStats getStats() {
		if (!this.statsEnabled) {
			throw new IllegalStateException("Statistics collection is disabled. " +
					"Create cache with enableStatistics=true to collect statistics.");
		}
		
		long totalHits = hits != null ? hits.get() : 0;
		long totalMisses = misses != null ? misses.get() : 0;
		long totalEvictions = evictions != null ? evictions.get() : 0;
		long totalRequests = totalHits + totalMisses;
		double hitRatio = totalRequests > 0 ? (double) totalHits / totalRequests : 0.0;
		
		return new CacheStats(
			totalHits,
			totalMisses,
			totalEvictions,
			hitRatio,
			size(),
			capacity
		);
	}
	
	/**
	 * Check if statistics collection is enabled for this cache.
	 * @return true if statistics are being collected, false otherwise
	 */
	public boolean isStatsEnabled() {
		return this.statsEnabled;
	}

	/**
	 * Immediately remove all entries from this cache.
	 */
	public void clear() {
		this.evictionLock.lock();
		try {
			Node<K, V> node;
			while ((node = this.evictionQueue.poll()) != null) {
				this.cache.remove(node.key, node);
				markAsRemoved(node);
			}
			this.readOperations.clear();
			this.writeOperations.drainAll();
		}
		finally {
			this.evictionLock.unlock();
		}
	}

	/*
	 * Transition the node to the {@code removed} state and decrement the current size of the cache.
	 */
	private void markAsRemoved(Node<K, V> node) {
		for (; ; ) {
			CacheEntry<V> current = node.get();
			CacheEntry<V> removed = new CacheEntry<>(current.value, CacheEntryState.REMOVED);
			if (node.compareAndSet(current, removed)) {
				this.currentSize.lazySet(this.currentSize.get() - 1);
				return;
			}
		}
	}

	/**
	 * Determine whether the given key is present in this cache.
	 * @param key the key to check for
	 * @return {@code true} if the key is present, {@code false} if there was no matching key
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
		final Node<K, V> node = this.cache.remove(key);
		if (node == null) {
			return false;
		}
		markForRemoval(node);
		processWrite(new RemovalTask(node));
		return true;
	}

	/*
	 * Transition the node from the {@code active} state to the {@code pending removal} state,
	 * if the transition is valid.
	 */
	private void markForRemoval(Node<K, V> node) {
		for (; ; ) {
			final CacheEntry<V> current = node.get();
			if (!current.isActive()) {
				return;
			}
			final CacheEntry<V> pendingRemoval = new CacheEntry<>(current.value, CacheEntryState.PENDING_REMOVAL);
			if (node.compareAndSet(current, pendingRemoval)) {
				return;
			}
		}
	}

	/**
	 * Write operation recorded when a new entry is added to the cache.
	 */
	private final class AddTask implements Runnable {
		final Node<K, V> node;

		AddTask(Node<K, V> node) {
			this.node = node;
		}

		@Override
		public void run() {
			currentSize.lazySet(currentSize.get() + 1);
			if (this.node.get().isActive()) {
				evictionQueue.add(this.node);
				evictEntries();
			}
		}

		private void evictEntries() {
			while (currentSize.get() > capacity) {
				final Node<K, V> node = evictionQueue.poll();
				if (node == null) {
					return;
				}
				cache.remove(node.key, node);
				markAsRemoved(node);
				if (ConcurrentLruCache.this.statsEnabled && ConcurrentLruCache.this.evictions != null) {
					ConcurrentLruCache.this.evictions.incrementAndGet();
				}
			}
		}

	}


	/**
	 * Write operation recorded when an entry is removed to the cache.
	 */
	private final class RemovalTask implements Runnable {
		final Node<K, V> node;

		RemovalTask(Node<K, V> node) {
			this.node = node;
		}

		@Override
		public void run() {
			evictionQueue.remove(this.node);
			markAsRemoved(this.node);
		}
	}


	/*
	 * Draining status for the read/write buffers.
	 */
	private enum DrainStatus {

		/*
		 * No drain operation currently running.
		 */
		IDLE {
			@Override
			boolean shouldDrainBuffers(boolean delayable) {
				return !delayable;
			}
		},

		/*
		 * A drain operation is required due to a pending write modification.
		 */
		REQUIRED {
			@Override
			boolean shouldDrainBuffers(boolean delayable) {
				return true;
			}
		},

		/*
		 * A drain operation is in progress.
		 */
		PROCESSING {
			@Override
			boolean shouldDrainBuffers(boolean delayable) {
				return false;
			}
		};

		/**
		 * Determine whether the buffers should be drained.
		 * @param delayable if a drain should be delayed until required
		 * @return if a drain should be attempted
		 */
		abstract boolean shouldDrainBuffers(boolean delayable);
	}

	private enum CacheEntryState {
		ACTIVE, PENDING_REMOVAL, REMOVED
	}

	private record CacheEntry<V>(V value, CacheEntryState state) {

		boolean isActive() {
			return this.state == CacheEntryState.ACTIVE;
		}
	}

	private static final class ReadOperations<K, V> {

		private static final int BUFFER_COUNT = detectNumberOfBuffers();

		private static int detectNumberOfBuffers() {
			int availableProcessors = Runtime.getRuntime().availableProcessors();
			int nextPowerOfTwo = 1 << (Integer.SIZE - Integer.numberOfLeadingZeros(availableProcessors - 1));
			return Math.min(4, nextPowerOfTwo);
		}

		private static final int BUFFERS_MASK = BUFFER_COUNT - 1;

		private static final int MAX_PENDING_OPERATIONS = 32;

		private static final int MAX_DRAIN_COUNT = 2 * MAX_PENDING_OPERATIONS;

		private static final int BUFFER_SIZE = 2 * MAX_DRAIN_COUNT;

		private static final int BUFFER_INDEX_MASK = BUFFER_SIZE - 1;

		/*
		 * Number of operations recorded, for each buffer
		 */
		private final AtomicLongArray recordedCount = new AtomicLongArray(BUFFER_COUNT);

		/*
		 * Number of operations read, for each buffer
		 */
		private final long[] readCount = new long[BUFFER_COUNT];

		/*
		 * Number of operations processed, for each buffer
		 */
		private final AtomicLongArray processedCount = new AtomicLongArray(BUFFER_COUNT);

		@SuppressWarnings("rawtypes")
		private final AtomicReferenceArray<Node<K, V>>[] buffers = new AtomicReferenceArray[BUFFER_COUNT];

		private final EvictionQueue<K, V> evictionQueue;

		ReadOperations(EvictionQueue<K, V> evictionQueue) {
			this.evictionQueue = evictionQueue;
			for (int i = 0; i < BUFFER_COUNT; i++) {
				this.buffers[i] = new AtomicReferenceArray<>(BUFFER_SIZE);
			}
		}

		@SuppressWarnings("deprecation")  // for Thread.getId() on JDK 19
		private static int getBufferIndex() {
			return ((int) Thread.currentThread().getId()) & BUFFERS_MASK;
		}

		boolean recordRead(Node<K, V> node) {
			int bufferIndex = getBufferIndex();
			final long writeCount = this.recordedCount.get(bufferIndex);
			this.recordedCount.lazySet(bufferIndex, writeCount + 1);
			final int index = (int) (writeCount & BUFFER_INDEX_MASK);
			this.buffers[bufferIndex].lazySet(index, node);
			final long pending = (writeCount - this.processedCount.get(bufferIndex));
			return (pending < MAX_PENDING_OPERATIONS);
		}

		@SuppressWarnings("deprecation")  // for Thread.getId() on JDK 19
		void drain() {
			final int start = (int) Thread.currentThread().getId();
			final int end = start + BUFFER_COUNT;
			for (int i = start; i < end; i++) {
				drainReadBuffer(i & BUFFERS_MASK);
			}
		}

		void clear() {
			for (int i = 0; i < BUFFER_COUNT; i++) {
				AtomicReferenceArray<Node<K, V>> buffer = this.buffers[i];
				for (int j = 0; j < BUFFER_SIZE; j++) {
					buffer.lazySet(j, null);
				}
			}
		}

		private void drainReadBuffer(int bufferIndex) {
			final long writeCount = this.recordedCount.get(bufferIndex);
			for (int i = 0; i < MAX_DRAIN_COUNT; i++) {
				final int index = (int) (this.readCount[bufferIndex] & BUFFER_INDEX_MASK);
				final AtomicReferenceArray<Node<K, V>> buffer = this.buffers[bufferIndex];
				final Node<K, V> node = buffer.get(index);
				if (node == null) {
					break;
				}
				buffer.lazySet(index, null);
				this.evictionQueue.moveToBack(node);
				this.readCount[bufferIndex]++;
			}
			this.processedCount.lazySet(bufferIndex, writeCount);
		}
	}

	private static final class WriteOperations {

		private static final int DRAIN_THRESHOLD = 16;

		private final Queue<Runnable> operations = new ConcurrentLinkedQueue<>();

		public void add(Runnable task) {
			this.operations.add(task);
		}

		public void drain() {
			for (int i = 0; i < DRAIN_THRESHOLD; i++) {
				final Runnable task = this.operations.poll();
				if (task == null) {
					break;
				}
				task.run();
			}
		}

		public void drainAll() {
			Runnable task;
			while ((task = this.operations.poll()) != null) {
				task.run();
			}
		}

	}

	@SuppressWarnings("serial")
	private static final class Node<K, V> extends AtomicReference<CacheEntry<V>> {
		final K key;

		@Nullable Node<K, V> prev;

		@Nullable Node<K, V> next;

		Node(K key, CacheEntry<V> cacheEntry) {
			super(cacheEntry);
			this.key = key;
		}

		public @Nullable Node<K, V> getPrevious() {
			return this.prev;
		}

		public void setPrevious(@Nullable Node<K, V> prev) {
			this.prev = prev;
		}

		public @Nullable Node<K, V> getNext() {
			return this.next;
		}

		public void setNext(@Nullable Node<K, V> next) {
			this.next = next;
		}

		V getValue() {
			return get().value;
		}
	}


	private static final class EvictionQueue<K, V> {

		@Nullable Node<K, V> first;

		@Nullable Node<K, V> last;


		@Nullable Node<K, V> poll() {
			if (this.first == null) {
				return null;
			}
			final Node<K, V> f = this.first;
			final Node<K, V> next = f.getNext();
			f.setNext(null);

			this.first = next;
			if (next == null) {
				this.last = null;
			}
			else {
				next.setPrevious(null);
			}
			return f;
		}

		void add(Node<K, V> e) {
			if (contains(e)) {
				return;
			}
			linkLast(e);
		}

		private boolean contains(Node<K, V> e) {
			return (e.getPrevious() != null) || (e.getNext() != null) || (e == this.first);
		}

		private void linkLast(final Node<K, V> e) {
			final Node<K, V> l = this.last;
			this.last = e;

			if (l == null) {
				this.first = e;
			}
			else {
				l.setNext(e);
				e.setPrevious(l);
			}
		}

		private void unlink(Node<K, V> e) {
			final Node<K, V> prev = e.getPrevious();
			final Node<K, V> next = e.getNext();
			if (prev == null) {
				this.first = next;
			}
			else {
				prev.setNext(next);
				e.setPrevious(null);
			}
			if (next == null) {
				this.last = prev;
			}
			else {
				next.setPrevious(prev);
				e.setNext(null);
			}
		}

		void moveToBack(Node<K, V> e) {
			if (contains(e) && e != this.last) {
				unlink(e);
				linkLast(e);
			}
		}

		void remove(Node<K, V> e) {
			if (contains(e)) {
				unlink(e);
			}
		}

	}

	/**
	 * Cache statistics holder for performance monitoring.
	 */
	public static final class CacheStats {
		private final long hits;
		private final long misses;
		private final long evictions;
		private final double hitRatio;
		private final int size;
		private final int capacity;

		CacheStats(long hits, long misses, long evictions, double hitRatio, int size, int capacity) {
			this.hits = hits;
			this.misses = misses;
			this.evictions = evictions;
			this.hitRatio = hitRatio;
			this.size = size;
			this.capacity = capacity;
		}

		public long getHits() { return hits; }
		public long getMisses() { return misses; }
		public long getEvictions() { return evictions; }
		public double getHitRatio() { return hitRatio; }
		public int getSize() { return size; }
		public int getCapacity() { return capacity; }
		public long getRequests() { return hits + misses; }

		@Override
		public String toString() {
			return String.format(
				"CacheStats{hits=%d, misses=%d, evictions=%d, hitRatio=%.3f, size=%d/%d}",
				hits, misses, evictions, hitRatio, size, capacity);
		}
	}

}
