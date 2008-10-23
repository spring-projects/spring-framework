/*
 * Copyright 2002-2005 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.jdbc.core.RowMapper;

/**
 * Extension of the {@link org.springframework.jdbc.core.RowMapper} interface,
 * adding type parameterization. Uses Java 5 covariant return types to override
 * the return type of the {@link #mapRow} method to be the type parameter
 * <code>T</code>.
 *
 * @author Rob Harrop
 * @since 2.0
 * @see org.springframework.jdbc.core.simple.SimpleJdbcOperations
 */
public interface ParameterizedRowMapper<T> extends RowMapper {

	/**
	 * Implementations should return the object representation of
	 * the current row in the supplied {@link ResultSet}.
	 * @see org.springframework.jdbc.core.RowMapper#mapRow
	 */
	T mapRow(ResultSet rs, int rowNum) throws SQLException;

}
