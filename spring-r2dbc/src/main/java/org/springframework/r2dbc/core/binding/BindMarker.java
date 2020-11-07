/*
 * Copyright 2002-2020 the original author or authors.
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

import io.r2dbc.spi.Statement;

/**
 * A bind marker represents a single bindable parameter within a query.
 * Bind markers are dialect-specific and provide a
 * {@link #getPlaceholder() placeholder} that is used in the actual query.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see Statement#bind
 * @see BindMarkers
 * @see BindMarkersFactory
 */
public interface BindMarker {

	/**
	 * Return the database-specific placeholder for a given substitution.
	 */
	String getPlaceholder();

	/**
	 * Bind the given {@code value} to the {@link Statement} using the underlying binding strategy.
	 * @param bindTarget the target to bind the value to
	 * @param value the actual value (must not be {@code null};
	 * use {@link #bindNull(BindTarget, Class)} for {@code null} values)
	 * @see Statement#bind
	 */
	void bind(BindTarget bindTarget, Object value);

	/**
	 * Bind a {@code null} value to the {@link Statement} using the underlying binding strategy.
	 * @param bindTarget the target to bind the value to
	 * @param valueType the value type (must not be {@code null})
	 * @see Statement#bindNull
	 */
	void bindNull(BindTarget bindTarget, Class<?> valueType);

}
