/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * Extension of the {@code javax.sql.DataSource} interface, to be
 * implemented by special DataSources that return JDBC Connections
 * in an unwrapped fashion.
 *
 * <p>Classes using this interface can query whether the Connection
 * should be closed after an operation. Spring's DataSourceUtils and
 * JdbcTemplate classes automatically perform such a check.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @see SingleConnectionDataSource#shouldClose
 * @see DataSourceUtils#releaseConnection
 * @see org.springframework.jdbc.core.JdbcTemplate
 */
public interface SmartDataSource extends DataSource {

	/**
	 * Should we close this Connection, obtained from this DataSource?
	 * <p>Code that uses Connections from a SmartDataSource should always
	 * perform a check via this method before invoking {@code close()}.
	 * <p>Note that the JdbcTemplate class in the 'jdbc.core' package takes care of
	 * releasing JDBC Connections, freeing application code of this responsibility.
	 * @param con the Connection to check
	 * @return whether the given Connection should be closed
	 * @see java.sql.Connection#close()
	 */
	boolean shouldClose(Connection con);

}
