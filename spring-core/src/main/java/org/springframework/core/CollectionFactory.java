/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.springframework.util.Assert;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

/**
 * Factory for collections, being aware of Java 5 and Java 6 collections.
 * Mainly for internal use within the framework.
 *
 * <p>The goal of this class is to avoid runtime dependencies on a specific
 * Java version, while nevertheless using the best collection implementation
 * that is available at runtime.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 1.1.1
 */
public abstract class CollectionFactory {

	private static final Set<Class<?>> approximableCollectionTypes = new HashSet<Class<?>>(10);

	private static final Set<Class<?>> approximableMapTypes = new HashSet<Class<?>>(6);


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
	 * Determine whether the given collection type is an approximable type,
	 * i.e. a type that {@link #createApproximateCollection} can approximate.
	 * @param collectionType the collection type to check
	 * @return {@code true} if the type is approximable,
	 * {@code false} if it is not
	 */
	public static boolean isApproximableCollectionType(Class<?> collectionType) {
		return (collectionType != null && approximableCollectionTypes.contains(collectionType));
	}

	/**
	 * Create the most approximate collection for the given collection.
	 * @param collection the original Collection object
	 * @param capacity the initial capacity
	 * @return the new Collection instance
	 * @see java.util.LinkedHashSet
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 * @see java.util.ArrayList
	 * @see java.util.LinkedList
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createApproximateCollection(Object collection, int capacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<E>();
		}
		else if (collection instanceof List) {
			return new ArrayList<E>(capacity);
		}
		else if (collection instanceof EnumSet) {
			return EnumSet.copyOf((Collection) collection);
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<E>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<E>(capacity);
		}
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p>Delegates to {@link #createCollection(Class, Class, int)} with a
	 * {@code null} element type.
	 * @param collectionClass the desired type of the target Collection
	 * @param capacity the initial capacity
	 * @return the new Collection instance
	 */
	public static <E> Collection<E> createCollection(Class<?> collectionClass, int capacity) {
		return createCollection(collectionClass, null, capacity);
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * @param collectionClass the desired type of the target Collection
	 * @param elementType the collection's element type, or {@code null} if not known
	 * (note: only relevant for {@link EnumSet} creation)
	 * @param capacity the initial capacity
	 * @return the new Collection instance
	 * @since 4.1.3
	 * @see java.util.LinkedHashSet
	 * @see java.util.TreeSet
	 * @see java.util.EnumSet
	 * @see java.util.ArrayList
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createCollection(Class<?> collectionClass, Class<?> elementType, int capacity) {
		if (collectionClass.isInterface()) {
			if (Set.class.equals(collectionClass) || Collection.class.equals(collectionClass)) {
				return new LinkedHashSet<E>(capacity);
			}
			else if (List.class.equals(collectionClass)) {
				return new ArrayList<E>(capacity);
			}
			else if (SortedSet.class.equals(collectionClass) || NavigableSet.class.equals(collectionClass)) {
				return new TreeSet<E>();
			}
			else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionClass.getName());
			}
		}
		else if (EnumSet.class.equals(collectionClass)) {
			Assert.notNull(elementType, "Cannot create EnumSet for unknown element type");
			return EnumSet.noneOf((Class) elementType);
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionClass)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionClass.getName());
			}
			try {
				return (Collection<E>) collectionClass.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(
						"Could not instantiate Collection type: " + collectionClass.getName(), ex);
			}
		}
	}

	/**
	 * Determine whether the given map type is an approximable type,
	 * i.e. a type that {@link #createApproximateMap} can approximate.
	 * @param mapType the map type to check
	 * @return {@code true} if the type is approximable,
	 * {@code false} if it is not
	 */
	public static boolean isApproximableMapType(Class<?> mapType) {
		return (mapType != null && approximableMapTypes.contains(mapType));
	}

	/**
	 * Create the most approximate map for the given map.
	 * @param map the original Map object
	 * @param capacity the initial capacity
	 * @return the new Map instance
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createApproximateMap(Object map, int capacity) {
		if (map instanceof EnumMap) {
			return new EnumMap((Map) map);
		}
		else if (map instanceof SortedMap) {
			return new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<K, V>(capacity);
		}
	}

	/**
	 * Create the most approximate map for the given map.
	 * <p>Delegates to {@link #createMap(Class, Class, int)} with a
	 * {@code null} key type.
	 * @param mapClass the desired type of the target Map
	 * @param capacity the initial capacity
	 * @return the new Map instance
	 */
	public static <K, V> Map<K, V> createMap(Class<?> mapClass, int capacity) {
		return createMap(mapClass, null, capacity);
	}

	/**
	 * Create the most approximate map for the given map.
	 * @param mapClass the desired type of the target Map
	 * @param keyType the map's key type, or {@code null} if not known
	 * (note: only relevant for {@link EnumMap} creation)
	 * @param capacity the initial capacity
	 * @return the new Map instance
	 * @since 4.1.3
	 * @see java.util.LinkedHashMap
	 * @see java.util.TreeMap
	 * @see java.util.EnumMap
	 * @see org.springframework.util.LinkedMultiValueMap
	 */
	@SuppressWarnings({"unchecked", "rawtypes"})
	public static <K, V> Map<K, V> createMap(Class<?> mapClass, Class<?> keyType, int capacity) {
		if (mapClass.isInterface()) {
			if (Map.class.equals(mapClass)) {
				return new LinkedHashMap<K, V>(capacity);
			}
			else if (SortedMap.class.equals(mapClass) || NavigableMap.class.equals(mapClass)) {
				return new TreeMap<K, V>();
			}
			else if (MultiValueMap.class.equals(mapClass)) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapClass.getName());
			}
		}
		else if (EnumMap.class.equals(mapClass)) {
			Assert.notNull(keyType, "Cannot create EnumMap for unknown key type");
			return new EnumMap(keyType);
		}
		else {
			if (!Map.class.isAssignableFrom(mapClass)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapClass.getName());
			}
			try {
				return (Map<K, V>) mapClass.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException(
						"Could not instantiate Map type: " + mapClass.getName(), ex);
			}
		}
	}

}
