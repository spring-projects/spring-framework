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
 */
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
		if (!registeredPath.startsWith(propertyPath)) {
			return false;
		}
		if (registeredPath.length() == propertyPath.length()) {
			return true;
		}
		if (registeredPath.charAt(propertyPath.length()) != PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR) {
			return false;
		}
		return (registeredPath.indexOf(PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR, propertyPath.length() + 1) ==
				registeredPath.length() - 1);
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
		if (propertyName == null) {
			return "";
		}

		StringBuilder sb = new StringBuilder(propertyName);
		int searchIndex = 0;
		while (searchIndex != -1) {
			int keyStart = sb.indexOf(PropertyAccessor.PROPERTY_KEY_PREFIX, searchIndex);
			searchIndex = -1;
			if (keyStart != -1) {
				int keyEnd = getPropertyNameKeyEnd(sb, keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length());
				if (keyEnd != -1) {
					String key = sb.substring(keyStart + PropertyAccessor.PROPERTY_KEY_PREFIX.length(), keyEnd);
					if (key.length() > 1 && ((key.startsWith("'") && key.endsWith("'")) ||
							(key.startsWith("\"") && key.endsWith("\"")))) {
						sb.delete(keyStart + 1, keyStart + 2);
						sb.delete(keyEnd - 2, keyEnd - 1);
						keyEnd = keyEnd - 2;
					}
					searchIndex = keyEnd + PropertyAccessor.PROPERTY_KEY_SUFFIX.length();
				}
			}
		}
		return sb.toString();
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

	/**
	 * Determine the end of the {@code [key]} expression that starts at the
	 * given {@code startIndex}, tracking bracket nesting depth so that any
	 * {@code [} or {@code ]} characters contained within the key itself (for
	 * example, in a quoted key such as {@code "['key[0]']"}) do not interfere
	 * with the detection of the actual closing bracket.
	 * @param propertyName the property name (or path) to search
	 * @param startIndex the index to start searching from (immediately after
	 * the opening {@code [})
	 * @return the index of the matching closing {@code ]}, or -1 if none
	 * @since 7.1
	 */
	static int getPropertyNameKeyEnd(CharSequence propertyName, int startIndex) {
		int unclosedPrefixes = 0;
		int length = propertyName.length();
		for (int i = startIndex; i < length; i++) {
			switch (propertyName.charAt(i)) {
				case PropertyAccessor.PROPERTY_KEY_PREFIX_CHAR -> {
					// The property name contains opening prefix(es)...
					unclosedPrefixes++;
				}
				case PropertyAccessor.PROPERTY_KEY_SUFFIX_CHAR -> {
					if (unclosedPrefixes == 0) {
						// No unclosed prefix(es) in the property name (left) ->
						// this is the suffix we are looking for.
						return i;
					}
					else {
						// This suffix does not close the initial prefix but rather
						// just one that occurred within the property name.
						unclosedPrefixes--;
					}
				}
			}
		}
		return -1;
	}

}
