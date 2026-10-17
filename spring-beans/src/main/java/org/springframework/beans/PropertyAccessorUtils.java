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

package org.springframework.beans;

import org.jspecify.annotations.Nullable;

/**
 * Utility methods for classes that perform bean property access
 * according to the {@link PropertyAccessor} interface.
 *
 * @author Juergen Hoeller
 * @author Sam Brannen
 * @since 1.2.6
 * @deprecated as of 7.1, in favor of {@link PropertyPath}. Use
 * {@link PropertyPath#parse(String)} and {@link PropertyPath#canonicalName()}
 * in place of {@link #canonicalPropertyName} and {@link #canonicalPropertyNames},
 * and match a registered path against a property by comparing canonical names
 * in place of {@link #matchesProperty}. There is no direct replacement for
 * {@link #getPropertyName}, {@link #isNestedOrIndexedProperty},
 * {@link #getFirstNestedPropertySeparatorIndex}, or
 * {@link #getLastNestedPropertySeparatorIndex}, which operate on raw,
 * unparsed property path text.
 */
@Deprecated(since = "7.1", forRemoval = true)
public abstract class PropertyAccessorUtils {

	/**
	 * Return the actual property name for the given property path.
	 * @param propertyPath the property path to determine the property name
	 * for (can include property keys, for example for specifying a map entry)
	 * @return the actual property name, without any key elements
	 */
	public static String getPropertyName(String propertyPath) {
		int separatorIndex = (propertyPath.endsWith(PropertyAccessor.PROPERTY_KEY_SUFFIX) ?
				propertyPath.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) : -1);
		return (separatorIndex != -1 ? propertyPath.substring(0, separatorIndex) : propertyPath);
	}

	/**
	 * Check whether the given property path indicates an indexed or nested property.
	 * @param propertyPath the property path to check
	 * @return whether the path indicates an indexed or nested property
	 */
	public static boolean isNestedOrIndexedProperty(@Nullable String propertyPath) {
		if (propertyPath == null) {
			return false;
		}
		for (int i = 0; i < propertyPath.length(); i++) {
			char ch = propertyPath.charAt(i);
			if (ch == PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR ||
					ch == PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Determine the first nested property separator in the
	 * given property path, ignoring dots in keys (like "map[my.key]").
	 * @param propertyPath the property path to check
	 * @return the index of the nested property separator, or -1 if none
	 */
	public static int getFirstNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, false);
	}

	/**
	 * Determine the first nested property separator in the
	 * given property path, ignoring dots in keys (like "map[my.key]").
	 * @param propertyPath the property path to check
	 * @return the index of the nested property separator, or -1 if none
	 */
	public static int getLastNestedPropertySeparatorIndex(String propertyPath) {
		return getNestedPropertySeparatorIndex(propertyPath, true);
	}

	/**
	 * Determine the first (or last) nested property separator in the
	 * given property path, ignoring dots in keys (like "map[my.key]").
	 * <p>Also tracks bracket nesting depth so that keys containing an unbalanced
	 * number of {@code [} or {@code ]} characters do not interfere with the
	 * detection of separators that precede or follow the key.
	 * @param propertyPath the property path to check
	 * @param last whether to return the last separator rather than the first
	 * @return the index of the nested property separator, or -1 if none
	 */
	private static int getNestedPropertySeparatorIndex(String propertyPath, boolean last) {
		int depth = 0;
		int length = propertyPath.length();
		int i = (last ? length - 1 : 0);
		while (last ? i >= 0 : i < length) {
			switch (propertyPath.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR -> depth += (last ? -1 : 1);
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR -> depth += (last ? 1 : -1);
				case PropertyAccessor.NESTED_PROPERTY_SEPARATOR_CHAR -> {
					if (depth <= 0) {
						return i;
					}
				}
			}
			if (last) {
				i--;
			}
			else {
				i++;
			}
		}
		return -1;
	}

	/**
	 * Determine whether the given registered path matches the given property path,
	 * either indicating the property itself or an indexed element of the property.
	 * @param propertyPath the property path (typically without index)
	 * @param registeredPath the registered path (potentially with index)
	 * @return whether the paths match
	 */
	public static boolean matchesProperty(String registeredPath, String propertyPath) {
		// canonicalPropertyName, not PropertyPath.parse directly: this method's
		// long-standing contract is non-throwing, even for a malformed path.
		String registered = canonicalPropertyName(registeredPath);
		String property = canonicalPropertyName(propertyPath);
		if (!registered.startsWith(property)) {
			return false;
		}
		if (registered.length() == property.length()) {
			return true;
		}
		if (registered.charAt(property.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
			return false;
		}
		return (registered.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, property.length() + 1) ==
				registered.length() - 1);
	}

	/**
	 * Determine the canonical name for the given property path.
	 * Removes surrounding quotes from map keys:<br>
	 * {@code map['key']} &rarr; {@code map[key]}<br>
	 * {@code map["key"]} &rarr; {@code map[key]}
	 * @param propertyName the bean property path
	 * @return the canonical representation of the property path
	 */
	public static String canonicalPropertyName(@Nullable String propertyName) {
		return PropertyPath.canonicalNameOrOriginal(propertyName);
	}

	/**
	 * Determine the canonical names for the given property paths.
	 * @param propertyNames the bean property paths (as array)
	 * @return the canonical representation of the property paths
	 * (as array of the same size)
	 * @see #canonicalPropertyName(String)
	 */
	public static String @Nullable [] canonicalPropertyNames(String @Nullable [] propertyNames) {
		if (propertyNames == null) {
			return null;
		}
		String[] result = new String[propertyNames.length];
		for (int i = 0; i < propertyNames.length; i++) {
			result[i] = canonicalPropertyName(propertyNames[i]);
		}
		return result;
	}

}
