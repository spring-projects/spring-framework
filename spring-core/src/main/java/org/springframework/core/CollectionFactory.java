/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.core;

import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableCollection;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSortedMap;
import com.google.common.collect.Iterables;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import java.util.function.Consumer;
import java.util.stream.StreamSupport;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Factory for collections that is aware of Java 5, Java 6, and Spring collection types.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 1.1.1
 */
public abstract class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = new HashSet<>();

	private static final Set<Class<?>> approximableMapTypes = new HashSet<>();


	static {
		// Standard collection interfaces
		approximableCollectionTypes.add(Collection.class);
		approximableCollectionTypes.add(List.class);
		approximableCollectionTypes.add(Set.class);
		approximableCollectionTypes.add(SortedSet.class);
		approximableCollectionTypes.add(NavigableSet.class);
		approximableMapTypes.add(Map.class);
		approximableMapTypes.add(SortedMap.class);
		approximableMapTypes.add(NavigableMap.class);

		// Common concrete collection classes
		approximableCollectionTypes.add(ArrayList.class);
		approximableCollectionTypes.add(LinkedList.class);
		approximableCollectionTypes.add(HashSet.class);
		approximableCollectionTypes.add(LinkedHashSet.class);
		approximableCollectionTypes.add(TreeSet.class);
		approximableCollectionTypes.add(EnumSet.class);
		approximableMapTypes.add(HashMap.class);
		approximableMapTypes.add(LinkedHashMap.class);
		approximableMapTypes.add(TreeMap.class);
		approximableMapTypes.add(EnumMap.class);
	}


	/**
	 * Determine whether the given collection type is an <em>approximable</em> type,
	 * i.e. a type that {@link #createApproximateCollection} can approximate.
	 * @param collectionType the collection type to check
	 * @return {@code true} if the type is <em>approximable</em>
	 */
	public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * Create the most approximate collection for the given collection.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code E} is
	 * not bound to the type of elements contained in the supplied
	 * {@code collection}, type safety cannot be guaranteed if the supplied
	 * {@code collection} is an {@link EnumSet}. In such scenarios, the caller
	 * is responsible for ensuring that the element type for the supplied
	 * {@code collection} is an enum type matching type {@code E}. As an
	 * alternative, the caller may wish to treat the return value as a raw
	 * collection or collection of {@link Object}.
	 * @param collection the original collection object, potentially {@code null}
	 * @param capacity the initial capacity
	 * @return a new, empty collection instance
	 * @see #isApproximableCollectionType
	 * @see java.util.LinkedList
	 * @see java.util.ArrayList
	 * @see java.util.EnumSet
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings({ "unchecked", "cast", "rawtypes" })
	public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<>();
		}
		else if (collection instanceof List) {
			return new ArrayList<>(capacity);
		}
		else if (collection instanceof EnumSet) {
			// Cast is necessary for compilation in Eclipse 4.4.1.
			Collection<E> enumSet = (Collection<E>) EnumSet.copyOf((EnumSet) collection);
			enumSet.clear();
			return enumSet;
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<>(capacity);
		}
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p>Delegates to {@link #createCollection(Class, Class, int)} with a
	 * {@code null} element type.
	 * <p><strong>Warning</strong>: This method does not support the Guava {@link ImmutableCollection} types.
	 * For those collection types see {@link #createCollectionOf(Class, Iterable)}
	 * @param collectionType the desired type of the target collection; never {@code null}
	 * @param capacity the initial capacity
	 * @return a new collection instance
	 * @see #createCollectionOf(Class, Iterable)
	 * @throws IllegalArgumentException if the supplied {@code collectionType}
	 * is {@code null} or of type {@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p><strong>Warning</strong>: This method does not support the Guava {@link ImmutableCollection} types.
	 * For those collection types see {@link #createCollectionOf(Class, Iterable, Class)}
	 * <p><strong>Warning</strong>: Since the parameterized type {@code E} is
	 * not bound to the supplied {@code elementType}, type safety cannot be
	 * guaranteed if the desired {@code collectionType} is {@link EnumSet}.
	 * In such scenarios, the caller is responsible for ensuring that the
	 * supplied {@code elementType} is an enum type matching type {@code E}.
	 * As an alternative, the caller may wish to treat the return value as a
	 * raw collection or collection of {@link Object}.
	 * @param collectionType the desired type of the target collection; never {@code null}
	 * @param elementType the collection's element type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumSet} creation)
	 * @param capacity the initial capacity
	 * @return a new collection instance
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 * @throws IllegalArgumentException if the supplied {@code collectionType} is
	 * {@code null}; or if the desired {@code collectionType} is {@link EnumSet} and
	 * the supplied {@code elementType} is not a subtype of {@link Enum}
	 * @see #createCollectionOf(Class, Iterable, Class)
	 */
	@SuppressWarnings({ "unchecked", "cast" })
	public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (collectionType.isInterface()) {
			if (Set.class == collectionType || Collection.class == collectionType) {
				return new LinkedHashSet<>(capacity);
			}
			else if (List.class == collectionType) {
				return new ArrayList<>(capacity);
			}
			else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
				return new TreeSet<>();
			}
			else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
			}
		}
		else if (EnumSet.class == collectionType) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			// Cast is necessary for compilation in Eclipse 4.4.1.
			return (Collection<E>) EnumSet.noneOf(asEnumType(elementType));
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) ReflectionUtils.accessibleConstructor(collectionType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
					"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	/**
	 * Create the most appropriate collection for the given collection type and
	 * populates the {@link Collection} with the provided set of {@code elements}.
	 * The distinction between this method and {@link #createCollection(Class, int)}
	 * is that this method support's the Guava {@link ImmutableCollection} types
	 * since it is capable of populating the created {@link ImmutableCollection}.
	 * The supported Guava types are {@link ImmutableCollection}, {@link ImmutableList}
	 * {@link ImmutableSet} and {@link ImmutableMultiset}.
	 * <p>Delegates to {@link #createCollection(Class, Class, int)} with a
	 * {@code null} element type.
	 * @param collectionType the desired type of the target collection; never {@code null}
	 * @param elements the set of values to include in the created {@link Collection}, or {@code null}
	 * no elements should be inserted
	 * @return a new collection instance populated with {@code elements}
	 * @throws IllegalArgumentException if the supplied {@code collectionType}
	 * is {@code null} or of type {@link EnumSet}
	 * @see ImmutableCollection
	 * @see ImmutableList
	 * @see ImmutableSet
	 * @see ImmutableMultiset
	 * @see #createCollection(Class, int)
	 */
	public static <C extends Collection<E>, E> C createCollectionOf(Class<?> collectionType,
			@Nullable Iterable<? extends E> elements) {
		return createCollectionOf(collectionType, elements, null);
	}

	/**
	 * Create the most appropriate collection for the given collection type and
	 * populates the {@link Collection} with the provided set of {@code elements}.
	 * The distinction between this method and {@link #createCollection(Class, Class, int)}
	 * is that this method support's the Guava {@link ImmutableCollection} types
	 * since it is capable of populating the created {@link ImmutableCollection}.
	 * The supported Guava types are {@link ImmutableCollection}, {@link ImmutableList}
	 * {@link ImmutableSet} and {@link ImmutableMultiset}.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code E} is
	 * not bound to the supplied {@code elementType}, type safety cannot be
	 * guaranteed if the desired {@code collectionType} is {@link EnumSet}.
	 * In such scenarios, the caller is responsible for ensuring that the
	 * supplied {@code elementType} is an enum type matching type {@code E}.
	 * As an alternative, the caller may wish to treat the return value as a
	 * raw collection or collection of {@link Object}.
	 * @param collectionType the desired type of the target collection; never {@code null}
	 * @param elements the set of values to include in the created {@link Collection}, or {@code null}
	 * no elements should be inserted
	 * @param elementType the collection's element type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumSet} creation)
	 * @return a new collection instance populated with {@code elements}
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 * @see ImmutableCollection
	 * @see ImmutableList
	 * @see ImmutableSet
	 * @see ImmutableMultiset
	 * @see #createCollection(Class, Class, int)
	 * @throws IllegalArgumentException if the supplied {@code collectionType} is
	 * {@code null}; or if the desired {@code collectionType} is {@link EnumSet} and
	 * the supplied {@code elementType} is not a subtype of {@link Enum}
	 */
	@SuppressWarnings({"unchecked", "cast"})
	public static <C extends Collection<E>, E> C createCollectionOf(Class<?> collectionType,
			@Nullable Iterable<? extends E> elements,
			@Nullable Class<?> elementType) {
		int capacity = elements == null ? 0 : Iterables.size(elements);
		Assert.notNull(collectionType, "Collection type must not be null");
		if (collectionType.isInterface()) {
			C resultCollection;
			if (Set.class == collectionType || Collection.class == collectionType) {
				resultCollection = (C) new LinkedHashSet<E>(capacity);
			}
			else if (List.class == collectionType) {
				resultCollection = (C) new ArrayList<E>(capacity);
			}
			else if (SortedSet.class == collectionType || NavigableSet.class == collectionType) {
				resultCollection = (C) new TreeSet<E>();
			}
			else {
				throw new IllegalArgumentException(
						"Unsupported Collection interface: " + collectionType.getName());
			}
			return addAllElements(elements, resultCollection);
		}
		else if (EnumSet.class == collectionType) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			// Cast is necessary for compilation in Eclipse 4.4.1.
			return addAllElements(elements, (C) EnumSet.noneOf(asEnumType(elementType)));
		}
		else if (ImmutableCollection.class.isAssignableFrom(collectionType)) {
			ImmutableCollection.Builder<E> builder;
			if (ImmutableCollection.class == collectionType
					|| ImmutableList.class == collectionType) {
				builder = ImmutableList.builder();
			}
			else if (ImmutableSet.class == collectionType) {
				builder = ImmutableSet.builder();
			}
			else if (ImmutableMultiset.class == collectionType) {
				builder = ImmutableMultiset.builder();
			}
			else {
				throw new IllegalArgumentException(
						"Unsupported ImmutableCollection interface: " + collectionType.getName());
			}
			nullableForEach(elements, builder::add);
			return (C) builder.build();
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException(
						"Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return addAllElements(elements,
						(C) ReflectionUtils.accessibleConstructor(collectionType).newInstance());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate Collection type: " + collectionType.getName(), ex);
			}
		}
	}

	private static <C extends Collection<E>, E> C addAllElements(
			@Nullable Iterable<? extends E> elements,
			C collection) {
		nullableForEach(elements, collection::add);
		return collection;
	}

	private static <E> void nullableForEach(@Nullable Iterable<E> elements,
			Consumer<E> consumer) {
		if (elements != null) {
			StreamSupport.stream(elements.spliterator(), false).forEach(consumer);
		}
	}

	/**
	 * Determine whether the given map type is an <em>approximable</em> type,
	 * i.e. a type that {@link #createApproximateMap} can approximate.
	 * @param mapType the map type to check
	 * @return {@code true} if the type is <em>approximable</em>
	 */
	public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * Create the most approximate map for the given map.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code K} is
	 * not bound to the type of keys contained in the supplied {@code map},
	 * type safety cannot be guaranteed if the supplied {@code map} is an
	 * {@link EnumMap}. In such scenarios, the caller is responsible for
	 * ensuring that the key type in the supplied {@code map} is an enum type
	 * matching type {@code K}. As an alternative, the caller may wish to
	 * treat the return value as a raw map or map keyed by {@link Object}.
	 * @param map the original map object, potentially {@code null}
	 * @param capacity the initial capacity
	 * @return a new, empty map instance
	 * @see #isApproximableMapType
	 * @see java.util.EnumMap
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
		if (map instanceof EnumMap) {
			EnumMap enumMap = new EnumMap((EnumMap) map);
			enumMap.clear();
			return enumMap;
		}
		else if (map instanceof SortedMap) {
			return new TreeMap<>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<>(capacity);
		}
	}

	/**
	 * Create the most appropriate map for the given map type.
	 * <p>Delegates to {@link #createMap(Class, Class, int)} with a
	 * {@code null} key type.
	 * <p><strong>Warning</strong>: This method does not support the Guava {@link ImmutableMap} types.
	 * For those collection types see {@link #createMapOf(Class, Iterable)}
	 * @param mapType the desired type of the target map
	 * @param capacity the initial capacity
	 * @return a new map instance
	 * @see #createMapOf(Class, Iterable)
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null} or of type {@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * Create the most appropriate map for the given map type.
	 * <p><strong>Warning</strong>: This method does not support the Guava {@link ImmutableMap} types.
	 * For those collection types see {@link #createMapOf(Class, Iterable, Class)}
	 * <p><strong>Warning</strong>: Since the parameterized type {@code K}
	 * is not bound to the supplied {@code keyType}, type safety cannot be
	 * guaranteed if the desired {@code mapType} is {@link EnumMap}. In such
	 * scenarios, the caller is responsible for ensuring that the {@code keyType}
	 * is an enum type matching type {@code K}. As an alternative, the caller
	 * may wish to treat the return value as a raw map or map keyed by
	 * {@link Object}. Similarly, type safety cannot be enforced if the
	 * desired {@code mapType} is {@link MultiValueMap}.
	 * @param mapType the desired type of the target map; never {@code null}
	 * @param keyType the map's key type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumMap} creation)
	 * @param capacity the initial capacity
	 * @return a new map instance
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 * @see #createMapOf(Class, Iterable, Class)
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null}; or if the desired {@code mapType} is {@link EnumMap} and
	 * the supplied {@code keyType} is not a subtype of {@link Enum}
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			if (Map.class == mapType) {
				return new LinkedHashMap<>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				return new TreeMap<>();
			}
			else if (MultiValueMap.class == mapType) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) ReflectionUtils.accessibleConstructor(mapType).newInstance();
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	/**
	 * Create the most appropriate map for the given map type and
	 * populates the {@link Map} with the provided set of {@code elements}.
	 * The distinction between this method and {@link #createMap(Class, int)}
	 * is that this method support's the Guava {@link ImmutableMap} types
	 * since it is capable of populating the created {@link ImmutableMap}.
	 * The supported Guava types are  {@link ImmutableMap}, {@link ImmutableBiMap}, and
	 * {@link ImmutableSortedMap}. Although {@link ImmutableSortedMap} is supported note that it is created with
	 * natural order which may not be desired ordering.
	 * <p>Delegates to {@link #createMapOf(Class, Iterable, Class)} with a
	 * {@code null} key type.
	 * @param mapType the desired type of the target map
	 * @param elements the set of values to include in the created {@link Map}, or {@code null}
	 * no elements should be inserted
	 * @return a new map instance populated with {@code elements}
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null} or of type {@link EnumMap}
	 * @see #createMap(Class, int)
	 * @see ImmutableMap
	 * @see ImmutableBiMap
	 * @see ImmutableSortedMap
	 */
	public static <M extends Map<K, V>, K, V> M createMapOf(Class<?> mapType,
			@Nullable Iterable<? extends Map.Entry<? extends K, ? extends V>> elements) {
		return createMapOf(mapType, elements, null);
	}

	/**
	 * Create the most appropriate map for the given map type and
	 * populates the {@link Map} with the provided set of {@code elements}.
	 * The distinction between this method and {@link #createMap(Class, Class, int)}
	 * is that this method support's the Guava {@link ImmutableMap} types
	 * since it is capable of populating the created {@link ImmutableMap}.
	 * The supported Guava types are  {@link ImmutableMap}, {@link ImmutableBiMap}, and
	 * {@link ImmutableSortedMap}. Although {@link ImmutableSortedMap} is supported note that it is created with
	 * natural order which may not be desired ordering.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code K}
	 * is not bound to the supplied {@code keyType}, type safety cannot be
	 * guaranteed if the desired {@code mapType} is {@link EnumMap}. In such
	 * scenarios, the caller is responsible for ensuring that the {@code keyType}
	 * is an enum type matching type {@code K}. As an alternative, the caller
	 * may wish to treat the return value as a raw map or map keyed by
	 * {@link Object}. Similarly, type safety cannot be enforced if the
	 * desired {@code mapType} is {@link MultiValueMap}.
	 * @param mapType the desired type of the target map; never {@code null}
	 * @param keyType the map's key type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumMap} creation)
	 * @return a new map instance
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 * @see ImmutableMap
	 * @see ImmutableBiMap
	 * @see ImmutableSortedMap
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null}; or if the desired {@code mapType} is {@link EnumMap} and
	 * the supplied {@code keyType} is not a subtype of {@link Enum}
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <M extends Map<K, V>, K, V> M createMapOf(Class<?> mapType,
			@Nullable Iterable<? extends Map.Entry<? extends K, ? extends V>> elements,
			@Nullable Class<?> keyType) {
		int capacity = elements == null ? 0 : Iterables.size(elements);
		Assert.notNull(mapType, "Map type must not be null");
		if (mapType.isInterface()) {
			M resultMap;
			if (Map.class == mapType) {
				resultMap = (M) new LinkedHashMap<K, V>(capacity);
			}
			else if (SortedMap.class == mapType || NavigableMap.class == mapType) {
				resultMap = (M) new TreeMap<K, V>();
			}
			else if (MultiValueMap.class == mapType) {
				resultMap = (M) new LinkedMultiValueMap<K, V>();
			}
			else {
				throw new IllegalArgumentException(
						"Unsupported Map interface: " + mapType.getName());
			}
			return addAllEntries(elements, resultMap);
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return addAllEntries(elements, (M) new EnumMap(asEnumType(keyType)));
		}
		else if (ImmutableMap.class.isAssignableFrom(mapType)) {
			ImmutableMap.Builder<K, V> builder;
			if (ImmutableMap.class == mapType) {
				builder = ImmutableMap.builder();
			}
			else if (ImmutableBiMap.class == mapType) {
				builder = ImmutableBiMap.builder();
			}
			else if (ImmutableSortedMap.class == mapType)
				builder = (ImmutableMap.Builder) ImmutableSortedMap.naturalOrder();
			else {
				throw new IllegalArgumentException(
						"Unsupported ImmutableMap interface: " + mapType.getName());
			}
			nullableForEach(elements, builder::put);
			return (M) builder.build();
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return addAllEntries(elements,
						(M) ReflectionUtils.accessibleConstructor(mapType).newInstance());
			}
			catch (Throwable ex) {
				throw new IllegalArgumentException(
						"Could not instantiate Map type: " + mapType.getName(), ex);
			}
		}
	}

	private static <M extends Map<K, V>, K, V> M addAllEntries(
			@Nullable Iterable<? extends Map.Entry<? extends K, ? extends V>> elements,
			M map) {
		nullableForEach(elements, p -> map.put(p.getKey(), p.getValue()));
		return map;
	}

	/**
	 * Create a variant of {@code java.util.Properties} that automatically adapts
	 * non-String values to String representations on {@link Properties#getProperty}.
	 * @return a new {@code Properties} instance
	 * @since 4.3.4
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new Properties() {
			@Override
			@Nullable
			public String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * Cast the given type to a subtype of {@link Enum}.
	 * @param enumType the enum type, never {@code null}
	 * @return the given type as subtype of {@link Enum}
	 * @throws IllegalArgumentException if the given type is not a subtype of {@link Enum}
	 */
	@SuppressWarnings("rawtypes")
	private static Class<? extends Enum> asEnumType(Class<?> enumType) {
		Assert.notNull(enumType, "Enum type must not be null");
		if (!Enum.class.isAssignableFrom(enumType)) {
			throw new IllegalArgumentException("Supplied type is not an enum: " + enumType.getName());
		}
		return enumType.asSubclass(Enum.class);
	}

}
