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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import org.springframework.util.ConcurrentReferenceHashMap.Entry;
import org.springframework.util.ConcurrentReferenceHashMap.Reference;
import org.springframework.util.ConcurrentReferenceHashMap.Restructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link ConcurrentReferenceHashMap}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 */
class ConcurrentReferenceHashMapTests {

	private TestWeakConcurrentCache<Integer, String> map = new TestWeakConcurrentCache<>();


	@Test
	void createWithDefaults() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>();
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(1);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void createWithInitialCapacity() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32);
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void createWithInitialCapacityAndLoadFactor() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(32, 0.5f);
		assertThat(map.getSegmentsSize()).isEqualTo(16);
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.5f);
	}

	@Test
	void createWithInitialCapacityAndConcurrentLevel() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(16, 2);
		assertThat(map.getSegmentsSize()).isEqualTo(2);
		assertThat(map.getSegment(0).getSize()).isEqualTo(8);
		assertThat(map.getLoadFactor()).isEqualTo(0.75f);
	}

	@Test
	void createFullyCustom() {
		ConcurrentReferenceHashMap<Integer, String> map = new ConcurrentReferenceHashMap<>(5, 0.5f, 3);
		// concurrencyLevel of 3 ends up as 4 (nearest power of 2)
		assertThat(map.getSegmentsSize()).isEqualTo(4);
		// initialCapacity is 5/4 (rounded up, to nearest power of 2)
		assertThat(map.getSegment(0).getSize()).isEqualTo(2);
		assertThat(map.getLoadFactor()).isEqualTo(0.5f);
	}

	@Test
	void nonNegativeInitialCapacity() {
		assertThatNoException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(0, 1));
		assertThatIllegalArgumentException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(-1, 1))
				.withMessageContaining("Initial capacity must not be negative");
	}

	@Test
	void positiveLoadFactor() {
		assertThatNoException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(0, 0.1f, 1));
		assertThatIllegalArgumentException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(0, 0.0f, 1))
				.withMessageContaining("Load factor must be positive");
	}

	@Test
	void positiveConcurrencyLevel() {
		assertThatNoException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(1, 1));
		assertThatIllegalArgumentException().isThrownBy(() -> new ConcurrentReferenceHashMap<Integer, String>(1, 0))
				.withMessageContaining("Concurrency level must be positive");
	}

	@Test
	void putAndGet() {
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
	void replaceOnDoublePut() {
		this.map.put(123, "321");
		this.map.put(123, "123");
		assertThat(this.map.get(123)).isEqualTo("123");
	}

	@Test
	void putNullKey() {
		assertThat(this.map.get(null)).isNull();
		assertThat(this.map.getOrDefault(null, "456")).isEqualTo("456");
		this.map.put(null, "123");
		assertThat(this.map.get(null)).isEqualTo("123");
		assertThat(this.map.getOrDefault(null, "456")).isEqualTo("123");
	}

	@Test
	void putNullValue() {
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
	void getWithNoItems() {
		assertThat(this.map.get(123)).isNull();
	}

	@Test
	void applySupplementalHash() {
		Integer key = 123;
		this.map.put(key, "123");
		assertThat(this.map.getSupplementalHash()).isNotEqualTo(key.hashCode());
		assertThat(this.map.getSupplementalHash() >> 30 & 0xFF).isNotEqualTo(0);
	}

	@Test
	void getFollowingNexts() {
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
	void resize() {
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
	void purgeOnGet() {
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
	void purgeOnPut() {
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
	void putIfAbsent() {
		assertThat(this.map.putIfAbsent(123, "123")).isNull();
		assertThat(this.map.putIfAbsent(123, "123b")).isEqualTo("123");
		assertThat(this.map.get(123)).isEqualTo("123");
	}

	@Test
	void putIfAbsentWithNullValue() {
		assertThat(this.map.putIfAbsent(123, null)).isNull();
		assertThat(this.map.putIfAbsent(123, "123")).isNull();
		assertThat(this.map.get(123)).isNull();
	}

	@Test
	void putIfAbsentWithNullKey() {
		assertThat(this.map.putIfAbsent(null, "123")).isNull();
		assertThat(this.map.putIfAbsent(null, "123b")).isEqualTo("123");
		assertThat(this.map.get(null)).isEqualTo("123");
	}

	@Test
	void removeKeyAndValue() {
		this.map.put(123, "123");
		assertThat(this.map.remove(123, "456")).isFalse();
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map.remove(123, "123")).isTrue();
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map).isEmpty();
	}

	@Test
	void removeKeyAndValueWithExistingNull() {
		this.map.put(123, null);
		assertThat(this.map.remove(123, "456")).isFalse();
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.remove(123, null)).isTrue();
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map).isEmpty();
	}

	@Test
	void replaceOldValueWithNewValue() {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456", "789")).isFalse();
		assertThat(this.map.get(123)).isEqualTo("123");
		assertThat(this.map.replace(123, "123", "789")).isTrue();
		assertThat(this.map.get(123)).isEqualTo("789");
	}

	@Test
	void replaceOldNullValueWithNewValue() {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456", "789")).isFalse();
		assertThat(this.map.get(123)).isNull();
		assertThat(this.map.replace(123, null, "789")).isTrue();
		assertThat(this.map.get(123)).isEqualTo("789");
	}

	@Test
	void replaceValue() {
		this.map.put(123, "123");
		assertThat(this.map.replace(123, "456")).isEqualTo("123");
		assertThat(this.map.get(123)).isEqualTo("456");
	}

	@Test
	void replaceNullValue() {
		this.map.put(123, null);
		assertThat(this.map.replace(123, "456")).isNull();
		assertThat(this.map.get(123)).isEqualTo("456");
	}

	@Test
	void computeIfAbsent() {
		assertThat(this.map.computeIfAbsent(123, k -> "123")).isEqualTo("123");
		assertThat(this.map.computeIfAbsent(123, k -> "123b")).isEqualTo("123");
		assertThat(this.map.get(123)).isEqualTo("123");
		this.map.remove(123);
		assertThat(this.map.computeIfAbsent(123, k -> null)).isNull();
		assertThat(this.map.containsKey(123)).isFalse();
	}

	@Test
	void computeIfPresent() {
		assertThat(this.map.computeIfPresent(123, (k, v) -> "123")).isNull();
		this.map.put(123, "123");
		assertThat(this.map.computeIfPresent(123, (k, v) -> v + "b")).isEqualTo("123b");
		assertThat(this.map.get(123)).isEqualTo("123b");
		assertThat(this.map.computeIfPresent(123, (k, v) -> null)).isNull();
		assertThat(this.map.containsKey(123)).isFalse();
	}

	@Test
	void compute() {
		assertThat(this.map.compute(123, (k, v) -> "123" + v)).isEqualTo("123null");
		assertThat(this.map.compute(123, (k, v) -> null)).isNull();
		assertThat(this.map.compute(123, (k, v) -> null)).isNull();
		assertThat(this.map.compute(123, (k, v) -> "123")).isEqualTo("123");
		assertThat(this.map.compute(123, (k, v) -> v + "b")).isEqualTo("123b");
		assertThat(this.map.get(123)).isEqualTo("123b");
	}

	@Test
	void merge() {
		assertThat(this.map.merge(123, "123", (v1, v2) -> v1 + v2)).isEqualTo("123");
		assertThat(this.map.merge(123, null, (v1, v2) -> v1 + v2)).isEqualTo("123null");
		assertThat(this.map.merge(123, null, (v1, v2) -> null)).isNull();
		assertThat(this.map.merge(123, "123", (v1, v2) -> v1 + v2)).isEqualTo("123");
		assertThat(this.map.merge(123, "b", (v1, v2) -> v1 + v2)).isEqualTo("123b");
		assertThat(this.map.get(123)).isEqualTo("123b");
	}

	@Test
	void size() {
		assertThat(this.map).isEmpty();
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map).hasSize(2);
	}

	@Test
	void isEmpty() {
		assertThat(this.map).isEmpty();
		this.map.put(123, "123");
		this.map.put(123, null);
		this.map.put(456, "456");
		assertThat(this.map).isNotEmpty();
	}

	@Test
	void containsKey() {
		assertThat(this.map.containsKey(123)).isFalse();
		assertThat(this.map.containsKey(456)).isFalse();
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsKey(123)).isTrue();
		assertThat(this.map.containsKey(456)).isTrue();
	}

	@Test
	void containsValue() {
		assertThat(this.map.containsValue("123")).isFalse();
		assertThat(this.map.containsValue(null)).isFalse();
		this.map.put(123, "123");
		this.map.put(456, null);
		assertThat(this.map.containsValue("123")).isTrue();
		assertThat(this.map.containsValue(null)).isTrue();
	}

	@Test
	void removeWhenKeyIsInMap() {
		this.map.put(123, null);
		this.map.put(456, "456");
		this.map.put(null, "789");
		assertThat(this.map.remove(123)).isNull();
		assertThat(this.map.remove(456)).isEqualTo("456");
		assertThat(this.map.remove(null)).isEqualTo("789");
		assertThat(this.map).isEmpty();
	}

	@Test
	void removeWhenKeyIsNotInMap() {
		assertThat(this.map.remove(123)).isNull();
		assertThat(this.map.remove(null)).isNull();
		assertThat(this.map).isEmpty();
	}

	@Test
	void putAll() {
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
	void clear() {
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
	void keySet() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Set<Integer> expected = new HashSet<>();
		expected.add(123);
		expected.add(456);
		expected.add(null);
		assertThat(this.map.keySet()).isEqualTo(expected);
	}

	@Test  // gh-35817
	void keySetContains() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		assertThat(this.map.keySet()).containsExactlyInAnyOrder(123, 456, null);
	}

	@Test  // gh-35817
	void keySetRemove() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		assertThat(this.map.keySet().remove(123)).isTrue();
		assertThat(this.map).doesNotContainKey(123);
		assertThat(this.map.keySet().remove(123)).isFalse();
	}

	@Test  // gh-35817
	void keySetIterator() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Iterator<Integer> it = this.map.keySet().iterator();
		assertThat(it).toIterable().containsExactlyInAnyOrder(123, 456, null);
		assertThat(it).isExhausted();
	}

	@Test  // gh-35817
	void keySetIteratorRemove() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Iterator<Integer> keySetIterator = this.map.keySet().iterator();
		while (keySetIterator.hasNext()) {
			Integer key = keySetIterator.next();
			if (key != null && key.equals(456)) {
				keySetIterator.remove();
			}
		}
		assertThat(this.map).containsOnlyKeys(123, null);
	}

	@Test  // gh-35817
	void keySetClear() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		this.map.keySet().clear();
		assertThat(this.map).isEmpty();
		assertThat(this.map.keySet()).isEmpty();
	}

	@Test  // gh-35817
	void keySetAdd() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> this.map.keySet().add(12345));
	}

	@Test  // gh-35817
	void keySetStream() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Set<Integer> keys = this.map.keySet().stream().collect(Collectors.toSet());
		assertThat(keys).containsExactlyInAnyOrder(123, 456, null);
	}

	@Test  // gh-35817
	void keySetSpliteratorCharacteristics() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Spliterator<Integer> spliterator = this.map.keySet().spliterator();
		assertThat(spliterator).hasOnlyCharacteristics(Spliterator.CONCURRENT, Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(3L);
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(-1L);
	}

	@Test
	void valuesCollection() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		assertThat(this.map.values()).containsExactlyInAnyOrder("123", null, "789");
	}

	@Test  // gh-35817
	void valuesCollectionAdd() {
		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> this.map.values().add("12345"));
	}

	@Test  // gh-35817
	void valuesCollectionClear() {
		Collection<String> values = this.map.values();
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		assertThat(values).hasSize(3);
		values.clear();
		assertThat(values).isEmpty();
		assertThat(this.map).isEmpty();
	}

	@Test  // gh-35817
	void valuesCollectionRemoval() {
		Collection<String> values = this.map.values();
		assertThat(values).isEmpty();
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		assertThat(values).containsExactlyInAnyOrder("123", null, "789");
		values.remove(null);
		assertThat(values).containsExactlyInAnyOrder("123", "789");
		assertThat(map).containsOnly(entry(123, "123"), entry(null, "789"));
		values.remove("123");
		values.remove("789");
		assertThat(values).isEmpty();
		assertThat(map).isEmpty();
	}

	@Test  // gh-35817
	void valuesCollectionIterator() {
		Iterator<String> iterator = this.map.values().iterator();
		assertThat(iterator).isExhausted();
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		iterator = this.map.values().iterator();
		assertThat(iterator).toIterable().containsExactlyInAnyOrder("123", null, "789");
	}

	@Test  // gh-35817
	void valuesCollectionIteratorRemoval() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Iterator<String> iterator = this.map.values().iterator();
		while (iterator.hasNext()) {
			String value = iterator.next();
			if (value != null && value.equals("789")) {
				iterator.remove();
			}
		}
		assertThat(iterator).isExhausted();
		assertThat(this.map.values()).containsExactlyInAnyOrder("123", null);
		assertThat(this.map).containsOnlyKeys(123, 456);
	}

	@Test  // gh-35817
	void valuesCollectionStream() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		List<String> values = this.map.values().stream().toList();
		assertThat(values).containsExactlyInAnyOrder("123", null, "789");
	}

	@Test  // gh-35817
	void valuesCollectionSpliteratorCharacteristics() {
		this.map.put(123, "123");
		this.map.put(456, null);
		this.map.put(null, "789");
		Spliterator<String> spliterator = this.map.values().spliterator();
		assertThat(spliterator).hasOnlyCharacteristics(Spliterator.CONCURRENT);
		assertThat(spliterator.estimateSize()).isEqualTo(3L);
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(-1L);
	}

	@Test
	void getEntrySet() {
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
	void getEntrySetFollowingNext() {
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
	void removeViaEntrySet() {
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
	void setViaEntrySet() {
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
		copy.forEach(entry -> assertThat(entrySet).contains(entry));
		this.map.put(1, "A");
		this.map.put(2, "B");
		this.map.put(3, "C");
		copy.forEach(entry -> assertThat(entrySet).doesNotContain(entry));
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		copy.forEach(entry -> assertThat(entrySet).contains(entry));
		entrySet.clear();
		copy.forEach(entry -> assertThat(entrySet).doesNotContain(entry));
	}

	@Test  // gh-35817
	void entrySetSpliteratorCharacteristics() {
		this.map.put(1, "1");
		this.map.put(2, "2");
		this.map.put(3, "3");
		Spliterator<Map.Entry<Integer, String>> spliterator = this.map.entrySet().spliterator();
		assertThat(spliterator).hasOnlyCharacteristics(Spliterator.CONCURRENT, Spliterator.DISTINCT);
		assertThat(spliterator.estimateSize()).isEqualTo(3L);
		assertThat(spliterator.getExactSizeIfKnown()).isEqualTo(-1L);
	}

	@Test
	void supportNullReference() {
		// GC could happen during restructure so we must be able to create a reference for a null entry
		map.createReferenceManager().createReference(null, 1234, null);
	}


	private static class TestWeakConcurrentCache<K, V> extends ConcurrentReferenceHashMap<K, V> {

		private int supplementalHash;

		private final LinkedList<MockReference<K, V>> queue = new LinkedList<>();

		public TestWeakConcurrentCache() {
			super();
		}

		public TestWeakConcurrentCache(int initialCapacity, float loadFactor, int concurrencyLevel) {
			super(initialCapacity, loadFactor, concurrencyLevel);
		}

		@Override
		protected int getHash(@Nullable Object o) {
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
					return new MockReference<>(entry, hash, next, TestWeakConcurrentCache.this.queue);
				}
				@Override
				public Reference<K, V> pollForPurge() {
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
