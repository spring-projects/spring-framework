/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.jdbc.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;

/**
 * Implement this interface when parameters need to be customized based
 * on the connection. We might need to do this to make use of proprietary
 * features, available only with a specific Connection type.
 *
 * @author Rod Johnson
 * @author Thomas Risberg
 * @see CallableStatementCreatorFactory#newCallableStatementCreator(ParameterMapper)
 * @see org.springframework.jdbc.object.StoredProcedure#execute(ParameterMapper)
 */
@FunctionalInterface
public interface ParameterMapper {

	/**
	 * Create a Map of input parameters, keyed by name.
	 * @param con a JDBC connection. This is useful (and the purpose of this interface)
	 * if we need to do something RDBMS-specific with a proprietary Connection
	 * implementation class. This class conceals such proprietary details. However,
	 * it is best to avoid using such proprietary RDBMS features if possible.
	 * @return a Map of input parameters, keyed by name (never {@code null})
	 * @throws SQLException if an SQLException is encountered setting
	 * parameter values (that is, there's no need to catch SQLException)
	 */
	Map<String, ?> createMap(Connection con) throws SQLException;

}
