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

package org.springframework.jdbc.core.support;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.LobRetrievalFailureException;
import org.springframework.jdbc.core.ResultSetExtractor;

/**
 * Abstract ResultSetExtractor implementation that assumes streaming of LOB data.
 * Typically used as inner class, with access to surrounding method arguments.
 *
 * <p>Delegates to the <code>streamData</code> template method for streaming LOB
 * content to some OutputStream, typically using a LobHandler. Converts an
 * IOException thrown during streaming to a LobRetrievalFailureException.
 *
 * <p>A usage example with JdbcTemplate:
 *
 * <pre class="code">JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);  // reusable object
 * final LobHandler lobHandler = new DefaultLobHandler();  // reusable object
 *
 * jdbcTemplate.query(
 *		 "SELECT content FROM imagedb WHERE image_name=?", new Object[] {name},
 *		 new AbstractLobStreamingResultSetExtractor() {
 *			 public void streamData(ResultSet rs) throws SQLException, IOException {
 *				 FileCopyUtils.copy(lobHandler.getBlobAsBinaryStream(rs, 1), contentStream);
 *			 }
 *		 }
 * );</pre>
 *
 * @author Juergen Hoeller
 * @since 1.0.2
 * @see org.springframework.jdbc.support.lob.LobHandler
 * @see org.springframework.jdbc.LobRetrievalFailureException
 */
public abstract class AbstractLobStreamingResultSetExtractor implements ResultSetExtractor {

	/**
	 * Delegates to handleNoRowFound, handleMultipleRowsFound and streamData,
	 * according to the ResultSet state. Converts an IOException thrown by
	 * streamData to a LobRetrievalFailureException.
	 * @see #handleNoRowFound
	 * @see #handleMultipleRowsFound
	 * @see #streamData
	 * @see org.springframework.jdbc.LobRetrievalFailureException
	 */
	public final Object extractData(ResultSet rs) throws SQLException, DataAccessException {
		if (!rs.next()) {
			handleNoRowFound();
		}
		else {
			try {
				streamData(rs);
				if (rs.next()) {
					handleMultipleRowsFound();
				}
			}
			catch (IOException ex) {
				throw new LobRetrievalFailureException("Couldn't stream LOB content", ex);
			}
		}
		return null;
	}

	/**
	 * Handle the case where the ResultSet does not contain a row.
	 * @throws DataAccessException a corresponding exception,
	 * by default an EmptyResultDataAccessException
	 * @see org.springframework.dao.EmptyResultDataAccessException
	 */
	protected void handleNoRowFound() throws DataAccessException {
		throw new EmptyResultDataAccessException(
				"LobStreamingResultSetExtractor did not find row in database", 1);
	}

	/**
	 * Handle the case where the ResultSet contains multiple rows.
	 * @throws DataAccessException a corresponding exception,
	 * by default an IncorrectResultSizeDataAccessException
	 * @see org.springframework.dao.IncorrectResultSizeDataAccessException
	 */
	protected void handleMultipleRowsFound() throws DataAccessException {
		throw new IncorrectResultSizeDataAccessException(
				"LobStreamingResultSetExtractor found multiple rows in database", 1);
	}

	/**
	 * Stream LOB content from the given ResultSet to some OutputStream.
	 * <p>Typically used as inner class, with access to surrounding method arguments
	 * and to a LobHandler instance variable of the surrounding class.
	 * @param rs the ResultSet to take the LOB content from
	 * @throws SQLException if thrown by JDBC methods
	 * @throws IOException if thrown by stream access methods
	 * @throws DataAccessException in case of custom exceptions
	 * @see org.springframework.jdbc.support.lob.LobHandler#getBlobAsBinaryStream
	 * @see org.springframework.util.FileCopyUtils
	 */
	protected abstract void streamData(ResultSet rs) throws SQLException, IOException, DataAccessException;

}
