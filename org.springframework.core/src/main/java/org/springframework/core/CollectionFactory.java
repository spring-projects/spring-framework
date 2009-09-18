/*
 * Copyright 2002-2009 the original author or authors.
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
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * Factory for collections, being aware of Commons Collection 3.x's extended
 * collections as well as of JDK 1.5+ concurrent collections.
 * Mainly for internal use within the framework.
 *
 * <p>The goal of this class is to avoid runtime dependencies on JDK 1.5+ and
 * Commons Collections 3.x, simply using the best collection implementation
 * that is available at runtime.
 *
 * @author Juergen Hoeller
 * @author Arjen Poutsma
 * @since 1.1.1
 */
public abstract class CollectionFactory {

	private static Class navigableSetClass = null;

	private static Class navigableMapClass = null;

	private static final Set<Class> approximableCollectionTypes = new HashSet<Class>(10);

	private static final Set<Class> approximableMapTypes = new HashSet<Class>(6);


	static {
		// Standard collection interfaces
		approximableCollectionTypes.add(Collection.class);
		approximableCollectionTypes.add(List.class);
		approximableCollectionTypes.add(Set.class);
		approximableCollectionTypes.add(SortedSet.class);
		approximableMapTypes.add(Map.class);
		approximableMapTypes.add(SortedMap.class);

		// New Java 6 collection interfaces
		ClassLoader cl = CollectionFactory.class.getClassLoader();
		try {
			navigableSetClass = cl.loadClass("java.util.NavigableSet");
			navigableMapClass = cl.loadClass("java.util.NavigableMap");
			approximableCollectionTypes.add(navigableSetClass);
			approximableMapTypes.add(navigableMapClass);
		}
		catch (ClassNotFoundException ex) {
			// not running on Java 6 or above...
		}

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
	 * Create a linked Set if possible: This implementation always
	 * creates a {@link java.util.LinkedHashSet}, since Spring 2.5
	 * requires JDK 1.4 anyway.
	 * @param initialCapacity the initial capacity of the Set
	 * @return the new Set instance
	 * @deprecated as of Spring 2.5, for usage on JDK 1.4 or higher
	 */
	@Deprecated
	public static <T> Set<T> createLinkedSetIfPossible(int initialCapacity) {
		return new LinkedHashSet<T>(initialCapacity);
	}

	/**
	 * Create a copy-on-write Set (allowing for synchronization-less iteration) if possible:
	 * This implementation always creates a {@link java.util.concurrent.CopyOnWriteArraySet},
	 * since Spring 3 requires JDK 1.5 anyway.
	 * @return the new Set instance
	 * @deprecated as of Spring 3.0, for usage on JDK 1.5 or higher
	 */
	@Deprecated
	public static <T> Set<T> createCopyOnWriteSet() {
		return new CopyOnWriteArraySet<T>();
	}

	/**
	 * Create a linked Map if possible: This implementation always
	 * creates a {@link java.util.LinkedHashMap}, since Spring 2.5
	 * requires JDK 1.4 anyway.
	 * @param initialCapacity the initial capacity of the Map
	 * @return the new Map instance
	 * @deprecated as of Spring 2.5, for usage on JDK 1.4 or higher
	 */
	@Deprecated
	public static <K,V> Map<K,V> createLinkedMapIfPossible(int initialCapacity) {
		return new LinkedHashMap<K,V>(initialCapacity);
	}

	/**
	 * Create a linked case-insensitive Map if possible: This implementation
	 * always returns a {@link org.springframework.util.LinkedCaseInsensitiveMap}.
	 * @param initialCapacity the initial capacity of the Map
	 * @return the new Map instance
	 * @deprecated as of Spring 3.0, for usage on JDK 1.5 or higher
	 */
	@Deprecated
	public static Map createLinkedCaseInsensitiveMapIfPossible(int initialCapacity) {
		return new LinkedCaseInsensitiveMap(initialCapacity);
	}

	/**
	 * Create an identity Map if possible: This implementation always
	 * creates a {@link java.util.IdentityHashMap}, since Spring 2.5
	 * requires JDK 1.4 anyway.
	 * @param initialCapacity the initial capacity of the Map
	 * @return the new Map instance
	 * @deprecated as of Spring 2.5, for usage on JDK 1.4 or higher
	 */
	@Deprecated
	public static Map createIdentityMapIfPossible(int initialCapacity) {
		return new IdentityHashMap(initialCapacity);
	}

	/**
	 * Create a concurrent Map if possible: This implementation always
	 * creates a {@link java.util.concurrent.ConcurrentHashMap}, since Spring 3.0
	 * requires JDK 1.5 anyway.
	 * @param initialCapacity the initial capacity of the Map
	 * @return the new Map instance
	 * @deprecated as of Spring 3.0, for usage on JDK 1.5 or higher
	 */
	@Deprecated
	public static Map createConcurrentMapIfPossible(int initialCapacity) {
		return new ConcurrentHashMap(initialCapacity);
	}

	/**
	 * Create a concurrent Map with a dedicated {@link ConcurrentMap} interface:
	 * This implementation always creates a {@link java.util.concurrent.ConcurrentHashMap},
	 * since Spring 3.0 requires JDK 1.5 anyway.
	 * @param initialCapacity the initial capacity of the Map
	 * @return the new ConcurrentMap instance
	 * @deprecated as of Spring 3.0, for usage on JDK 1.5 or higher
	 */
	@Deprecated
	public static ConcurrentMap createConcurrentMap(int initialCapacity) {
		return new JdkConcurrentHashMap(initialCapacity);
	}

	/**
	 * Determine whether the given collection type is an approximable type,
	 * i.e. a type that {@link #createApproximateCollection} can approximate.
	 * @param collectionType the collection type to check
	 * @return <code>true</code> if the type is approximable,
	 * <code>false</code> if it is not
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
	public static Collection createApproximateCollection(Object collection, int initialCapacity) {
		if (collection instanceof LinkedList) {
			return new LinkedList();
		}
		else if (collection instanceof List) {
			return new ArrayList(initialCapacity);
		}
		else if (collection instanceof SortedSet) {
			return new TreeSet(((SortedSet) collection).comparator());
		}
		else {
			return new LinkedHashSet(initialCapacity);
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
	public static Collection createCollection(Class<?> collectionType, int initialCapacity) {
		if (collectionType.isInterface()) {
			if (List.class.equals(collectionType)) {
				return new ArrayList(initialCapacity);
			}
			else if (SortedSet.class.equals(collectionType) || collectionType.equals(navigableSetClass)) {
				return new TreeSet();
			}
			else if (Set.class.equals(collectionType) || Collection.class.equals(collectionType)) {
				return new LinkedHashSet(initialCapacity);
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
				return (Collection) collectionType.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Could not instantiate Collection type: " + collectionType.getName());
			}
		}
	}

	/**
	 * Determine whether the given map type is an approximable type,
	 * i.e. a type that {@link #createApproximateMap} can approximate.
	 * @param mapType the map type to check
	 * @return <code>true</code> if the type is approximable,
	 * <code>false</code> if it is not
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
	public static Map createApproximateMap(Object map, int initialCapacity) {
		if (map instanceof SortedMap) {
			return new TreeMap(((SortedMap) map).comparator());
		}
		else {
			return new LinkedHashMap(initialCapacity);
		}
	}

	/**
	 * Create the most approximate map for the given map.
	 * <p>Creates a TreeMap or linked Map for a SortedMap or Map, respectively.
	 * @param collectionType the desired type of the target Map
	 * @param initialCapacity the initial capacity
	 * @return the new Map instance
	 * @see java.util.TreeMap
	 * @see java.util.LinkedHashMap
	 */
	public static Map createMap(Class<?> mapType, int initialCapacity) {
		if (mapType.isInterface()) {
			if (Map.class.equals(mapType)) {
				return new LinkedHashMap(initialCapacity);
			}
			else if (SortedMap.class.equals(mapType) || mapType.equals(navigableMapClass)) {
				return new TreeMap();
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
				return (Map) mapType.newInstance();
			}
			catch (Exception ex) {
				throw new IllegalArgumentException("Could not instantiate Map type: " + mapType.getName());
			}
		}
	}


	/**
	 * ConcurrentMap adapter for the JDK ConcurrentHashMap class.
	 */
	@Deprecated
	private static class JdkConcurrentHashMap extends ConcurrentHashMap implements ConcurrentMap {

		private JdkConcurrentHashMap(int initialCapacity) {
			super(initialCapacity);
		}
	}

}
