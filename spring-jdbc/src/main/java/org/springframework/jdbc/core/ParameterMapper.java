/*
 * Copyright 2002-2016 the original author or authors.
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
	 * @param con JDBC connection. This is useful (and the purpose of this interface)
	 * if we need to do something RDBMS-specific with a proprietary Connection
	 * implementation class. This class conceals such proprietary details. However,
	 * it is best to avoid using such proprietary RDBMS features if possible.
	 * @throws SQLException if a SQLException is encountered setting
	 * parameter values (that is, there's no need to catch SQLException)
	 * @return Map of input parameters, keyed by name (never {@code null})
	 */
	Map<String, ?> createMap(Connection con) throws SQLException;

}
