/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 *     });
 * </pre>
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.jdbc.support.lob.LobCreator
 * @deprecated as of 6.2, in favor of {@link SqlBinaryValue} and {@link SqlCharacterValue}
 */
@Deprecated(since = "6.2")
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
		try (LobCreator lobCreator = this.lobHandler.getLobCreator()) {
			setValues(ps, lobCreator);
			return ps.executeUpdate();
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
