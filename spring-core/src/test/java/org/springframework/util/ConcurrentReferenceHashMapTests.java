/*
 * Copyright 2002-2022 the original author or authors.
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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap.Entry;
import org.springframework.util.ConcurrentReferenceHashMap.Reference;
import org.springframework.util.ConcurrentReferenceHashMap.Restructure;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link ConcurrentReferenceHashMap}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ConcurrentReferenceHashMapTests {

	private static final Comparator<? super String> NULL_SAFE_STRING_SORT = new NullSafeComparator<>(
			new ComparableComparator<String>(), true);

	private TestWeakConcurrentCache<Integer, String> map = new TestWeakConcurrentCache<>();


	@Test
	void shouldCreateWithDefaults() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(1);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void shouldCreateWithInitialCapacity() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32);
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void shouldCreateWithInitialCapacityAndLoadFactor() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32, 0.5f);
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.5f);
	}

	@Test
	void shouldCreateWithInitialCapacityAndConcurrentLevel() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(16, 2);
		assertThat(map.getSegmentsSize()).isEqualTo(2);
		assertThat(map.getSegment(0).getSize()).isEqualTo(8);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void shouldCreateFullyCustom() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(5, 0.5f, 3);
		// concurrencyLevel of 3 ends up as 4 (nearest power of 2)
		assertThat(map.getSegmentsSize()).isEqualTo(4);
		// initialCapacity is 5/4 (rounded up, to nearest power of 2)
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.5f);
	}

	@Test
	void shouldNeedNonNegativeInitialCapacity() {
		new ConcurrentReferenceHashMap<Integer, String>(0, 1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				new TestWeakConcurrentCache<Integer, String>(-1, 1))
			.withMessageContaining("Initial capacity must not be negative");
	}

	@Test
	void shouldNeedPositiveLoadFactor() {
		new ConcurrentReferenceHashMap<Integer, String>(0, 0.1f, 1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				new TestWeakConcurrentCache<Integer, String>(0, 0.0f, 1))
			.withMessageContaining("Load factor must be positive");
	}

	@Test
	void shouldNeedPositiveConcurrencyLevel() {
		new ConcurrentReferenceHashMap<Integer, String>(1, 1);
		assertThatIllegalArgumentException().isThrownBy(() ->
				new TestWeakConcurrentCache<Integer, String>(1, 0))
			.withMessageContaining("Concurrency level must be positive");
	}

	@Test
	void shouldPutAndGet() {
		// NOTE we are using mock references so we don't need to worry about GC
		assertThat(this.map).isEmpty();
		this.map.put(123, "123");
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map).hasSize(1);
		this.map.put(123, "123b");
		assertThat(this.map).hasSize(1);
		this.map.put(123, null);
		assertThat(this.map).hasSize(1);
	}

	@Test
	void shouldReplaceOnDoublePut() {
		this.map.put(123, "321");
		this.map.put(123, "123");
		assertThat(this.map.get(123)).isEqualTo("123");
	}

	@Test
	void shouldPutNullKey() {
		assertThat(this.map.get(null)).isNull();
		assertThat(this.map.getOrDefault(null, "456")).isEqualTo("456");
		this.map.put(null, "123");
		assertThat(this.map.get(null)).isEqualTo("123");
		assertThat(this.map.getOrDefault(null, "456")).isEqualTo("123");
	}

	@Test
	void shouldPutNullValue() {
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.getOrDefault(123, "456")).isEqualTo("456");
		this.map.put(123, "321");
		assertThat(this.map.get(123)).isEqualTo("321");
		assertThat(this.map.getOrDefault(123, "456")).isEqualTo("321");
		this.map.put(123, null);
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.getOrDefault(123, "456")).isNull();
	}

	@Test
	void shouldGetWithNoItems() {
		assertThat(this.map.get(123)).isNull();
	}

	@Test
	void shouldApplySupplementalHash() {
		Integer key = 123;
		this.map.put(key, "123");
		assertThat(this.map.getSupplementalHash()).isNotEqualTo(key.hashCode());
		assertThat(this.map.getSupplementalHash() >> 30 & 0xFF).isNotEqualTo(0);
	}

	@Test
	void shouldGetFollowingNexts() {
		// Use loadFactor to disable resize
		this.map = new TestWeakConcurrentCache<>(1, 10.0f, 1);
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(1);
		assertThat(this.map.get(1)).isEqualTo("1");
		assertThat(this.map.get(2)).isEqualTo("2");
		assertThat(this.map.get(3)).isEqualTo("3");
		assertThat(this.map.get(4)).isNull();
	}

	@Test
	void shouldResize() {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		this.map.put(1, "1");
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(1);
		assertThat(this.map.get(1)).isEqualTo("1");

		this.map.put(2, "2");
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(this.map.get(1)).isEqualTo("1");
		assertThat(this.map.get(2)).isEqualTo("2");

		this.map.put(3, "3");
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(4);
		assertThat(this.map.get(1)).isEqualTo("1");
		assertThat(this.map.get(2)).isEqualTo("2");
		assertThat(this.map.get(3)).isEqualTo("3");

		this.map.put(4, "4");
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(8);
		assertThat(this.map.get(4)).isEqualTo("4");

		// Putting again should not increase the count
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		assertThat(this.map.getSegment(0).getSize()).isEqualTo(8);
		assertThat(this.map.get(5)).isEqualTo("5");
	}

	@Test
	void shouldPurgeOnGet() {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		this.map.getMockReference(1, Restructure.NEVER).queueForPurge();
		this.map.getMockReference(3, Restructure.NEVER).queueForPurge();
		assertThat(this.map.getReference(1, Restructure.WHEN_NECESSARY)).isNull();
		assertThat(this.map.get(2)).isEqualTo("2");
		assertThat(this.map.getReference(3, Restructure.WHEN_NECESSARY)).isNull();
		assertThat(this.map.get(4)).isEqualTo("4");
		assertThat(this.map.get(5)).isEqualTo("5");
	}

	@Test
	void shouldPurgeOnPut() {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		this.map.getMockReference(1, Restructure.NEVER).queueForPurge();
		this.map.getMockReference(3, Restructure.NEVER).queueForPurge();
		this.map.put(1, "1");
		assertThat(this.map.get(1)).isEqualTo("1");
		assertThat(this.map.get(2)).isEqualTo("2");
		assertThat(this.map.getReference(3, Restructure.WHEN_NECESSARY)).isNull();
		assertThat(this.map.get(4)).isEqualTo("4");
		assertThat(this.map.get(5)).isEqualTo("5");
	}

	@Test
	void shouldPutIfAbsent() {
		assertThat(this.map.putIfAbsent(123, "123")).isNull();
		assertThat(this.map.putIfAbsent(123, "123b")).isEqualTo("123");
		assertThat(this.map.get(123)).isEqualTo("123");
	}

	@Test
	void shouldPutIfAbsentWithNullValue() {
		assertThat(this.map.putIfAbsent(123, null)).isNull();
		assertThat(this.map.putIfAbsent(123, "123")).isNull();
		assertThat(this.map.get(123)).isNull();
	}

	@Test
	void shouldPutIfAbsentWithNullKey() {
		assertThat(this.map.putIfAbsent(null, "123")).isNull();
		assertThat(this.map.putIfAbsent(null, "123b")).isEqualTo("123");
		assertThat(this.map.get(null)).isEqualTo("123");
	}

	@Test
	void shouldRemoveKeyAndValue() {
		this.map.put(123, "123");
		assertThat(this.map.remove(123, "456")).isFalse();
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map.remove(123, "123")).isTrue();
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map.isEmpty()).isTrue();
	}

	@Test
	void shouldRemoveKeyAndValueWithExistingNull() {
		this.map.put(123, null);
		assertThat(this.map.remove(123, "456")).isFalse();
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.remove(123, null)).isTrue();
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map.isEmpty()).isTrue();
	}

	@Test
	void shouldReplaceOldValueWithNewValue() {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456", "789")).isFalse();
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map.replace(123, "123", "789")).isTrue();
		assertThat(this.map.get(123)).isEqualTo("789");
	}

	@Test
	void shouldReplaceOldNullValueWithNewValue() {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456", "789")).isFalse();
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.replace(123, null, "789")).isTrue();
		assertThat(this.map.get(123)).isEqualTo("789");
	}

	@Test
	void shouldReplaceValue() {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456")).isEqualTo("123");
		assertThat(this.map.get(123)).isEqualTo("456");
	}

	@Test
	void shouldReplaceNullValue() {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456")).isNull();
		assertThat(this.map.get(123)).isEqualTo("456");
	}

	@Test
	void shouldGetSize() {
		assertThat(this.map).isEmpty();
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map).hasSize(2);
	}

	@Test
	void shouldSupportIsEmpty() {
		assertThat(this.map.isEmpty()).isTrue();
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map.isEmpty()).isFalse();
	}

	@Test
	void shouldContainKey() {
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map.containsKey(456)).isFalse();
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsKey(123)).isTrue();
		assertThat(this.map.containsKey(456)).isTrue();
	}

	@Test
	void shouldContainValue() {
		assertThat(this.map.containsValue("123")).isFalse();
		assertThat(this.map.containsValue(null)).isFalse();
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsValue("123")).isTrue();
		assertThat(this.map.containsValue(null)).isTrue();
	}

	@Test
	void shouldRemoveWhenKeyIsInMap() {
		this.map.put(123, null);
		this.map.put(456, "456");
		this.map.put(null, "789");
		assertThat(this.map.remove(123)).isNull();
		assertThat(this.map.remove(456)).isEqualTo("456");
		assertThat(this.map.remove(null)).isEqualTo("789");
		assertThat(this.map.isEmpty()).isTrue();
	}

	@Test
	void shouldRemoveWhenKeyIsNotInMap() {
		assertThat(this.map.remove(123)).isNull();
		assertThat(this.map.remove(null)).isNull();
		assertThat(this.map.isEmpty()).isTrue();
	}

	@Test
	void shouldPutAll() {
		Map<Integer, String> m = new HashMap<>();
		m.put(123, "123");
		m.put(456, null);
		m.put(null, "789");
		this.map.putAll(m);
		assertThat(this.map).hasSize(3);
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map.get(456)).isNull();
		assertThat(this.map.get(null)).isEqualTo("789");
	}

	@Test
	void shouldClear() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		this.map.clear();
		assertThat(this.map).isEmpty();
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map.containsKey(456)).isFalse();
		assertThat(this.map.containsKey(null)).isFalse();
	}

	@Test
	void shouldGetKeySet() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Set<Integer> expected = new HashSet<>();
		expected.add(123);
		expected.add(456);
		expected.add(null);
		assertThat(this.map.keySet()).isEqualTo(expected);
	}

	@Test
	void shouldGetValues() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		List<String> actual = new ArrayList<>(this.map.values());
		List<String> expected = new ArrayList<>();
		expected.add("123");
		expected.add(null);
		expected.add("789");
		actual.sort(NULL_SAFE_STRING_SORT);
		expected.sort(NULL_SAFE_STRING_SORT);
		assertThat(actual).isEqualTo(expected);
	}

	@Test
	void shouldGetEntrySet() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		HashMap<Integer, String> expected = new HashMap<>();
		expected.put(123, "123");
		expected.put(456, null);
		expected.put(null, "789");
		assertThat(this.map.entrySet()).isEqualTo(expected.entrySet());
	}

	@Test
	void shouldGetEntrySetFollowingNext() {
		// Use loadFactor to disable resize
		this.map = new TestWeakConcurrentCache<>(1, 10.0f, 1);
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		HashMap<Integer, String> expected = new HashMap<>();
		expected.put(1, "1");
		expected.put(2, "2");
		expected.put(3, "3");
		assertThat(this.map.entrySet()).isEqualTo(expected.entrySet());
	}

	@Test
	void shouldRemoveViaEntrySet() {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Iterator<Map.Entry<Integer, String>> iterator = this.map.entrySet().iterator();
		iterator.next();
		iterator.next();
		iterator.remove();
		assertThatIllegalStateException().isThrownBy(iterator::remove);
		iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(this.map).hasSize(2);
		assertThat(this.map.containsKey(2)).isFalse();
	}

	@Test
	void shouldSetViaEntrySet() {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Iterator<Map.Entry<Integer, String>> iterator = this.map.entrySet().iterator();
		iterator.next();
		iterator.next().setValue("2b");
		iterator.next();
		assertThat(iterator.hasNext()).isFalse();
		assertThat(this.map).hasSize(3);
		assertThat(this.map.get(2)).isEqualTo("2b");
	}

	@Test
	void containsViaEntrySet() {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Set<Map.Entry<Integer, String>> entrySet = this.map.entrySet();
		Set<Map.Entry<Integer, String>> copy = new HashMap<>(this.map).entrySet();
		copy.forEach(entry -> assertThat(entrySet.contains(entry)).isTrue());
		this.map.put(1, "A");
		this.map.put(2, "B");
		this.map.put(3, "C");
		copy.forEach(entry -> assertThat(entrySet.contains(entry)).isFalse());
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		copy.forEach(entry -> assertThat(entrySet.contains(entry)).isTrue());
		entrySet.clear();
		copy.forEach(entry -> assertThat(entrySet.contains(entry)).isFalse());
	}

	@Test
	@Disabled("Intended for use during development only")
	void shouldBeFasterThanSynchronizedMap() throws InterruptedException {
		Map<Integer, WeakReference<String>> synchronizedMap = Collections.synchronizedMap(new WeakHashMap<Integer, WeakReference<String>>());
		StopWatch mapTime = timeMultiThreaded("SynchronizedMap", synchronizedMap, v -> new WeakReference<>(String.valueOf(v)));
		System.out.println(mapTime.prettyPrint());

		this.map.setDisableTestHooks(true);
		StopWatch cacheTime = timeMultiThreaded("WeakConcurrentCache", this.map, String::valueOf);
		System.out.println(cacheTime.prettyPrint());

		// We should be at least 4 time faster
		assertThat(cacheTime.getTotalTimeSeconds()).isLessThan(mapTime.getTotalTimeSeconds() / 4.0);
	}

	@Test
	void shouldSupportNullReference() {
		// GC could happen during restructure so we must be able to create a reference for a null entry
		map.createReferenceManager().createReference(null, 1234, null);
	}

	/**
	 * Time a multi-threaded access to a cache.
	 * @return the timing stopwatch
	 */
	private <V> StopWatch timeMultiThreaded(String id, final Map<Integer, V> map,
			ValueFactory<V> factory) throws InterruptedException {

		StopWatch stopWatch = new StopWatch(id);
		for (int i = 0; i < 500; i++) {
			map.put(i, factory.newValue(i));
		}
		Thread[] threads = new Thread[30];
		stopWatch.start("Running threads");
		for (int threadIndex = 0; threadIndex < threads.length; threadIndex++) {
			threads[threadIndex] = new Thread("Cache access thread " + threadIndex) {
				@Override
				public void run() {
					for (int j = 0; j < 1000; j++) {
						for (int i = 0; i < 1000; i++) {
							map.get(i);
						}
					}
				}
			};
		}
		for (Thread thread : threads) {
			thread.start();
		}

		for (Thread thread : threads) {
			if (thread.isAlive()) {
				thread.join(2000);
			}
		}
		stopWatch.stop();
		return stopWatch;
	}


	private interface ValueFactory<V> {

		V newValue(int k);
	}


	private static class TestWeakConcurrentCache<K, V> extends ConcurrentReferenceHashMap<K, V> {

		private int supplementalHash;

		private final LinkedList<MockReference<K, V>> queue = new LinkedList<>();

		private boolean disableTestHooks;

		public TestWeakConcurrentCache() {
			super();
		}

		public void setDisableTestHooks(boolean disableTestHooks) {
			this.disableTestHooks = disableTestHooks;
		}

		public TestWeakConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
			super(initialCapacity, loadFactor, concurrencyLevel);
		}

		public TestWeakConcurrentCache(int initialCapacity, int concurrencyLevel) {
			super(initialCapacity, concurrencyLevel);
		}

		@Override
		protected int getHash(@Nullable Object o) {
			if (this.disableTestHooks) {
				return super.getHash(o);
			}
			// For testing we want more control of the hash
			this.supplementalHash = super.getHash(o);
			return (o != null ? o.hashCode() : 0);
		}

		public int getSupplementalHash() {
			return this.supplementalHash;
		}

		@Override
		protected ReferenceManager createReferenceManager() {
			return new ReferenceManager() {
				@Override
				public Reference<K, V> createReference(Entry<K, V> entry, int hash, @Nullable Reference<K, V> next) {
					if (TestWeakConcurrentCache.this.disableTestHooks) {
						return super.createReference(entry, hash, next);
					}
					return new MockReference<>(entry, hash, next, TestWeakConcurrentCache.this.queue);
				}
				@Override
				public Reference<K, V> pollForPurge() {
					if (TestWeakConcurrentCache.this.disableTestHooks) {
						return super.pollForPurge();
					}
					return TestWeakConcurrentCache.this.queue.isEmpty() ? null : TestWeakConcurrentCache.this.queue.removeFirst();
				}
			};
		}

		public MockReference<K, V> getMockReference(K key, Restructure restructure) {
			return (MockReference<K, V>) super.getReference(key, restructure);
		}
	}


	private static class MockReference<K, V> implements Reference<K, V> {

		private final int hash;

		private Entry<K, V> entry;

		private final Reference<K, V> next;

		private final LinkedList<MockReference<K, V>> queue;

		public MockReference(Entry<K, V> entry, int hash, Reference<K, V> next, LinkedList<MockReference<K, V>> queue) {
			this.hash = hash;
			this.entry = entry;
			this.next = next;
			this.queue = queue;
		}

		@Override
		public Entry<K, V> get() {
			return this.entry;
		}

		@Override
		public int getHash() {
			return this.hash;
		}

		@Override
		public Reference<K, V> getNext() {
			return this.next;
		}

		@Override
		public void release() {
			this.queue.add(this);
			this.entry = null;
		}

		public void queueForPurge() {
			this.queue.add(this);
		}
	}

}
