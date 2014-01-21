/*
 * Copyright 2002-2013 the original author or authors.
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

import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Array;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A {@link ConcurrentHashMap} that uses {@link ReferenceType#SOFT soft} or
 * {@linkplain ReferenceType#WEAK weak} references for both {@code keys} and {@code values}.
 *
 * <p>This class can be used as an alternative to
 * {@code Collections.synchronizedMap(new WeakHashMap<K, Reference<V>>())} in order to
 * support better performance when accessed concurrently. This implementation follows the
 * same design constraints as {@link ConcurrentHashMap} with the exception that
 * {@code null} values and {@code null} keys are supported.
 *
 * <p><b>NOTE:</b> The use of references means that there is no guarantee that items
 * placed into the map will be subsequently available. The garbage collector may discard
 * references at any time, so it may appear that an unknown thread is silently removing
 * entries.
 *
 * <p>If not explicitly specified, this implementation will use
 * {@linkplain SoftReference soft entry references}.
 *
 * @param <K> The key type
 * @param <V> The value type
 * @author Phillip Webb
 * @since 3.2
 */
public class ConcurrentReferenceHashMap<K, V> extends AbstractMap<K, V> implements ConcurrentMap<K, V> {

	private static final int DEFAULT_INITIAL_CAPACITY = 16;

	private static final float DEFAULT_LOAD_FACTOR = 0.75f;

	private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

	private static final ReferenceType DEFAULT_REFERENCE_TYPE = ReferenceType.SOFT;

	private static final int MAXIMUM_CONCURRENCY_LEVEL = 1 << 16;

	private static final int MAXIMUM_SEGMENT_SIZE = 1 << 30;


	/**
	 * Array of segments indexed using the high order bits from the hash.
	 */
	private final Segment[] segments;

	/**
	 * When the average number of references per table exceeds this value resize will be attempted.
	 */
	private final float loadFactor;

	/**
	 * The reference type: SOFT or WEAK.
	 */
	private final ReferenceType referenceType;

	/**
	 * The shift value used to calculate the size of the segments array and an index from the hash.
	 */
	private final int shift;

