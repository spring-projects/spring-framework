/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.jspecify.annotations.Nullable;

import org.springframework.lang.Contract;

/**
 * Miscellaneous collection utility methods.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @since 1.1.3
 */
public abstract class CollectionUtils {

	/**
	 * Default load factor for {@link HashMap}/{@link LinkedHashMap} variants.
	 * @see #newHashMap(int)
	 * @see #newLinkedHashMap(int)
	 */
	static final float DEFAULT_LOAD_FACTOR = 0.75f;


	/**
	 * Return {@code true} if the supplied Collection is {@code null} or empty.
	 * Otherwise, return {@code false}.
	 * @param collection the Collection to check
	 * @return whether the given Collection is empty
	 */
	@Contract("null -> true")
	public static boolean isEmpty(@Nullable Collection<? extends @Nullable Object> collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return {@code true} if the supplied Map is {@code null} or empty.
	 * Otherwise, return {@code false}.
	 * @param map the Map to check
	 * @return whether the given Map is empty
	 */
	@Contract("null -> true")
	public static boolean isEmpty(@Nullable Map<?, ? extends @Nullable Object> map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * Instantiate a new {@link HashMap} with an initial capacity
	 * that can accommodate the specified number of elements without
	 * any immediate resize/rehash operations to be expected.
	 * <p>This differs from the regular {@link HashMap} constructor
	 * which takes an initial capacity relative to a load factor
	 * but is effectively aligned with the JDK's
	 * {@link java.util.concurrent.ConcurrentHashMap#ConcurrentHashMap(int)}.
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @since 5.3
	 * @see #newLinkedHashMap(int)
	 */
	public static <K, V> HashMap<K, V> newHashMap(int expectedSize) {
		return new HashMap<>(computeInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Instantiate a new {@link LinkedHashMap} with an initial capacity
	 * that can accommodate the specified number of elements without
	 * any immediate resize/rehash operations to be expected.
	 * <p>This differs from the regular {@link LinkedHashMap} constructor
	 * which takes an initial capacity relative to a load factor but is
	 * aligned with Spring's own {@link LinkedCaseInsensitiveMap} and
	 * {@link LinkedMultiValueMap} constructor semantics.
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @since 5.3
	 * @see #newHashMap(int)
	 */
	public static <K, V> LinkedHashMap<K, V> newLinkedHashMap(int expectedSize) {
		return new LinkedHashMap<>(computeInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Instantiate a new {@link HashSet} with an initial capacity that can
	 * accommodate the specified number of elements without any immediate
	 * resize/rehash operations to be expected.
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @since 6.2
	 * @see #newLinkedHashSet(int)
	 */
	public static <E> HashSet<E> newHashSet(int expectedSize) {
		return new HashSet<>(computeInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Instantiate a new {@link LinkedHashSet} with an initial capacity that can
	 * accommodate the specified number of elements without any immediate
	 * resize/rehash operations to be expected.
	 * @param expectedSize the expected number of elements (with a corresponding
	 * capacity to be derived so that no resize/rehash operations are needed)
	 * @since 6.2
	 * @see #newHashSet(int)
	 */
	public static <E> LinkedHashSet<E> newLinkedHashSet(int expectedSize) {
		return new LinkedHashSet<>(computeInitialCapacity(expectedSize), DEFAULT_LOAD_FACTOR);
	}

	private static int computeInitialCapacity(int expectedSize) {
		return (int) Math.ceil(expectedSize / (double) DEFAULT_LOAD_FACTOR);
	}

	/**
	 * Convert the supplied array into a List. A primitive array gets converted
	 * into a List of the appropriate wrapper type.
	 * <p><b>NOTE:</b> Generally prefer the standard {@link Arrays#asList} method.
	 * This {@code arrayToList} method is just meant to deal with an incoming Object
	 * value that might be an {@code Object[]} or a primitive array at runtime.
	 * <p>A {@code null} source value will be converted to an empty List.
	 * @param source the (potentially primitive) array
	 * @return the converted List result
	 * @see ObjectUtils#toObjectArray(Object)
	 * @see Arrays#asList(Object[])
	 */
	public static List<?> arrayToList(@Nullable Object source) {
		return Arrays.asList(ObjectUtils.toObjectArray(source));
	}

	/**
	 * Merge the given array into the given Collection.
	 * @param array the array to merge (may be {@code null})
	 * @param collection the target Collection to merge the array into
	 */
	@SuppressWarnings("unchecked")
	public static <E> void mergeArrayIntoCollection(@Nullable Object array, Collection<E> collection) {
		Object[] arr = ObjectUtils.toObjectArray(array);
		Collections.addAll(collection, (E[])arr);
	}

	/**
	 * Merge the given Properties instance into the given Map,
	 * copying all properties (key-value pairs) over.
	 * <p>Uses {@code Properties.propertyNames()} to even catch
	 * default properties linked into the original Properties instance.
	 * @param props the Properties instance to merge (may be {@code null})
	 * @param map the target Map to merge the properties into
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> void mergePropertiesIntoMap(@Nullable Properties props, Map<K, V> map) {
		if (props != null) {
			for (Enumeration<?> en = props.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				Object value = props.get(key);
				if (value == null) {
					// Allow for defaults fallback or potentially overridden accessor...
					value = props.getProperty(key);
				}
				map.put((K) key, (V) value);
			}
		}
	}


	/**
	 * Check whether the given {@link Iterator} contains the given element.
	 * @param iterator the {@code Iterator} to check
	 * @param element the element to look for
	 * @return {@code true} if found, {@code false} otherwise
	 */
	@Contract("null, _ -> false")
	public static boolean contains(@Nullable Iterator<? extends @Nullable Object> iterator,
			@Nullable Object element) {

		if (iterator != null) {
			while (iterator.hasNext()) {
				Object candidate = iterator.next();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given {@link Enumeration} contains the given element.
	 * @param enumeration the {@code Enumeration} to check
	 * @param element the element to look for
	 * @return {@code true} if found, {@code false} otherwise
	 */
	@Contract("null, _ -> false")
	public static boolean contains(@Nullable Enumeration<? extends @Nullable Object> enumeration,
			@Nullable Object element) {

		if (enumeration != null) {
			while (enumeration.hasMoreElements()) {
				Object candidate = enumeration.nextElement();
				if (ObjectUtils.nullSafeEquals(candidate, element)) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check whether the given {@link Collection} contains the given element instance.
	 * <p>Enforces the given instance to be present, rather than returning
	 * {@code true} for an equal element as well.
	 * @param collection the {@code Collection} to check
	 * @param element the element to look for
	 * @return {@code true} if found, {@code false} otherwise
	 */
	@Contract("null, _ -> false")
	public static boolean containsInstance(@Nullable Collection<? extends @Nullable Object> collection,
			@Nullable Object element) {

		if (collection != null) {
			for (Object candidate : collection) {
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return {@code true} if any element in '{@code candidates}' is
	 * contained in '{@code source}'; otherwise returns {@code false}.
	 * @param source the source {@link Collection}
	 * @param candidates the candidates to search for
	 * @return whether any of the candidates has been found
	 */
	public static boolean containsAny(Collection<? extends @Nullable Object> source,
			Collection<? extends @Nullable Object> candidates) {

		if (isEmpty(source) || isEmpty(candidates)) {
			return false;
		}
		for (Object candidate : candidates) {
			if (source.contains(candidate)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the first element in '{@code candidates}' that is contained in
	 * '{@code source}'. If no element in '{@code candidates}' is present in
	 * '{@code source}' returns {@code null}. Iteration order is
	 * {@link Collection} implementation specific.
	 * @param source the source Collection
	 * @param candidates the candidates to search for
	 * @return the first present object, or {@code null} if not found
	 */
	public static <E> @Nullable E findFirstMatch(Collection<?> source, Collection<E> candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (E candidate : candidates) {
			if (source.contains(candidate)) {
				return candidate;
			}
		}
		return null;
	}

	/**
	 * Find a single value of the given type in the given Collection.
	 * @param collection the Collection to search
	 * @param type the type to look for
	 * @return a value of the given type found if there is a clear match,
	 * or {@code null} if none or more than one such value found
	 */
	@SuppressWarnings("unchecked")
	@Contract("null, _ -> null")
	public static <T> @Nullable T findValueOfType(@Nullable Collection<?> collection, @Nullable Class<T> type) {
		if (isEmpty(collection)) {
			return null;
		}
		T value = null;
		for (Object element : collection) {
			if (type == null || type.isInstance(element)) {
				if (value != null) {
					// More than one value found... no clear single value.
					return null;
				}
				value = (T) element;
			}
		}
		return value;
	}

	/**
	 * Find a single value of one of the given types in the given Collection:
	 * searching the Collection for a value of the first type, then
	 * searching for a value of the second type, etc.
	 * @param collection the collection to search
	 * @param types the types to look for, in prioritized order
	 * @return a value of one of the given types found if there is a clear match,
	 * or {@code null} if none or more than one such value found
	 */
	public static @Nullable Object findValueOfType(Collection<?> collection, Class<?>[] types) {
		if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
			return null;
		}
		for (Class<?> type : types) {
			Object value = findValueOfType(collection, type);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Determine whether the given Collection only contains a single unique object.
	 * @param collection the Collection to check
	 * @return {@code true} if the collection contains a single reference or
	 * multiple references to the same instance, {@code false} otherwise
	 */
	public static boolean hasUniqueObject(Collection<?> collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Object elem : collection) {
			if (!hasCandidate) {
				hasCandidate = true;
				candidate = elem;
			}
			else if (candidate != elem) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Find the common element type of the given Collection, if any.
	 * @param collection the Collection to check
	 * @return the common element type, or {@code null} if no clear
	 * common type has been found (or the collection was empty)
	 */
	public static @Nullable Class<?> findCommonElementType(Collection<?> collection) {
		if (isEmpty(collection)) {
			return null;
		}
		Class<?> candidate = null;
		for (Object val : collection) {
			if (val != null) {
				if (candidate == null) {
					candidate = val.getClass();
				}
				else if (candidate != val.getClass()) {
					return null;
				}
			}
		}
		return candidate;
	}

	/**
	 * Retrieve the first element of the given Set, using {@link SortedSet#first()}
	 * or otherwise using the iterator.
	 * @param set the Set to check (may be {@code null} or empty)
	 * @return the first element, or {@code null} if none
	 * @since 5.2.3
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 */
	@Contract("null -> null")
	public static <T> @Nullable T firstElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet<T> sortedSet) {
			return sortedSet.first();
		}

		Iterator<T> it = set.iterator();
		T first = null;
		if (it.hasNext()) {
			first = it.next();
		}
		return first;
	}

	/**
	 * Retrieve the first element of the given List, accessing the zero index.
	 * @param list the List to check (may be {@code null} or empty)
	 * @return the first element, or {@code null} if none
	 * @since 5.2.3
	 */
	@Contract("null -> null")
	public static <T> @Nullable T firstElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(0);
	}

	/**
	 * Retrieve the last element of the given Set, using {@link SortedSet#last()}
	 * or otherwise iterating over all elements (assuming a linked set).
	 * @param set the Set to check (may be {@code null} or empty)
	 * @return the last element, or {@code null} if none
	 * @since 5.0.3
	 * @see SortedSet
	 * @see LinkedHashMap#keySet()
	 * @see java.util.LinkedHashSet
	 */
	@Contract("null -> null")
	public static <T> @Nullable T lastElement(@Nullable Set<T> set) {
		if (isEmpty(set)) {
			return null;
		}
		if (set instanceof SortedSet<T> sortedSet) {
			return sortedSet.last();
		}

		// Full iteration necessary...
		Iterator<T> it = set.iterator();
		T last = null;
		while (it.hasNext()) {
			last = it.next();
		}
		return last;
	}

	/**
	 * Retrieve the last element of the given List, accessing the highest index.
	 * @param list the List to check (may be {@code null} or empty)
	 * @return the last element, or {@code null} if none
	 * @since 5.0.3
	 */
	@Contract("null -> null")
	public static <T> @Nullable T lastElement(@Nullable List<T> list) {
		if (isEmpty(list)) {
			return null;
		}
		return list.get(list.size() - 1);
	}

	/**
	 * Marshal the elements from the given enumeration into an array of the given type.
	 * Enumeration elements must be assignable to the type of the given array. The array
	 * returned will be a different instance than the array given.
	 */
	public static <A, E extends A> A[] toArray(Enumeration<E> enumeration, A[] array) {
		ArrayList<A> elements = new ArrayList<>();
		while (enumeration.hasMoreElements()) {
			elements.add(enumeration.nextElement());
		}
		return elements.toArray(array);
	}

	/**
	 * Adapt an {@link Enumeration} to an {@link Iterator}.
	 * @param enumeration the original {@code Enumeration}
	 * @return the adapted {@code Iterator}
	 */
	public static <E> Iterator<E> toIterator(@Nullable Enumeration<E> enumeration) {
		return (enumeration != null ? enumeration.asIterator() : Collections.emptyIterator());
	}

	/**
	 * Adapt a {@code Map<K, List<V>>} to an {@code MultiValueMap<K, V>}.
	 * @param targetMap the original map
	 * @return the adapted multi-value map (wrapping the original map)
	 * @since 3.1
	 */
	public static <K, V> MultiValueMap<K, V> toMultiValueMap(Map<K, List<V>> targetMap) {
		return new MultiValueMapAdapter<>(targetMap);
	}

	/**
	 * Return an unmodifiable view of the specified multi-value map.
	 * @param targetMap the map for which an unmodifiable view is to be returned.
	 * @return an unmodifiable view of the specified multi-value map
	 * @since 3.1
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> MultiValueMap<K, V> unmodifiableMultiValueMap(
			MultiValueMap<? extends K, ? extends V> targetMap) {

		Assert.notNull(targetMap, "'targetMap' must not be null");
		if (targetMap instanceof UnmodifiableMultiValueMap) {
			return (MultiValueMap<K, V>) targetMap;
		}
		return new UnmodifiableMultiValueMap<>(targetMap);
	}

	/**
	 * Return a (partially unmodifiable) map that combines the provided two
	 * maps. Invoking {@link Map#put(Object, Object)} or {@link Map#putAll(Map)}
	 * on the returned map results in an {@link UnsupportedOperationException}.
	 * <p>In the case of a key collision, {@code first} takes precedence over
	 * {@code second}. In other words, entries in {@code second} with a key
	 * that is also mapped by {@code first} are effectively ignored.
	 * @param first the first map to compose
	 * @param second the second map to compose
	 * @return a new map that composes the given two maps
	 * @since 6.2
	 */
	public static <K, V> Map<K, V> compositeMap(Map<K,V> first, Map<K,V> second) {
		return new CompositeMap<>(first, second);
	}

	/**
	 * Return a map that combines the provided maps. Invoking
	 * {@link Map#put(Object, Object)} on the returned map will apply
	 * {@code putFunction}, or will throw an
	 * {@link UnsupportedOperationException} {@code putFunction} is
	 * {@code null}. The same applies to {@link Map#putAll(Map)} and
	 * {@code putAllFunction}.
	 * <p>In the case of a key collision, {@code first} takes precedence over
	 * {@code second}. In other words, entries in {@code second} with a key
	 * that is also mapped by {@code first} are effectively ignored.
	 * @param first the first map to compose
	 * @param second the second map to compose
	 * @param putFunction applied when {@code Map::put} is invoked. If
	 * {@code null}, {@code Map::put} throws an
	 * {@code UnsupportedOperationException}.
	 * @param putAllFunction applied when {@code Map::putAll} is invoked. If
	 * {@code null}, {@code Map::putAll} throws an
	 * {@code UnsupportedOperationException}.
	 * @return a new map that composes the give maps
	 * @since 6.2
	 */
	public static <K, V> Map<K, V> compositeMap(Map<K,V> first, Map<K,V> second,
			@Nullable BiFunction<K, V, V> putFunction,
			@Nullable Consumer<Map<K, V>> putAllFunction) {

		return new CompositeMap<>(first, second, putFunction, putAllFunction);
	}

}
