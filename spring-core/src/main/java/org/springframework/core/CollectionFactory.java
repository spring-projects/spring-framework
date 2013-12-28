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

package org.springframework.core;

import java.util.ArrayList;
import java.util.Collection;
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
		approximableMapTypes.add(HashMap.class);
		approximableMapTypes.add(LinkedHashMap.class);
		approximableMapTypes.add(TreeMap.class);
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
	 * <p>Creates an ArrayList, TreeSet or linked Set for a List, SortedSet
	 * or Set, respectively.
	 * @param collection the original Collection object
	 * @param initialCapacity the initial capacity
	 * @return the new Collection instance
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createApproximateCollection(Object collection, int initialCapacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList<E>();
		}
		else if (collection instanceof List) {
			return new ArrayList<E>(initialCapacity);
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet<E>(((SortedSet<E>) collection).comparator());
		}
		else {
			return new LinkedHashSet<E>(initialCapacity);
		}
	}

	/**
	 * Create the most appropriate collection for the given collection type.
	 * <p>Creates an ArrayList, TreeSet or linked Set for a List, SortedSet
	 * or Set, respectively.
	 * @param collectionType the desired type of the target Collection
	 * @param initialCapacity the initial capacity
	 * @return the new Collection instance
	 * @see java.util.ArrayList
	 * @see java.util.TreeSet
	 * @see java.util.LinkedHashSet
	 */
	@SuppressWarnings("unchecked")
	public static <E> Collection<E> createCollection(Class<?> collectionType, int initialCapacity) {
		if (collectionType.isInterface()) {
			if (List.class.equals(collectionType)) {
				return new ArrayList<E>(initialCapacity);
			}
			else if (SortedSet.class.equals(collectionType) || NavigableSet.class.equals(collectionType)) {
				return new TreeSet<E>();
			}
			else if (Set.class.equals(collectionType) || Collection.class.equals(collectionType)) {
				return new LinkedHashSet<E>(initialCapacity);
			}
			else {
				throw new IllegalArgumentException("Unsupported Collection interface: " + collectionType.getName());
			}
		}
		else {
			if (!Collection.class.isAssignableFrom(collectionType)) {
				throw new IllegalArgumentException("Unsupported Collection type: " + collectionType.getName());
			}
			try {
				return (Collection<E>) collectionType.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Could not instantiate Collection type: " +
						collectionType.getName(), ex);
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
	 * <p>Creates a TreeMap or linked Map for a SortedMap or Map, respectively.
	 * @param map the original Map object
	 * @param initialCapacity the initial capacity
	 * @return the new Map instance
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings("unchecked")
	public static <K, V> Map<K, V> createApproximateMap(Object map, int initialCapacity) {
		if (map instanceof SortedMap) {
			return new TreeMap<K, V>(((SortedMap<K, V>) map).comparator());
		}
		else {
			return new LinkedHashMap<K, V>(initialCapacity);
		}
	}

	/**
	 * Create the most approximate map for the given map.
	 * <p>Creates a TreeMap or linked Map for a SortedMap or Map, respectively.
	 * @param mapType the desired type of the target Map
	 * @param initialCapacity the initial capacity
	 * @return the new Map instance
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static <K, V> Map<K, V> createMap(Class<?> mapType, int initialCapacity) {
		if (mapType.isInterface()) {
			if (Map.class.equals(mapType)) {
				return new LinkedHashMap<K, V>(initialCapacity);
			}
			else if (SortedMap.class.equals(mapType) || NavigableMap.class.equals(mapType)) {
				return new TreeMap<K, V>();
			}
			else if (MultiValueMap.class.equals(mapType)) {
				return new LinkedMultiValueMap();
			}
			else {
				throw new IllegalArgumentException("Unsupported Map interface: " + mapType.getName());
			}
		}
		else {
			if (!Map.class.isAssignableFrom(mapType)) {
				throw new IllegalArgumentException("Unsupported Map type: " + mapType.getName());
			}
			try {
				return (Map<K, V>) mapType.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " +
						mapType.getName(), ex);
			}
		}
	}

}
