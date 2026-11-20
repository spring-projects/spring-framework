/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.r2dbc.core;

import io.r2dbc.spi.Parameter;

/**
 * Interface that defines common functionality for objects
 * that can offer parameter values for named bind parameters,
 * serving as argument for {@link NamedParameterExpander} operations.
 *
 * <p>This interface allows for the specification of the type in
 * addition to parameter values. All parameter values and types are
 * identified by specifying the name of the parameter.
 *
 * <p>Intended to wrap various implementations like a {@link java.util.Map}
 * with a consistent interface.
 *
 * @author Mark Paluch
 * @since 5.3
 * @see MapBindParameterSource
 */
interface BindParameterSource {

	/**
	 * Determine whether there is a value for the specified named parameter.
	 * @param paramName the name of the parameter
	 * @return {@code true} if there is a value defined; {@code false} otherwise
	 */
	boolean hasValue(String paramName);

	/**
	 * Return the parameter for the requested named parameter.
	 * @param paramName the name of the parameter
	 * @return the specified parameter
	 * @throws IllegalArgumentException if there is no value
	 * for the requested parameter
	 */
	Parameter getValue(String paramName) throws IllegalArgumentException;

	/**
	 * Return the parameter names of the underlying parameter source.
	 * @return an iterator over the parameter names
	 */
	Iterable<String> getParameterNames();

}
