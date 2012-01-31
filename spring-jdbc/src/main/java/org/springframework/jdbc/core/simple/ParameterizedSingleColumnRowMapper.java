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

package org.springframework.jdbc.core.simple;

import org.springframework.jdbc.core.SingleColumnRowMapper;

/**
 * {@link ParameterizedRowMapper} implementation that converts a single column
 * into a single result value per row. Expects to operate on a
 * <code>java.sql.ResultSet</code> that just contains a single column.
 *
 * <p>The type of the result value for each row can be specified. The value
 * for the single column will be extracted from the <code>ResultSet</code>
 * and converted into the specified target type.
 *
 * <p>Uses Java 5 covariant return types to override the return type of the
 * {@link #mapRow} method to be the type parameter <code>T</code>.
 *
 * @author Juergen Hoeller
 * @since 2.5.2
 */
public class ParameterizedSingleColumnRowMapper<T> extends SingleColumnRowMapper<T>
		implements ParameterizedRowMapper<T> {

	/**
	 * Static factory method to create a new ParameterizedSingleColumnRowMapper
	 * (with the required type specified only once).
	 * @param requiredType the type that each result object is expected to match
	 */
	public static <T> ParameterizedSingleColumnRowMapper<T> newInstance(Class<T> requiredType) {
		ParameterizedSingleColumnRowMapper<T> rm = new ParameterizedSingleColumnRowMapper<T>();
		rm.setRequiredType(requiredType);
		return rm;
	}

}
