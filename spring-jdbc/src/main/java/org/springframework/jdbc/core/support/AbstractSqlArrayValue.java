/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.CleanupFailureDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.support.SqlValue;


/**
 * Abstract base class for array values.
 * <p>
 * Individual subclasses classes can define how arrays are created and
 * therefore use vendor extensions.
 *
 * @author Philippe Marschall
 * @since 5.1
 */
public abstract class AbstractSqlArrayValue implements SqlValue {

	private Array array;

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.support.SqlValue#setValue(java.sql.PreparedStatement, int)
	 */
	@Override
	public void setValue(PreparedStatement ps, int paramIndex) throws SQLException {
		if (array != null) {
			throw new InvalidDataAccessApiUsageException("Value bound more than once");
		}
		array = this.createArray(ps.getConnection());
		ps.setArray(paramIndex, array);
	}

	/**
	 * Create the Array on the given Connection.
	 * @param conn the Connection to work on
	 * @return the Array
	 * @throws SQLException if a SQLException is encountered while creating the array
	 */
	protected abstract Array createArray(Connection conn) throws SQLException;

	/* (non-Javadoc)
	 * @see org.springframework.jdbc.support.SqlValue#cleanup()
	 */
	@Override
	public void cleanup() {
		if (array == null) {
			// #cleanup may be called twice in case of exceptions
			// avoid calling #free twice
			return;
		}
		try {
			array.free();
			array = null;
		} catch (SQLException e) {
			throw new CleanupFailureDataAccessException("could not free array", e);
		}
	}

}
