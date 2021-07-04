/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jdbc.core.support;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.support.lob.LobCreator;
import org.springframework.jdbc.support.lob.LobHandler;
import org.springframework.util.Assert;

/**
 * Abstract {@link PreparedStatementCallback} implementation that manages a {@link LobCreator}.
 * Typically used as inner class, with access to surrounding method arguments.
 *
 * <p>Delegates to the {@code setValues} template method for setting values
 * on the PreparedStatement, using a given LobCreator for BLOB/CLOB arguments.
 *
 * <p>A usage example with {@link org.springframework.jdbc.core.JdbcTemplate}:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.execute(
 *     "INSERT INTO imagedb (image_name, content, description) VALUES (?, ?, ?)",
 *     new AbstractLobCreatingPreparedStatementCallback(lobHandler) {
 *       protected void setValues(PreparedStatement ps, LobCreator lobCreator) throws SQLException {
 *         ps.setString(1, name);
 *         lobCreator.setBlobAsBinaryStream(ps, 2, contentStream, contentLength);
 *         lobCreator.setClobAsString(ps, 3, description);
 *       }
 *     }
 * );</pre>
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.jdbc.support.lob.LobCreator
 */
public abstract class AbstractLobCreatingPreparedStatementCallback implements PreparedStatementCallback<Integer> {

	private final LobHandler lobHandler;


	/**
	 * Create a new AbstractLobCreatingPreparedStatementCallback for the
	 * given LobHandler.
	 * @param lobHandler the LobHandler to create LobCreators with
	 */
	public AbstractLobCreatingPreparedStatementCallback(LobHandler lobHandler) {
		Assert.notNull(lobHandler, "LobHandler must not be null");
		this.lobHandler = lobHandler;
	}


	@Override
	public final Integer doInPreparedStatement(PreparedStatement ps) throws SQLException, DataAccessException {
		LobCreator lobCreator = this.lobHandler.getLobCreator();
		try {
			setValues(ps, lobCreator);
			return ps.executeUpdate();
		}
		finally {
			lobCreator.close();
		}
	}

	/**
	 * Set values on the given PreparedStatement, using the given
	 * LobCreator for BLOB/CLOB arguments.
	 * @param ps the PreparedStatement to use
	 * @param lobCreator the LobCreator to use
	 * @throws SQLException if thrown by JDBC methods
	 * @throws DataAccessException in case of custom exceptions
	 */
	protected abstract void setValues(PreparedStatement ps, LobCreator lobCreator)
			throws SQLException, DataAccessException;

}
