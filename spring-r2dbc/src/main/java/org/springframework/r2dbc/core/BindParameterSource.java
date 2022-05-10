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
