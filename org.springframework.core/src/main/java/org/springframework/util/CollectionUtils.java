/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.Arrays;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Miscellaneous collection utility methods.
 * Mainly for internal use within the framework.
 *
 * @author Juergen Hoeller
 * @author Rob Harrop
 * @since 1.1.3
 */
public abstract class CollectionUtils {

	/**
	 * Return <code>true</code> if the supplied Collection is <code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * @param collection the Collection to check
	 * @return whether the given Collection is empty
	 */
	public static boolean isEmpty(Collection collection) {
		return (collection == null || collection.isEmpty());
	}

	/**
	 * Return <code>true</code> if the supplied Map is <code>null</code>
	 * or empty. Otherwise, return <code>false</code>.
	 * @param map the Map to check
	 * @return whether the given Map is empty
	 */
	public static boolean isEmpty(Map map) {
		return (map == null || map.isEmpty());
	}

	/**
	 * Convert the supplied array into a List. A primitive array gets
	 * converted into a List of the appropriate wrapper type.
	 * <p>A <code>null</code> source value will be converted to an
	 * empty List.
	 * @param source the (potentially primitive) array
	 * @return the converted List result
	 * @see ObjectUtils#toObjectArray(Object)
	 */
	public static List arrayToList(Object source) {
		return Arrays.asList(ObjectUtils.toObjectArray(source));
	}

	/**
	 * Merge the given array into the given Collection.
	 * @param array the array to merge (may be <code>null</code>)
	 * @param collection the target Collection to merge the array into
	 */
	public static void mergeArrayIntoCollection(Object array, Collection collection) {
		if (collection == null) {
			throw new IllegalArgumentException("Collection must not be null");
		}
		Object[] arr = ObjectUtils.toObjectArray(array);
		for (int i = 0; i < arr.length; i++) {
			collection.add(arr[i]);
		}
	}

	/**
	 * Merge the given Properties instance into the given Map,
	 * copying all properties (key-value pairs) over.
	 * <p>Uses <code>Properties.propertyNames()</code> to even catch
	 * default properties linked into the original Properties instance.
	 * @param props the Properties instance to merge (may be <code>null</code>)
	 * @param map the target Map to merge the properties into
	 */
	public static void mergePropertiesIntoMap(Properties props, Map map) {
		if (map == null) {
			throw new IllegalArgumentException("Map must not be null");
		}
		if (props != null) {
			for (Enumeration en = props.propertyNames(); en.hasMoreElements();) {
				String key = (String) en.nextElement();
				map.put(key, props.getProperty(key));
			}
		}
	}


	/**
	 * Check whether the given Iterator contains the given element.
	 * @param iterator the Iterator to check
	 * @param element the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean contains(Iterator iterator, Object element) {
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
	 * Check whether the given Enumeration contains the given element.
	 * @param enumeration the Enumeration to check
	 * @param element the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean contains(Enumeration enumeration, Object element) {
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
	 * Check whether the given Collection contains the given element instance.
	 * <p>Enforces the given instance to be present, rather than returning
	 * <code>true</code> for an equal element as well.
	 * @param collection the Collection to check
	 * @param element the element to look for
	 * @return <code>true</code> if found, <code>false</code> else
	 */
	public static boolean containsInstance(Collection collection, Object element) {
		if (collection != null) {
			for (Iterator it = collection.iterator(); it.hasNext();) {
				Object candidate = it.next();
				if (candidate == element) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Return <code>true</code> if any element in '<code>candidates</code>' is
	 * contained in '<code>source</code>'; otherwise returns <code>false</code>.
	 * @param source the source Collection
	 * @param candidates the candidates to search for
	 * @return whether any of the candidates has been found
	 */
	public static boolean containsAny(Collection source, Collection candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return false;
		}
		for (Iterator it = candidates.iterator(); it.hasNext();) {
			if (source.contains(it.next())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Return the first element in '<code>candidates</code>' that is contained in
	 * '<code>source</code>'. If no element in '<code>candidates</code>' is present in
	 * '<code>source</code>' returns <code>null</code>. Iteration order is
	 * {@link Collection} implementation specific.
	 * @param source the source Collection
	 * @param candidates the candidates to search for
	 * @return the first present object, or <code>null</code> if not found
	 */
	public static Object findFirstMatch(Collection source, Collection candidates) {
		if (isEmpty(source) || isEmpty(candidates)) {
			return null;
		}
		for (Iterator it = candidates.iterator(); it.hasNext();) {
			Object candidate = it.next();
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
	 * or <code>null</code> if none or more than one such value found
	 */
	public static Object findValueOfType(Collection collection, Class type) {
		if (isEmpty(collection)) {
			return null;
		}
		Object value = null;
		for (Iterator it = collection.iterator(); it.hasNext();) {
			Object obj = it.next();
			if (type == null || type.isInstance(obj)) {
				if (value != null) {
					// More than one value found... no clear single value.
					return null;
				}
				value = obj;
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
	 * or <code>null</code> if none or more than one such value found
	 */
	public static Object findValueOfType(Collection collection, Class[] types) {
		if (isEmpty(collection) || ObjectUtils.isEmpty(types)) {
			return null;
		}
		for (int i = 0; i < types.length; i++) {
			Object value = findValueOfType(collection, types[i]);
			if (value != null) {
				return value;
			}
		}
		return null;
	}

	/**
	 * Determine whether the given Collection only contains a single unique object.
	 * @param collection the Collection to check
	 * @return <code>true</code> if the collection contains a single reference or
	 * multiple references to the same instance, <code>false</code> else
	 */
	public static boolean hasUniqueObject(Collection collection) {
		if (isEmpty(collection)) {
			return false;
		}
		boolean hasCandidate = false;
		Object candidate = null;
		for (Iterator it = collection.iterator(); it.hasNext();) {
			Object elem = it.next();
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

}