	/**
	 * Late binding entry set.
	 */
	private Set<Map.Entry<K, V>> entrySet;


	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 */
	public ConcurrentReferenceHashMap() {
		this(DEFAULT_INITIAL_CAPACITY, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 * @param initialCapacity the initial capacity of the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per table
	 * exceeds this value resize will be attempted
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor) {
		this(initialCapacity, loadFactor, DEFAULT_CONCURRENCY_LEVEL, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 * @param initialCapacity the initial capacity of the map
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, int concurrencyLevel) {
		this(initialCapacity, DEFAULT_LOAD_FACTOR, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per
	 * table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 */
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel) {
		this(initialCapacity, loadFactor, concurrencyLevel, DEFAULT_REFERENCE_TYPE);
	}

	/**
	 * Create a new {@code ConcurrentReferenceHashMap} instance.
	 * @param initialCapacity the initial capacity of the map
	 * @param loadFactor the load factor. When the average number of references per
	 * table exceeds this value, resize will be attempted.
	 * @param concurrencyLevel the expected number of threads that will concurrently
	 * write to the map
	 * @param referenceType the reference type used for entries
	 */
	@SuppressWarnings("unchecked")
	public ConcurrentReferenceHashMap(int initialCapacity, float loadFactor, int concurrencyLevel,
			ReferenceType referenceType) {

		Assert.isTrue(initialCapacity >= 0, "Initial capacity must not be negative");
		Assert.isTrue(loadFactor > 0f, "Load factor must be positive");
		Assert.isTrue(concurrencyLevel > 0, "Concurrency level must be positive");
		Assert.notNull(referenceType, "Reference type must not be null");
		this.loadFactor = loadFactor;
		this.shift = calculateShift(concurrencyLevel, MAXIMUM_CONCURRENCY_LEVEL);
		int size = 1 << this.shift;
		this.referenceType = referenceType;
		int roundedUpSegmentCapactity = (int) ((initialCapacity + size - 1L) / size);
		this.segments = (Segment[]) Array.newInstance(Segment.class, size);
		for (int i = 0; i < this.segments.length; i++) {
			this.segments[i] = new Segment(roundedUpSegmentCapactity);
		}
	}


	protected final float getLoadFactor() {
		return this.loadFactor;
	}

	protected final int getSegmentsSize() {
		return this.segments.length;
	}

	protected final Segment getSegment(int index) {
		return this.segments[index];
	}

	/**
	 * Factory method that returns the {@link ReferenceManager}. This method will be
	 * called once for each {@link Segment}.
	 * @return a new reference manager
	 */
	protected ReferenceManager createReferenceManager() {
		return new ReferenceManager();
	}

	/**
	 * Get the hash for a given object, apply an additional hash function to reduce
	 * collisions. This implementation uses the same Wang/Jenkins algorithm as
	 * {@link ConcurrentHashMap}. Subclasses can override to provide alternative hashing.
	 * @param o the object to hash (may be null)
	 * @return the resulting hash code
	 */
	protected int getHash(Object o) {
		int hash = o == null ? 0 : o.hashCode();
		hash += (hash << 15) ^ 0xffffcd7d;
		hash ^= (hash >>> 10);
		hash += (hash << 3);
		hash ^= (hash >>> 6);
		hash += (hash << 2) + (hash << 14);
		hash ^= (hash >>> 16);
		return hash;
	}

	@Override
	public V get(Object key) {
		Reference<K, V> reference = getReference(key, Restructure.WHEN_NECESSARY);
		Entry<K, V> entry = (reference == null ? null : reference.get());
		return (entry != null ? entry.getValue() : null);
	}

	@Override
	public boolean containsKey(Object key) {
		Reference<K, V> reference = getReference(key, Restructure.WHEN_NECESSARY);
		Entry<K, V> entry = (reference == null ? null : reference.get());
		return (entry != null && ObjectUtils.nullSafeEquals(entry.getKey(), key));
	}

	/**
	 * Returns a {@link Reference} to the {@link Entry} for the specified {@code key} or
	 * {@code null} if not found.
	 * @param key the key (can be {@code null})
	 * @param restructure types of restructure allowed during this call
	 * @return the reference or {@code null}
	 */
	protected final Reference<K, V> getReference(Object key, Restructure restructure) {
		int hash = getHash(key);
		return getSegmentForHash(hash).getReference(key, hash, restructure);
	}

	@Override
	public V put(K key, V value) {
		return put(key, value, true);
	}

	public V putIfAbsent(K key, V value) {
		return put(key, value, false);
	}

	private V put(final K key, final V value, final boolean overwriteExisting) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.RESIZE) {
			@Override
			protected V execute(Reference<K, V> reference, Entry<K, V> entry, Entries entries) {
				if (entry != null) {
					V previousValue = entry.getValue();
					if (overwriteExisting) {
						entry.setValue(value);
					}
					return previousValue;
				}
				entries.add(value);
				return null;
			}
		});
	}

