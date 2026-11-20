/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core.binding;

/**
 * Bind markers represent placeholders in SQL queries for substitution
 * for an actual parameter. Using bind markers allows creating safe queries
 * so query strings are not required to contain escaped values but rather
 * the driver encodes the parameter in the appropriate representation.
 *
 * <p>{@link BindMarkers} is stateful and can be only used for a single binding
 * pass of one or more parameters. It maintains bind indexes or bind parameter names.
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
	 * <p>Implementations are allowed to consider/ignore/filter
	 * the name hint to create more expressive bind markers.
	 * @param hint an optional name hint that can be used as part of the bind marker
	 * @return a new {@link BindMarker}
	 */
	default BindMarker next(String hint) {
		return next();
	}

}
