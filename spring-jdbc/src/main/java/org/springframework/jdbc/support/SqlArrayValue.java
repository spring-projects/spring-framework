/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.support;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.DataAccessResourceFailureException;
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

	private final @Nullable Object[] elements;

	private @Nullable Array array;


	/**
	 * Create a new {@code SqlArrayValue} for the given type name and elements.
	 * @param typeName the SQL name of the type the elements of the array map to
	 * @param elements the elements to populate the {@code Array} object with
	 * @see java.sql.Connection#createArrayOf
	 */
	public SqlArrayValue(String typeName, @Nullable Object... elements) {
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
