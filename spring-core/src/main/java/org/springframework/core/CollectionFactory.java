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

package org.springframework.core;

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

import org.jspecify.annotations.Nullable;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.ReflectionUtils;

/**
 * Factory for collections that is aware of common Java and Spring collection types.
 *
 * <p>Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @author Oliver Gierke
 * @author Sam Brannen
 * @since 1.1.1
 */
public final class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = Set.of(
			// Standard collection interfaces
			Collection.class,
			List.class,
			Set.class,
			SortedSet.class,
			NavigableSet.class,
			// Common concrete collection classes
			ArrayList.class,
			LinkedList.class,
			HashSet.class,
			LinkedHashSet.class,
			TreeSet.class,
			EnumSet.class);

	private static final Set<Class<?>> approximableMapTypes = Set.of(
			// Standard map interfaces
			Map.class,
			MultiValueMap.class,
			SortedMap.class,
			NavigableMap.class,
			// Common concrete map classes
			HashMap.class,
			LinkedHashMap.class,
			LinkedMultiValueMap.class,
			TreeMap.class,
			EnumMap.class);


	private CollectionFactory() {
	}


	/**
	 * Determine whether the given collection type is an <em>approximable</em> type,
	 * i.e. a type that {@link #createApproximateCollection} can approximate.
	 * @param collectionType the collection type to check
	 * @return {@code true} if the type is <em>approximable</em>
	 */
	public static boolean isApproximableCollectionType(@Nullable Class<?> collectionType) {
		return (collectionType != null && (approximableCollectionTypes.contains(collectionType) ||
				collectionType.getName().equals("java.util.SequencedSet") ||
				collectionType.getName().equals("java.util.SequencedCollection")));
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
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <E> Collection<E> createApproximateCollection(@Nullable Object collection, int capacity) {
		if (collection instanceof EnumSet enumSet) {
			Collection<E> copy = EnumSet.copyOf(enumSet);
			copy.clear();
			return copy;
		}
		else if (collection instanceof SortedSet sortedSet) {
			return new TreeSet<>(sortedSet.comparator());
		}
		else if (collection instanceof LinkedList) {
			return new LinkedList<>();
		}
		else if (collection instanceof List) {
			return new ArrayList<>(capacity);
		}
		else {
			return new LinkedHashSet<>(capacity);
		}
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p>Delegates to {@link #createCollection(Class, Class, int)} with a
	 * {@code null} element type.
	 * @param collectionType the desired type of the target collection (never {@code null})
	 * @param capacity the initial capacity
	 * @return a new collection instance
	 * @throws IllegalArgumentException if the supplied {@code collectionType}
	 * is {@code null} or of type {@link EnumSet}
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionType, int capacity) {
		return createCollection(collectionType, null, capacity);
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code E} is
	 * not bound to the supplied {@code elementType}, type safety cannot be
	 * guaranteed if the desired {@code collectionType} is {@link EnumSet}.
	 * In such scenarios, the caller is responsible for ensuring that the
	 * supplied {@code elementType} is an enum type matching type {@code E}.
	 * As an alternative, the caller may wish to treat the return value as a
	 * raw collection or collection of {@link Object}.
	 * @param collectionType the desired type of the target collection (never {@code null})
	 * @param elementType the collection's element type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumSet} creation)
	 * @param capacity the initial capacity
	 * @return a new collection instance
	 * @throws IllegalArgumentException if the supplied {@code collectionType} is
	 * {@code null}; or if the desired {@code collectionType} is {@link EnumSet} and
	 * the supplied {@code elementType} is not a subtype of {@link Enum}
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createCollection(Class<?> collectionType, @Nullable Class<?> elementType, int capacity) {
		Assert.notNull(collectionType, "Collection type must not be null");
		if (LinkedHashSet.class == collectionType ||
				Set.class == collectionType || Collection.class == collectionType ||
				collectionType.getName().equals("java.util.SequencedSet") ||
				collectionType.getName().equals("java.util.SequencedCollection")) {
			return new LinkedHashSet<>(capacity);
		}
		else if (ArrayList.class == collectionType || List.class == collectionType) {
			return new ArrayList<>(capacity);
		}
		else if (LinkedList.class == collectionType) {
			return new LinkedList<>();
		}
		else if (TreeSet.class == collectionType || NavigableSet.class == collectionType ||
				SortedSet.class == collectionType) {
			return new TreeSet<>();
		}
		else if (EnumSet.class.isAssignableFrom(collectionType)) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			return EnumSet.noneOf(asEnumType(elementType));
		}
		else if (HashSet.class == collectionType) {
			return new HashSet<>(capacity);
		}
		else {
			if (collectionType.isInterface() || !Collection.class.isAssignableFrom(collectionType)) {
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
	 * Determine whether the given map type is an <em>approximable</em> type,
	 * i.e. a type that {@link #createApproximateMap} can approximate.
	 * @param mapType the map type to check
	 * @return {@code true} if the type is <em>approximable</em>
	 */
	public static boolean isApproximableMapType(@Nullable Class<?> mapType) {
		return (mapType != null && (approximableMapTypes.contains(mapType) ||
				mapType.getName().equals("java.util.SequencedMap")));
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
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createApproximateMap(@Nullable Object map, int capacity) {
		if (map instanceof EnumMap enumMap) {
			EnumMap copy = new EnumMap(enumMap);
			copy.clear();
			return copy;
		}
		else if (map instanceof SortedMap sortedMap) {
			return new TreeMap<>(sortedMap.comparator());
		}
		else if (map instanceof MultiValueMap) {
			return new LinkedMultiValueMap(capacity);
		}
		else {
			return new LinkedHashMap<>(capacity);
		}
	}

	/**
	 * Create the most appropriate map for the given map type.
	 * <p>Delegates to {@link #createMap(Class, Class, int)} with a
	 * {@code null} key type.
	 * @param mapType the desired type of the target map
	 * @param capacity the initial capacity
	 * @return a new map instance
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null} or of type {@link EnumMap}
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int capacity) {
		return createMap(mapType, null, capacity);
	}

	/**
	 * Create the most appropriate map for the given map type.
	 * <p><strong>Warning</strong>: Since the parameterized type {@code K}
	 * is not bound to the supplied {@code keyType}, type safety cannot be
	 * guaranteed if the desired {@code mapType} is {@link EnumMap}. In such
	 * scenarios, the caller is responsible for ensuring that the {@code keyType}
	 * is an enum type matching type {@code K}. As an alternative, the caller
	 * may wish to treat the return value as a raw map or map keyed by
	 * {@link Object}. Similarly, type safety cannot be enforced if the
	 * desired {@code mapType} is {@link MultiValueMap}.
	 * @param mapType the desired type of the target map (never {@code null})
	 * @param keyType the map's key type, or {@code null} if unknown
	 * (note: only relevant for {@link EnumMap} creation)
	 * @param capacity the initial capacity
	 * @return a new map instance
	 * @throws IllegalArgumentException if the supplied {@code mapType} is
	 * {@code null}; or if the desired {@code mapType} is {@link EnumMap} and
	 * the supplied {@code keyType} is not a subtype of {@link Enum}
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 * @see java.util.EnumMap
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	public static <K, V> Map<K, V> createMap(Class<?> mapType, @Nullable Class<?> keyType, int capacity) {
		Assert.notNull(mapType, "Map type must not be null");
		if (LinkedHashMap.class == mapType || Map.class == mapType ||
				mapType.getName().equals("java.util.SequencedMap")) {
			return new LinkedHashMap<>(capacity);
		}
		else if (LinkedMultiValueMap.class == mapType || MultiValueMap.class == mapType) {
			return new LinkedMultiValueMap();
		}
		else if (TreeMap.class == mapType || SortedMap.class == mapType || NavigableMap.class == mapType) {
			return new TreeMap<>();
		}
		else if (EnumMap.class == mapType) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(asEnumType(keyType));
		}
		else if (HashMap.class == mapType) {
			return new HashMap<>(capacity);
		}
		else {
			if (mapType.isInterface() || !Map.class.isAssignableFrom(mapType)) {
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
	 * Create a variant of {@link java.util.Properties} that automatically adapts
	 * non-String values to String representations in {@link Properties#getProperty}.
	 * <p>In addition, the returned {@code Properties} instance sorts properties
	 * alphanumerically based on their keys.
	 * @return a new {@code Properties} instance
	 * @since 4.3.4
	 * @see #createSortedProperties(boolean)
	 * @see #createSortedProperties(Properties, boolean)
	 */
	@SuppressWarnings("serial")
	public static Properties createStringAdaptingProperties() {
		return new SortedProperties(false) {
			@Override
			public @Nullable String getProperty(String key) {
				Object value = get(key);
				return (value != null ? value.toString() : null);
			}
		};
	}

	/**
	 * Create a variant of {@link java.util.Properties} that sorts properties
	 * alphanumerically based on their keys.
	 * <p>This can be useful when storing the {@link Properties} instance in a
	 * properties file, since it allows such files to be generated in a repeatable
	 * manner with consistent ordering of properties. Comments in generated
	 * properties files can also be optionally omitted.
	 * @param omitComments {@code true} if comments should be omitted when
	 * storing properties in a file
	 * @return a new {@code Properties} instance
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(Properties, boolean)
	 */
	public static Properties createSortedProperties(boolean omitComments) {
		return new SortedProperties(omitComments);
	}

	/**
	 * Create a variant of {@link java.util.Properties} that sorts properties
	 * alphanumerically based on their keys.
	 * <p>This can be useful when storing the {@code Properties} instance in a
	 * properties file, since it allows such files to be generated in a repeatable
	 * manner with consistent ordering of properties. Comments in generated
	 * properties files can also be optionally omitted.
	 * <p>The returned {@code Properties} instance will be populated with
	 * properties from the supplied {@code properties} object, but default
	 * properties from the supplied {@code properties} object will not be copied.
	 * @param properties the {@code Properties} object from which to copy the
	 * initial properties
	 * @param omitComments {@code true} if comments should be omitted when
	 * storing properties in a file
	 * @return a new {@code Properties} instance
	 * @since 5.2
	 * @see #createStringAdaptingProperties()
	 * @see #createSortedProperties(boolean)
	 */
	public static Properties createSortedProperties(Properties properties, boolean omitComments) {
		return new SortedProperties(properties, omitComments);
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
