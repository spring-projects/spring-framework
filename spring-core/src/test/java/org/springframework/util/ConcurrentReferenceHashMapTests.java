/*
 * Copyright 2002-2016 the original author or authors.
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

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.lang.Nullable;
import org.springframework.util.ConcurrentReferenceHashMap.Entry;
import org.springframework.util.ConcurrentReferenceHashMap.Reference;
import org.springframework.util.ConcurrentReferenceHashMap.Restructure;
import org.springframework.util.comparator.ComparableComparator;
import org.springframework.util.comparator.NullSafeComparator;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 * Tests for {@link ConcurrentReferenceHashMap}.
 *
 * @author Phillip Webb
 */
public class ConcurrentReferenceHashMapTests {

	private static final Comparator<? super String> NULL_SAFE_STRING_SORT = new NullSafeComparator<String>(
			new ComparableComparator<String>(), true);

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	private TestWeakConcurrentCache<Integer, String> map = new TestWeakConcurrentCache<>();


	@Test
	public void shouldCreateWithDefaults() throws Exception {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
		assertThat(map.getSegmentsSize(), is(16));
		assertThat(map.getSegment(0).getSize(), is(1));
		assertThat(map.getLoadFactor(), is(0.75f));
	}

	@Test
	public void shouldCreateWithInitialCapacity() throws Exception {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32);
		assertThat(map.getSegmentsSize(), is(16));
		assertThat(map.getSegment(0).getSize(), is(2));
		assertThat(map.getLoadFactor(), is(0.75f));
	}

	@Test
	public void shouldCreateWithInitialCapacityAndLoadFactor() throws Exception {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32, 0.5f);
		assertThat(map.getSegmentsSize(), is(16));
		assertThat(map.getSegment(0).getSize(), is(2));
		assertThat(map.getLoadFactor(), is(0.5f));
	}

	@Test
	public void shouldCreateWithInitialCapacityAndConcurrenyLevel() throws Exception {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(16, 2);
		assertThat(map.getSegmentsSize(), is(2));
		assertThat(map.getSegment(0).getSize(), is(8));
		assertThat(map.getLoadFactor(), is(0.75f));
	}

	@Test
	public void shouldCreateFullyCustom() throws Exception {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(5, 0.5f, 3);
		// concurrencyLevel of 3 ends up as 4 (nearest power of 2)
		assertThat(map.getSegmentsSize(), is(4));
		// initialCapacity is 5/4 (rounded up, to nearest power of 2)
		assertThat(map.getSegment(0).getSize(), is(2));
		assertThat(map.getLoadFactor(), is(0.5f));
	}

	@Test
	public void shouldNeedNonNegativeInitialCapacity() throws Exception {
		new ConcurrentReferenceHashMap<Integer, String>(0, 1);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Initial capacity must not be negative");
		new TestWeakConcurrentCache<Integer, String>(-1, 1);
	}

	@Test
	public void shouldNeedPositiveLoadFactor() throws Exception {
		new ConcurrentReferenceHashMap<Integer, String>(0, 0.1f, 1);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Load factor must be positive");
		new TestWeakConcurrentCache<Integer, String>(0, 0.0f, 1);
	}

	@Test
	public void shouldNeedPositiveConcurrencyLevel() throws Exception {
		new ConcurrentReferenceHashMap<Integer, String>(1, 1);
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Concurrency level must be positive");
		new TestWeakConcurrentCache<Integer, String>(1, 0);
	}

	@Test
	public void shouldPutAndGet() throws Exception {
		// NOTE we are using mock references so we don't need to worry about GC
		assertThat(this.map.size(), is(0));
		this.map.put(123, "123");
		assertThat(this.map.get(123), is("123"));
		assertThat(this.map.size(), is(1));
		this.map.put(123, "123b");
		assertThat(this.map.size(), is(1));
		this.map.put(123, null);
		assertThat(this.map.size(), is(1));
	}

	@Test
	public void shouldReplaceOnDoublePut() throws Exception {
		this.map.put(123, "321");
		this.map.put(123, "123");
		assertThat(this.map.get(123), is("123"));
	}

	@Test
	public void shouldPutNullKey() throws Exception {
		this.map.put(null, "123");
		assertThat(this.map.get(null), is("123"));
	}

	@Test
	public void shouldPutNullValue() throws Exception {
		this.map.put(123, "321");
		this.map.put(123, null);
		assertThat(this.map.get(123), is(nullValue()));
	}

	@Test
	public void shouldGetWithNoItems() throws Exception {
		assertThat(this.map.get(123), is(nullValue()));
	}

	@Test
	public void shouldApplySupplimentalHash() throws Exception {
		Integer key = 123;
		this.map.put(key, "123");
		assertThat(this.map.getSupplimentalHash(), is(not(key.hashCode())));
		assertThat(this.map.getSupplimentalHash() >> 30 & 0xFF, is(not(0)));
	}

	@Test
	public void shouldGetFollowingNexts() throws Exception {
		// Use loadFactor to disable resize
		this.map = new TestWeakConcurrentCache<>(1, 10.0f, 1);
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		assertThat(this.map.getSegment(0).getSize(), is(1));
		assertThat(this.map.get(1), is("1"));
		assertThat(this.map.get(2), is("2"));
		assertThat(this.map.get(3), is("3"));
		assertThat(this.map.get(4), is(nullValue()));
	}

	@Test
	public void shouldResize() throws Exception {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		this.map.put(1, "1");
		assertThat(this.map.getSegment(0).getSize(), is(1));
		assertThat(this.map.get(1), is("1"));

		this.map.put(2, "2");
		assertThat(this.map.getSegment(0).getSize(), is(2));
		assertThat(this.map.get(1), is("1"));
		assertThat(this.map.get(2), is("2"));

		this.map.put(3, "3");
		assertThat(this.map.getSegment(0).getSize(), is(4));
		assertThat(this.map.get(1), is("1"));
		assertThat(this.map.get(2), is("2"));
		assertThat(this.map.get(3), is("3"));

		this.map.put(4, "4");
		assertThat(this.map.getSegment(0).getSize(), is(8));
		assertThat(this.map.get(4), is("4"));

		// Putting again should not increase the count
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		assertThat(this.map.getSegment(0).getSize(), is(8));
		assertThat(this.map.get(5), is("5"));
	}

	@Test
	public void shouldPurgeOnGet() throws Exception {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		this.map.getMockReference(1, Restructure.NEVER).queueForPurge();
		this.map.getMockReference(3, Restructure.NEVER).queueForPurge();
		assertThat(this.map.getReference(1, Restructure.WHEN_NECESSARY), is(nullValue()));
		assertThat(this.map.get(2), is("2"));
		assertThat(this.map.getReference(3, Restructure.WHEN_NECESSARY), is(nullValue()));
		assertThat(this.map.get(4), is("4"));
		assertThat(this.map.get(5), is("5"));
	}

	@Test
	public void shouldPergeOnPut() throws Exception {
		this.map = new TestWeakConcurrentCache<>(1, 0.75f, 1);
		for (int i = 1; i <= 5; i++) {
			this.map.put(i, String.valueOf(i));
		}
		this.map.getMockReference(1, Restructure.NEVER).queueForPurge();
		this.map.getMockReference(3, Restructure.NEVER).queueForPurge();
		this.map.put(1, "1");
		assertThat(this.map.get(1), is("1"));
		assertThat(this.map.get(2), is("2"));
		assertThat(this.map.getReference(3, Restructure.WHEN_NECESSARY), is(nullValue()));
		assertThat(this.map.get(4), is("4"));
		assertThat(this.map.get(5), is("5"));
	}

	@Test
	public void shouldPutIfAbsent() throws Exception {
		assertThat(this.map.putIfAbsent(123, "123"), is(nullValue()));
		assertThat(this.map.putIfAbsent(123, "123b"), is("123"));
		assertThat(this.map.get(123), is("123"));
	}

	@Test
	public void shouldPutIfAbsentWithNullValue() throws Exception {
		assertThat(this.map.putIfAbsent(123, null), is(nullValue()));
		assertThat(this.map.putIfAbsent(123, "123"), is(nullValue()));
		assertThat(this.map.get(123), is(nullValue()));
	}

	@Test
	public void shouldPutIfAbsentWithNullKey() throws Exception {
		assertThat(this.map.putIfAbsent(null, "123"), is(nullValue()));
		assertThat(this.map.putIfAbsent(null, "123b"), is("123"));
		assertThat(this.map.get(null), is("123"));
	}

	@Test
	public void shouldRemoveKeyAndValue() throws Exception {
		this.map.put(123, "123");
		assertThat(this.map.remove(123, "456"), is(false));
		assertThat(this.map.get(123), is("123"));
		assertThat(this.map.remove(123, "123"), is(true));
		assertFalse(this.map.containsKey(123));
		assertThat(this.map.isEmpty(), is(true));
	}

	@Test
	public void shouldRemoveKeyAndValueWithExistingNull() throws Exception {
		this.map.put(123, null);
		assertThat(this.map.remove(123, "456"), is(false));
		assertThat(this.map.get(123), is(nullValue()));
		assertThat(this.map.remove(123, null), is(true));
		assertFalse(this.map.containsKey(123));
		assertThat(this.map.isEmpty(), is(true));
	}

	@Test
	public void shouldReplaceOldValueWithNewValue() throws Exception {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456", "789"), is(false));
		assertThat(this.map.get(123), is("123"));
		assertThat(this.map.replace(123, "123", "789"), is(true));
		assertThat(this.map.get(123), is("789"));
	}

	@Test
	public void shouldReplaceOldNullValueWithNewValue() throws Exception {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456", "789"), is(false));
		assertThat(this.map.get(123), is(nullValue()));
		assertThat(this.map.replace(123, null, "789"), is(true));
		assertThat(this.map.get(123), is("789"));
	}

	@Test
	public void shouldReplaceValue() throws Exception {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456"), is("123"));
		assertThat(this.map.get(123), is("456"));
	}

	@Test
	public void shouldReplaceNullValue() throws Exception {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456"), is(nullValue()));
		assertThat(this.map.get(123), is("456"));
	}

	@Test
	public void shouldGetSize() throws Exception {
		assertThat(this.map.size(), is(0));
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map.size(), is(2));
	}

	@Test
	public void shouldSupportIsEmpty() throws Exception {
		assertThat(this.map.isEmpty(), is(true));
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map.isEmpty(), is(false));
	}

	@Test
	public void shouldContainKey() throws Exception {
		assertThat(this.map.containsKey(123), is(false));
		assertThat(this.map.containsKey(456), is(false));
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsKey(123), is(true));
		assertThat(this.map.containsKey(456), is(true));
	}

	@Test
	public void shouldContainValue() throws Exception {
		assertThat(this.map.containsValue("123"), is(false));
		assertThat(this.map.containsValue(null), is(false));
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsValue("123"), is(true));
		assertThat(this.map.containsValue(null), is(true));
	}

	@Test
	public void shouldRemoveWhenKeyIsInMap() throws Exception {
		this.map.put(123, null);
		this.map.put(456, "456");
		this.map.put(null, "789");
		assertThat(this.map.remove(123), is(nullValue()));
		assertThat(this.map.remove(456), is("456"));
		assertThat(this.map.remove(null), is("789"));
		assertThat(this.map.isEmpty(), is(true));
	}

	@Test
	public void shouldRemoveWhenKeyIsNotInMap() throws Exception {
		assertThat(this.map.remove(123), is(nullValue()));
		assertThat(this.map.remove(null), is(nullValue()));
		assertThat(this.map.isEmpty(), is(true));
	}

	@Test
	public void shouldPutAll() throws Exception {
		Map<Integer, String> m = new HashMap<>();
		m.put(123, "123");
		m.put(456, null);
		m.put(null, "789");
		this.map.putAll(m);
		assertThat(this.map.size(), is(3));
		assertThat(this.map.get(123), is("123"));
		assertThat(this.map.get(456), is(nullValue()));
		assertThat(this.map.get(null), is("789"));
	}

	@Test
	public void shouldClear() throws Exception {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		this.map.clear();
		assertThat(this.map.size(), is(0));
		assertThat(this.map.containsKey(123), is(false));
		assertThat(this.map.containsKey(456), is(false));
		assertThat(this.map.containsKey(null), is(false));
	}

	@Test
	public void shouldGetKeySet() throws Exception {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Set<Integer> expected = new HashSet<>();
		expected.add(123);
		expected.add(456);
		expected.add(null);
		assertThat(this.map.keySet(), is(expected));
	}

	@Test
	public void shouldGetValues() throws Exception {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		List<String> actual = new ArrayList<>(this.map.values());
		List<String> expected = new ArrayList<>();
		expected.add("123");
		expected.add(null);
		expected.add("789");
		Collections.sort(actual, NULL_SAFE_STRING_SORT);
		Collections.sort(expected, NULL_SAFE_STRING_SORT);
		assertThat(actual, is(expected));
	}

	@Test
	public void shouldGetEntrySet() throws Exception {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		HashMap<Integer, String> expected = new HashMap<>();
		expected.put(123, "123");
		expected.put(456, null);
		expected.put(null, "789");
		assertThat(this.map.entrySet(), is(expected.entrySet()));
	}

	@Test
	public void shouldGetEntrySetFollowingNext() throws Exception {
		// Use loadFactor to disable resize
		this.map = new TestWeakConcurrentCache<>(1, 10.0f, 1);
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		HashMap<Integer, String> expected = new HashMap<>();
		expected.put(1, "1");
		expected.put(2, "2");
		expected.put(3, "3");
		assertThat(this.map.entrySet(), is(expected.entrySet()));
	}

	@Test
	public void shouldRemoveViaEntrySet() throws Exception {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Iterator<Map.Entry<Integer, String>> iterator = this.map.entrySet().iterator();
		iterator.next();
		iterator.next();
		iterator.remove();
		iterator.next();
		assertThat(iterator.hasNext(), is(false));
		assertThat(this.map.size(), is(2));
		assertThat(this.map.containsKey(2), is(false));
	}

	@Test
	public void shouldSetViaEntrySet() throws Exception {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Iterator<Map.Entry<Integer, String>> iterator = this.map.entrySet().iterator();
		iterator.next();
		iterator.next().setValue("2b");
		iterator.next();
		assertThat(iterator.hasNext(), is(false));
		assertThat(this.map.size(), is(3));
		assertThat(this.map.get(2), is("2b"));
	}

	@Test
	@Ignore("Intended for use during development only")
	public void shouldBeFasterThanSynchronizedMap() throws Exception {
		Map<Integer, WeakReference<String>> synchronizedMap = Collections.synchronizedMap(new WeakHashMap<Integer, WeakReference<String>>());
		StopWatch mapTime = timeMultiThreaded("SynchronizedMap", synchronizedMap,
				new ValueFactory<WeakReference<String>>() {

					@Override
					public WeakReference<String> newValue(int v) {
						return new WeakReference<>(String.valueOf(v));
					}
				});
		System.out.println(mapTime.prettyPrint());

		this.map.setDisableTestHooks(true);
		StopWatch cacheTime = timeMultiThreaded("WeakConcurrentCache", this.map,
				new ValueFactory<String>() {

					@Override
					public String newValue(int v) {
						return String.valueOf(v);
					}
				});
		System.out.println(cacheTime.prettyPrint());

		// We should be at least 4 time faster
		assertThat(cacheTime.getTotalTimeSeconds(),
				is(lessThan(mapTime.getTotalTimeSeconds() / 4.0)));
	}

	@Test
	public void shouldSupportNullReference() throws Exception {
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


	private static interface ValueFactory<V> {

		V newValue(int k);
	}


	private static class TestWeakConcurrentCache<K, V> extends ConcurrentReferenceHashMap<K, V> {

		private int supplimentalHash;

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
			this.supplimentalHash = super.getHash(o);
			return o == null ? 0 : o.hashCode();
		}

		public int getSupplimentalHash() {
			return this.supplimentalHash;
		}

		@Override
		protected ReferenceManager createReferenceManager() {
			return new ReferenceManager() {
				@Override
				public Reference<K, V> createReference(Entry<K, V> entry, int hash,
						@Nullable Reference<K, V> next) {
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
