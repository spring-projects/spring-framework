/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.jdbc.core.BeanPropertyRowMapper;

/**
 * {@link ParameterizedRowMapper} implementation that converts a row into a new instance
 * of the specified mapped target class. The mapped target class must be a top-level class
 * and it must have a default or no-arg constructor.
 *
 * <p>Uses Java 5 covariant return types to override the return type of the {@link #mapRow}
 * method to be the type parameter <code>T</code>.
 *
 * <p>Column values are mapped based on matching the column name as obtained from result set
 * metadata to public setters for the corresponding properties. The names are matched either
 * directly or by transforming a name separating the parts with underscores to the same name
 * using "camel" case.
 *
 * <p>Mapping is provided for fields in the target class for many common types, e.g.:
 * String, boolean, Boolean, byte, Byte, short, Short, int, Integer, long, Long,
 * float, Float, double, Double, BigDecimal, <code>java.util.Date</code>, etc.
 *
 * <p>The mapper can be configured to use the primitives default value when mapping null values
 * by setting the {@link #setPrimitivesDefaultedForNullValue 'primitivesDefaultedForNullValue'}
 * flag to 'true'.
 *
 * <p>To facilitate mapping between columns and fields that don't have matching names,
 * try using column aliases in the SQL statement like "select fname as first_name from customer".
 *
 * <p>Please note that this class is designed to provide convenience rather than high performance.
 * For best performance consider using a custom RowMapper.
 *
 * @author Thomas Risberg
 * @author Juergen Hoeller
 * @since 2.5
 * @see ParameterizedRowMapper
 */
public class ParameterizedBeanPropertyRowMapper<T> extends BeanPropertyRowMapper<T>
		implements ParameterizedRowMapper<T> {

	/**
	 * Static factory method to create a new ParameterizedBeanPropertyRowMapper
	 * (with the mapped class specified only once).
	 * @param mappedClass the class that each row should be mapped to
	 */
	public static <T> ParameterizedBeanPropertyRowMapper<T> newInstance(Class<T> mappedClass) {
		ParameterizedBeanPropertyRowMapper<T> newInstance = new ParameterizedBeanPropertyRowMapper<T>();
		newInstance.setMappedClass(mappedClass);
		return newInstance;
	}

}
