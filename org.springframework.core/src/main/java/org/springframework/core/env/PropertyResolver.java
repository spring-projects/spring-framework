/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.core.env;

import java.util.Properties;


/**
 * Interface for resolving properties against any underlying source.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Environment#getPropertyResolver()
 */
public interface PropertyResolver {

	/**
	 * @return whether the given property key is available for resolution
	 */
	boolean containsProperty(String key);

	/**
	 * @return the property value associated with the given key
	 * @see #getProperty(String, Class)
	 */
	String getProperty(String key);

	/**
	 * @return the property value associated with the given key, or {@code null}
	 * if the key cannot be resolved
	 */
	<T> T getProperty(String key, Class<T> targetType);

	/**
	 * @return the property value associated with the given key, converted to the given
	 * targetType (never {@code null})
	 * @throws IllegalStateException if the key cannot be resolved
	 * @see #getRequiredProperty(String, Class)
	 */
	String getRequiredProperty(String key) throws IllegalStateException;

	/**
	 * @return the property value associated with the given key, converted to the given
	 * targetType (never {@code null})
	 * @throws IllegalStateException if the given key cannot be resolved
	 */
	<T> T getRequiredProperty(String key, Class<T> targetType) throws IllegalStateException;

	/**
	 * @return the number of unique properties keys resolvable
	 */
	int getPropertyCount();

	/**
	 * @return all property key/value pairs as a {@link java.util.Properties} instance
	 */
	Properties asProperties();

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value are ignored and passed through unchanged.
	 * @param text the String to resolve
	 * @return the resolved String (never {@code null})
	 * @throws IllegalArgumentException if given text is {@code null}
	 * @see #resolveRequiredPlaceholders
	 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String)
	 */
	String resolvePlaceholders(String text);

	/**
	 * Resolve ${...} placeholders in the given text, replacing them with corresponding
	 * property values as resolved by {@link #getProperty}. Unresolvable placeholders with
	 * no default value will cause an IllegalArgumentException to be thrown.
	 * @return the resolved String (never {@code null})
	 * @throws IllegalArgumentException if given text is {@code null}
	 * @throws IllegalArgumentException if any placeholders are unresolvable
	 * @see org.springframework.util.SystemPropertyUtils#resolvePlaceholders(String, boolean)
	 */
	String resolveRequiredPlaceholders(String path) throws IllegalArgumentException;

}
