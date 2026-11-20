/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.datasource;

import java.sql.Connection;

/**
 * Subinterface of {@link java.sql.Connection} to be implemented by
 * Connection proxies. Allows access to the underlying target Connection.
 *
 * <p>This interface can be checked when there is a need to cast to a
 * native JDBC Connection such as Oracle's OracleConnection. Alternatively,
 * all such connections also support JDBC 4.0's {@link Connection#unwrap}.
 *
 * @author Juergen Hoeller
 * @since 1.1
 * @see TransactionAwareDataSourceProxy
 * @see LazyConnectionDataSourceProxy
 * @see DataSourceUtils#getTargetConnection(java.sql.Connection)
 */
public interface ConnectionProxy extends Connection {

	/**
	 * Return the target Connection of this proxy.
	 * <p>This will typically be the native driver Connection
	 * or a wrapper from a connection pool.
	 * @return the underlying Connection (never {@code null})
	 */
	Connection getTargetConnection();

}
