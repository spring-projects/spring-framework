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

/**
 * Bind markers represent placeholders in SQL queries for substitution
 * for an actual parameter. Using bind markers allows creating safe queries
 * so query strings are not required to contain escaped values but rather
 * the driver encodes parameter in the appropriate representation.
 *
 * <p>{@link BindMarkers} is stateful and can be only used for a single binding
 * pass of one or more parameters. It maintains bind indexes/bind parameter names.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see BindMarker
 * @see BindMarkersFactory
 * @see io.r2dbc.spi.Statement#bind
 */
@FunctionalInterface
public interface BindMarkers {

	/**
	 * Create a new {@link BindMarker}.
	 */
	BindMarker next();

	/**
	 * Create a new {@link BindMarker} that accepts a {@code hint}.
	 * Implementations are allowed to consider/ignore/filter
	 * the name hint to create more expressive bind markers.
	 * @param hint an optional name hint that can be used as part of the bind marker
	 * @return a new {@link BindMarker}
	 */
	default BindMarker next(String hint) {
		return next();
	}

}