	@Override
	public V remove(Object key) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected V execute(Reference<K, V> reference, Entry<K, V> entry) {
				if (entry != null) {
					reference.release();
					return entry.value;
				}
				return null;
			}
		});
	}

	public boolean remove(Object key, final Object value) {
		return doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_AFTER, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(Reference<K, V> reference, Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), value)) {
					reference.release();
					return true;
				}
				return false;
			}
		});
	}

	public boolean replace(K key, final V oldValue, final V newValue) {
		return doTask(key, new Task<Boolean>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected Boolean execute(Reference<K, V> reference, Entry<K, V> entry) {
				if (entry != null && ObjectUtils.nullSafeEquals(entry.getValue(), oldValue)) {
					entry.setValue(newValue);
					return true;
				}
				return false;
			}
		});
	}

	public V replace(K key, final V value) {
		return doTask(key, new Task<V>(TaskOption.RESTRUCTURE_BEFORE, TaskOption.SKIP_IF_EMPTY) {
			@Override
			protected V execute(Reference<K, V> reference, Entry<K, V> entry) {
				if (entry != null) {
					V previousValue = entry.getValue();
					entry.setValue(value);
					return previousValue;
				}
				return null;
			}
		});
	}

	@Override
	public void clear() {
		for (Segment segment : this.segments) {
			segment.clear();
		}
	}

	@Override
	public int size() {
		int size = 0;
		for (Segment segment : this.segments) {
			size += segment.getCount();
		}
		return size;
	}

	@Override
	public Set<java.util.Map.Entry<K, V>> entrySet() {
		if (this.entrySet == null) {
			this.entrySet = new EntrySet();
		}
		return this.entrySet;
	}

	private <T> T doTask(Object key, Task<T> task) {
		int hash = getHash(key);
		return getSegmentForHash(hash).doTask(hash, key, task);
	}

	private Segment getSegmentForHash(int hash) {
		return this.segments[(hash >>> (32 - this.shift)) & (this.segments.length - 1)];
	}

	/**
	 * Calculate a shift value that can be used to create a power-of-two value between
	 * the specified maximum and minimum values.
	 * @param minimumValue the minimum value
	 * @param maximumValue the maximum value
	 * @return the calculated shift (use {@code 1 << shift} to obtain a value)
	 */
	protected static int calculateShift(int minimumValue, int maximumValue) {
		int shift = 0;
		int value = 1;
		while (value < minimumValue && value < minimumValue) {
			value <<= 1;
			shift++;
		}
		return shift;
	}


	/**
	 * Various reference types supported by this map.
	 */
	public static enum ReferenceType {

		/**
		 * Use {@link SoftReference}s.
		 */
		SOFT,

		/**
		 * Use {@link WeakReference}s.
		 */
		WEAK
	}


	/**
	 * A single segment used to divide the map to allow better concurrent performance.
	 */
	@SuppressWarnings("serial")
	protected final class Segment extends ReentrantLock {

		private final ReferenceManager referenceManager;

		private final int initialSize;

		/**
		 * Array of references indexed using the low order bits from the hash. This
		 * property should only be set via {@link #setReferences} to ensure that the
		 * {@code resizeThreshold} is maintained.
		 */
		private volatile Reference<K, V>[] references;

		/**
		 * The total number of references contained in this segment. This includes chained
		 * references and references that have been garbage collected but not purged.
		 */
		private volatile int count = 0;

		/**
		 * The threshold when resizing of the references should occur. When {@code count}
		 * exceeds this value references will be resized.
		 */
		private int resizeThreshold;

		public Segment(int initialCapacity) {
			this.referenceManager = createReferenceManager();
			this.initialSize = 1 << calculateShift(initialCapacity, MAXIMUM_SEGMENT_SIZE);
			setReferences(createReferenceArray(this.initialSize));
		}

		public Reference<K, V> getReference(Object key, int hash, Restructure restructure) {
			if (restructure == Restructure.WHEN_NECESSARY) {
				restructureIfNecessary(false);
			}
			if (this.count == 0) {
				return null;
			}
			// Use a local copy to protect against other threads writing
			Reference<K, V>[] references = this.references;
			int index = getIndex(hash, references);
			Reference<K, V> head = references[index];
			return findInChain(head, key, hash);
		}

		/**
		 * Apply an update operation to this segment.  The segment will be locked
		 * during update.
		 * @param hash the hash of the key
		 * @param key the key
		 * @param task the update operation
		 * @return the result of the operation
		 */
		public <T> T doTask(final int hash, final Object key, final Task<T> task) {
			boolean resize = task.hasOption(TaskOption.RESIZE);
			if (task.hasOption(TaskOption.RESTRUCTURE_BEFORE)) {
				restructureIfNecessary(resize);
			}
			if (task.hasOption(TaskOption.SKIP_IF_EMPTY) && (this.count == 0)) {
				return task.execute(null, null, null);
			}
			lock();
			try {
				final int index = getIndex(hash, this.references);
				final Reference<K, V> head = this.references[index];
				Reference<K, V> reference = findInChain(head, key, hash);
				Entry<K, V> entry = (reference == null ? null : reference.get());
				Entries entries = new Entries() {
					@Override
					public void add(V value) {
						@SuppressWarnings("unchecked")
						Entry<K, V> newEntry = new Entry<K, V>((K)key, value);
						Reference<K, V> newReference = Segment.this.referenceManager.createReference(newEntry, hash, head);
						Segment.this.references[index] = newReference;
						Segment.this.count++;
					}
				};
				return task.execute(reference, entry, entries);
			}
			finally {
				unlock();
				if (task.hasOption(TaskOption.RESTRUCTURE_AFTER)) {
					restructureIfNecessary(resize);
				}
			}
		}

		/**
		 * Clear all items from this segment.
		 */
		public void clear() {
			if (this.count == 0) {
				return;
			}
			lock();
			try {
				setReferences(createReferenceArray(this.initialSize));
				this.count = 0;
			} finally {
				unlock();
			}
		}

		/**
		 * Restructure the underlying data structure when it becomes necessary. This
		 * method can increase the size of the references table as well as purge any
		 * references that have been garbage collected.
		 * @param allowResize if resizing is permitted
		 */
		private void restructureIfNecessary(boolean allowResize) {
			boolean needsResize = ((this.count > 0) && (this.count >= this.resizeThreshold));
			Reference<K, V> reference = this.referenceManager.pollForPurge();
			if ((reference != null) || (needsResize && allowResize)) {
				lock();
				try {
					int countAfterRestructure = this.count;

					Set<Reference<K, V>> toPurge = Collections.emptySet();
					if (reference != null) {
						toPurge = new HashSet<Reference<K, V>>();
						while (reference != null) {
							toPurge.add(reference);
							reference = this.referenceManager.pollForPurge();
						}
					}
					countAfterRestructure -= toPurge.size();

					// Recalculate taking into account count inside lock and items that
					// will be purged
					needsResize = ((countAfterRestructure > 0) && (countAfterRestructure >= this.resizeThreshold));
					boolean resizing = false;
					int restructureSize = this.references.length;
					if (allowResize && needsResize && (restructureSize < MAXIMUM_SEGMENT_SIZE)) {
						restructureSize <<= 1;
						resizing = true;
					}

					// Either create a new table or reuse the existing one
					Reference<K, V>[] restructured =  (resizing ? createReferenceArray(restructureSize) : this.references);

					// Restructure
					for (int i = 0; i < this.references.length; i++) {
						reference = this.references[i];
						if (!resizing) {
							restructured[i] = null;
						}
						while (reference != null) {
							if (!toPurge.contains(reference)) {
								int index = getIndex(reference.getHash(), restructured);
								restructured[index] = this.referenceManager.createReference(
										reference.get(), reference.getHash(),
										restructured[index]);
							}
							reference = reference.getNext();
						}
					}

					// Replace volatile members
					if (resizing) {
						setReferences(restructured);
					}
					this.count = countAfterRestructure;
				} finally {
					unlock();
				}
			}
		}

		private Reference<K, V> findInChain(Reference<K, V> reference, Object key, int hash) {
			while (reference != null) {
				if (reference.getHash() == hash) {
					Entry<K, V> entry = reference.get();
					if (entry != null) {
						K entryKey = entry.getKey();
						if (entryKey == key || entryKey.equals(key)) {
							return reference;
						}
					}
				}
				reference = reference.getNext();
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		private Reference<K, V>[] createReferenceArray(int size) {
			return (Reference<K, V>[]) Array.newInstance(Reference.class, size);
		}

		private int getIndex(int hash, Reference<K, V>[] references) {
			return hash & (references.length - 1);
		}

		/**
		 * Replace the references with a new value, recalculating the resizeThreshold.
		 * @param references the new references
		 */
		private void setReferences(Reference<K, V>[] references) {
			this.references = references;
			this.resizeThreshold = (int) (references.length * getLoadFactor());
		}

		/**
		 * @return the size of the current references array
		 */
		public final int getSize() {
			return this.references.length;
		}

		/**
		 * @return the total number of references in this segment
		 */
		public final int getCount() {
			return this.count;
		}
	}


	/**
	 * A reference to an {@link Entry} contained in the map. Implementations are usually
	 * wrappers around specific Java reference implementations (e.g., {@link SoftReference}).
	 */
	protected static interface Reference<K, V> {

		/**
		 * Returns the referenced entry or {@code null} if the entry is no longer
		 * available.
		 * @return the entry or {@code null}
		 */
		Entry<K, V> get();

		/**
		 * Returns the hash for the reference.
		 * @return the hash
		 */
		int getHash();

		/**
		 * Returns the next reference in the chain or {@code null}
		 * @return the next reference of {@code null}
		 */
		Reference<K, V> getNext();

		/**
		 * Release this entry and ensure that it will be returned from
		 * {@code ReferenceManager#pollForPurge()}.
		 */
		void release();
	}


	/**
	 * A single map entry.
	 */
	protected static final class Entry<K, V> implements Map.Entry<K, V> {

		private final K key;

		private volatile V value;

		public Entry(K key, V value) {
			this.key = key;
			this.value = value;
		}

		public K getKey() {
			return this.key;
		}

		public V getValue() {
			return this.value;
		}

		public V setValue(V value) {
			V previous = this.value;
			this.value = value;
			return previous;
		}

		@Override
		public String toString() {
			return this.key + "=" + this.value;
		}

		@Override
		@SuppressWarnings("rawtypes")
		public final boolean equals(Object o) {
			if (o == this) {
				return true;
			}
			if (o != null && o instanceof Map.Entry) {
				Map.Entry other = (Map.Entry) o;
				return ObjectUtils.nullSafeEquals(getKey(), other.getKey())
						&& ObjectUtils.nullSafeEquals(getValue(), other.getValue());
			}
			return false;
		}

		@Override
		public final int hashCode() {
			return ObjectUtils.nullSafeHashCode(this.key)
					^ ObjectUtils.nullSafeHashCode(this.value);
		}
	}


	/**
	 * A task that can be {@link Segment#doTask run} against a {@link Segment}.
	 */
	private abstract class Task<T> {

		private final EnumSet<TaskOption> options;

		public Task(TaskOption... options) {
			this.options = (options.length == 0 ? EnumSet.noneOf(TaskOption.class) : EnumSet.of(options[0], options));
		}

		public boolean hasOption(TaskOption option) {
			return this.options.contains(option);
		}

		/**
		 * Execute the task.
		 * @param reference the found reference or {@code null}
		 * @param entry the found entry or {@code null}
		 * @param entries access to the underlying entries
		 * @return the result of the task
		 * @see #execute(Reference, Entry)
		 */
		protected T execute(Reference<K, V> reference, Entry<K, V> entry, Entries entries) {
			return execute(reference, entry);
		}

		/**
		 * Convenience method that can be used for tasks that do not need access to {@link Entries}.
		 * @param reference the found reference or {@code null}
		 * @param entry the found entry or {@code null}
		 * @return the result of the task
		 * @see #execute(Reference, Entry, Entries)
		 */
		protected T execute(Reference<K, V> reference, Entry<K, V> entry) {
			return null;
		}
	}


	/**
	 * Various options supported by a {@code Task}.
	 */
	private static enum TaskOption {

		RESTRUCTURE_BEFORE, RESTRUCTURE_AFTER, SKIP_IF_EMPTY, RESIZE
	}


	/**
	 * Allows a task access to {@link Segment} entries.
	 */
	private abstract class Entries {

		/**
		 * Add a new entry with the specified value.
		 * @param value the value to add
		 */
		public abstract void add(V value);
	}


	/**
	 * Internal entry-set implementation.
	 */
	private class EntrySet extends AbstractSet<Map.Entry<K, V>> {

		@Override
		public Iterator<Map.Entry<K, V>> iterator() {
			return new EntryIterator();
		}

		@Override
		public boolean contains(Object o) {
			if (o != null && o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (java.util.Map.Entry<?, ?>) o;
				Reference<K, V> reference = ConcurrentReferenceHashMap.this.getReference(entry.getKey(), Restructure.NEVER);
				Entry<K, V> other = (reference == null ? null : reference.get());
				if (other != null) {
					return ObjectUtils.nullSafeEquals(entry.getValue(), other.getValue());
				}
			}
			return false;
		}

		@Override
		public boolean remove(Object o) {
			if (o instanceof Map.Entry<?, ?>) {
				Map.Entry<?, ?> entry = (Map.Entry<?, ?>) o;
				return ConcurrentReferenceHashMap.this.remove(entry.getKey(), entry.getValue());
			}
			return false;
		}

		@Override
		public int size() {
			return ConcurrentReferenceHashMap.this.size();
		}

		@Override
		public void clear() {
			ConcurrentReferenceHashMap.this.clear();
		}
	}


	/**
	 * Internal entry iterator implementation.
	 */
	private class EntryIterator implements Iterator<Map.Entry<K, V>> {

		private int segmentIndex;

		private int referenceIndex;

		private Reference<K, V>[] references;

		private Reference<K, V> reference;

		private Entry<K, V> next;

		private Entry<K, V> last;

		public EntryIterator() {
			moveToNextSegment();
		}

		public boolean hasNext() {
			getNextIfNecessary();
			return this.next != null;
		}

		public Entry<K, V> next() {
			getNextIfNecessary();
			if (this.next == null) {
				throw new NoSuchElementException();
			}
			this.last = this.next;
			this.next = null;
			return this.last;
		}

		private void getNextIfNecessary() {
			while (this.next == null) {
				moveToNextReference();
				if (this.reference == null) {
					return;
				}
				this.next = this.reference.get();
			}
		}

		private void moveToNextReference() {
			if (this.reference != null) {
				this.reference = this.reference.getNext();
			}
			while (this.reference == null && this.references != null) {
				if (this.referenceIndex >= this.references.length) {
					moveToNextSegment();
					this.referenceIndex = 0;
				}
				else {
					this.reference = this.references[this.referenceIndex];
					this.referenceIndex++;
				}
			}
		}

		private void moveToNextSegment() {
			this.reference = null;
			this.references = null;
			if (this.segmentIndex < ConcurrentReferenceHashMap.this.segments.length) {
				this.references = ConcurrentReferenceHashMap.this.segments[this.segmentIndex].references;
				this.segmentIndex++;
			}
		}

		public void remove() {
			Assert.state(this.last != null);
			ConcurrentReferenceHashMap.this.remove(this.last.getKey());
		}
	}


	/**
	 * The types of restructuring that can be performed.
	 */
	protected static enum Restructure {

		WHEN_NECESSARY, NEVER
	}


	/**
	 * Strategy class used to manage {@link Reference}s. This class can be overridden if
	 * alternative reference types need to be supported.
	 */
	protected class ReferenceManager {

		private final ReferenceQueue<Entry<K, V>> queue = new ReferenceQueue<Entry<K, V>>();

		/**
		 * Factory method used to create a new {@link Reference}.
		 * @param entry the entry contained in the reference
		 * @param hash the hash
		 * @param next the next reference in the chain or {@code null}
		 * @return a new {@link Reference}
		 */
		public Reference<K, V> createReference(Entry<K, V> entry, int hash, Reference<K, V> next) {
			if (ConcurrentReferenceHashMap.this.referenceType == ReferenceType.WEAK) {
				return new WeakEntryReference<K, V>(entry, hash, next, this.queue);
			}
			return new SoftEntryReference<K, V>(entry, hash, next, this.queue);
		}

		/**
		 * Return any reference that has been garbage collected and can be purged from the
		 * underlying structure or {@code null} if no references need purging. This
		 * method must be thread safe and ideally should not block when returning
		 * {@code null}. References should be returned once and only once.
		 * @return a reference to purge or {@code null}
		 */
		@SuppressWarnings("unchecked")
		public Reference<K, V> pollForPurge() {
			return (Reference<K, V>) this.queue.poll();
		}
	}


	/**
	 * Internal {@link Reference} implementation for {@link SoftReference}s.
	 */
	private static final class SoftEntryReference<K, V> extends SoftReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		private final Reference<K, V> nextReference;

		public SoftEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		public int getHash() {
			return this.hash;
		}

		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		public void release() {
			enqueue();
			clear();
		}
	}


	/**
	 * Internal {@link Reference} implementation for {@link WeakReference}s.
	 */
	private static final class WeakEntryReference<K, V> extends WeakReference<Entry<K, V>> implements Reference<K, V> {

		private final int hash;

		private final Reference<K, V> nextReference;

		public WeakEntryReference(Entry<K, V> entry, int hash, Reference<K, V> next, ReferenceQueue<Entry<K, V>> queue) {
			super(entry, queue);
			this.hash = hash;
			this.nextReference = next;
		}

		public int getHash() {
			return this.hash;
		}

		public Reference<K, V> getNext() {
			return this.nextReference;
		}

		public void release() {
			enqueue();
			clear();
		}
	}

}
