/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.r2dbc.core.binding;

import java.util.function.Function;

import org.springframework.util.Assert;

/**
 * This class creates new {@link BindMarkers} instances to bind
 * parameter to a specific {@link io.r2dbc.spi.Statement}.
 *
 * <p>Bind markers can be typically represented as placeholder and identifier.
 * Placeholders are used within the query to execute so the underlying database
 * system can substitute the placeholder with the actual value. Identifiers
 * are used in R2DBC drivers to bind a value to a bind marker. Identifiers are
 * typically a part of an entire bind marker when using indexed or named bind markers.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarkers
 * @see io.r2dbc.spi.Statement
 */
@FunctionalInterface
public interface BindMarkersFactory {

	/**
	 * Create a new {@link BindMarkers} instance.
	 */
	BindMarkers create();

	/**
	 * Return whether the {@link BindMarkersFactory} uses identifiable placeholders:
	 * {@code false} if multiple placeholders cannot be distinguished by just the
	 * {@link BindMarker#getPlaceholder() placeholder} identifier.
	 */
	default boolean identifiablePlaceholders() {
		return true;
	}


	// Static factory methods

	/**
	 * Create index-based {@link BindMarkers} using indexes to bind parameters.
	 * Allows customization of the bind marker placeholder {@code prefix} to
	 * represent the bind marker as placeholder within the query.
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param beginWith the first index to use
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(int, Class)
	 * @see io.r2dbc.spi.Statement#bind(int, Object)
	 */
	static BindMarkersFactory indexed(String prefix, int beginWith) {
		Assert.notNull(prefix, "Prefix must not be null");
		return () -> new IndexedBindMarkers(prefix, beginWith);
	}

	/**
	 * Create anonymous, index-based bind marker using a static placeholder.
	 * Instances are bound by the ordinal position ordered by the appearance
	 * of the placeholder. This implementation creates indexed bind markers
	 * using an anonymous placeholder that correlates with an index.
	 * @param placeholder parameter placeholder
	 * @return a {@link BindMarkersFactory} using {@code placeholder}
	 * @see io.r2dbc.spi.Statement#bindNull(int, Class)
	 * @see io.r2dbc.spi.Statement#bind(int, Object)
	 */
	static BindMarkersFactory anonymous(String placeholder) {
		Assert.hasText(placeholder, "Placeholder must not be empty");
		return new BindMarkersFactory() {
			@Override
			public BindMarkers create() {
				return new AnonymousBindMarkers(placeholder);
			}
			@Override
			public boolean identifiablePlaceholders() {
				return false;
			}
		};
	}

	/**
	 * Create named {@link BindMarkers} using identifiers to bind parameters.
	 * Named bind markers can support {@link BindMarkers#next(String) name hints}.
	 * If no {@link BindMarkers#next(String) hint} is given, named bind markers can
	 * use a counter or a random value source to generate unique bind markers.
	 * Allows customization of the bind marker placeholder {@code prefix} and
	 * {@code namePrefix} to represent the bind marker as placeholder within
	 * the query.
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param namePrefix prefix for bind marker name that is included in
	 * {@link BindMarker#getPlaceholder()} and the actual identifier
	 * @param maxLength maximal length of parameter names when using name hints
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(String, Class)
	 * @see io.r2dbc.spi.Statement#bind(String, Object)
	 */
	static BindMarkersFactory named(String prefix, String namePrefix, int maxLength) {
		return named(prefix, namePrefix, maxLength, Function.identity());
	}

	/**
	 * Create named {@link BindMarkers} using identifiers to bind parameters.
	 * Named bind markers support {@link BindMarkers#next(String) name hints}.
	 * If no {@link BindMarkers#next(String) hint} is given, named bind markers
	 * can use a counter or a random value source to generate unique bind markers.
	 * @param prefix bind parameter prefix that is included in
	 * {@link BindMarker#getPlaceholder()} but not the actual identifier
	 * @param namePrefix prefix for bind marker name that is included in
	 * {@link BindMarker#getPlaceholder()} and the actual identifier
	 * @param maxLength maximal length of parameter names when using name hints
	 * @param hintFilterFunction filter {@link Function} to consider
	 * database-specific limitations in bind marker/variable names such as ASCII chars only
	 * @return a {@link BindMarkersFactory} using {@code prefix} and {@code beginWith}
	 * @see io.r2dbc.spi.Statement#bindNull(String, Class)
	 * @see io.r2dbc.spi.Statement#bind(String, Object)
	 */
	static BindMarkersFactory named(String prefix, String namePrefix, int maxLength,
			Function<String, String> hintFilterFunction) {

		Assert.notNull(prefix, "Prefix must not be null");
		Assert.notNull(namePrefix, "Index prefix must not be null");
		Assert.notNull(hintFilterFunction, "Hint filter function must not be null");
		return () -> new NamedBindMarkers(prefix, namePrefix, maxLength, hintFilterFunction);
	}

}
