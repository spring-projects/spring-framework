/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jca.cci.core;

import java.sql.SQLException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;

import org.springframework.dao.DataAccessException;

/**
 * Generic callback interface for code that operates on a CCI Connection.
 * Allows to execute any number of operations on a single Connection,
 * using any type and number of Interaction.
 *
 * <p>This is particularly useful for delegating to existing data access code
 * that expects a Connection to work on and throws ResourceException. For newly
 * written code, it is strongly recommended to use CciTemplate's more specific
 * {@code execute} variants.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate#execute(ConnectionCallback)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, javax.resource.cci.Record)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 */
public interface ConnectionCallback<T> {

	/**
	 * Gets called by {@code CciTemplate.execute} with an active CCI Connection.
	 * Does not need to care about activating or closing the Connection, or handling
	 * transactions.
	 * <p>If called without a thread-bound CCI transaction (initiated by
	 * CciLocalTransactionManager), the code will simply get executed on the CCI
	 * Connection with its transactional semantics. If CciTemplate is configured
	 * to use a JTA-aware ConnectionFactory, the CCI Connection and thus the callback
	 * code will be transactional if a JTA transaction is active.
	 * <p>Allows for returning a result object created within the callback, i.e.
	 * a domain object or a collection of domain objects. Note that there's special
	 * support for single step actions: see the {@code CciTemplate.execute}
	 * variants. A thrown RuntimeException is treated as application exception:
	 * it gets propagated to the caller of the template.
	 * @param connection active CCI Connection
	 * @param connectionFactory the CCI ConnectionFactory that the Connection was
	 * created with (gives access to RecordFactory and ResourceAdapterMetaData)
	 * @return a result object, or {@code null} if none
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @throws SQLException if thrown by a ResultSet method, to be auto-converted
	 * to a DataAccessException
	 * @throws DataAccessException in case of custom exceptions
	 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
	 * @see javax.resource.cci.ConnectionFactory#getMetaData()
	 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
	 */
	T doInConnection(Connection connection, ConnectionFactory connectionFactory)
			throws ResourceException, SQLException, DataAccessException;

}
