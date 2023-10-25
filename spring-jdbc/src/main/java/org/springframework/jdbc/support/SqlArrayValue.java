/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jdbc.support;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Common {@link SqlValue} implementation for JDBC {@link Array} creation
 * based on the JDBC 4 {@link java.sql.Connection#createArrayOf} method.
 *
 * <p>Also serves as a template for custom {@link SqlValue} implementations
 * with cleanup demand.
 *
 * @author Juergen Hoeller
 * @author Philippe Marschall
 * @since 6.1
 */
public class SqlArrayValue implements SqlValue {

	private final String typeName;

	private final Object[] elements;

	@Nullable
	private Array array;


	/**
	 * Create a new {@code SqlArrayValue} for the given type name and elements.
	 * @param typeName the SQL name of the type the elements of the array map to
	 * @param elements the elements to populate the {@code Array} object with
	 * @see java.sql.Connection#createArrayOf
	 */
	public SqlArrayValue(String typeName, Object... elements) {
		Assert.notNull(typeName, "Type name must not be null");
		Assert.notNull(elements, "Elements array must not be null");
		this.typeName = typeName;
		this.elements = elements;
	}


	@Override
	public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
		this.array = ps.getConnection().createArrayOf(this.typeName, this.elements);
		ps.setArray(paramIndex, this.array);
	}

	@Override
	public void cleanup() {
		if (this.array != null) {
			try {
				this.array.free();
			}
			catch (SQLException ex) {
				throw new DataAccessResourceFailureException("Could not free Array object", ex);
			}
		}
	}

}
