/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jca.cci.core;

import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Interface that specifies a basic set of CCI operations on an EIS.
 * Implemented by CciTemplate. Not often used, but a useful option
 * to enhance testability, as it can easily be mocked or stubbed.
 *
 * <p>Alternatively, the standard CCI infrastructure can be mocked.
 * However, mocking this interface constitutes significantly less work.
 *
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate
 */
public interface CciOperations {

	/**
	 * Execute a request on an EIS with CCI, implemented as callback action
	 * working on a CCI Connection. This allows for implementing arbitrary
	 * data access operations, within Spring's managed CCI environment:
	 * that is, participating in Spring-managed transactions and converting
	 * JCA ResourceExceptions into Spring's DataAccessException hierarchy.
	 * <p>The callback action can return a result object, for example a
	 * domain object or a collection of domain objects.
	 * @param action the callback object that specifies the action
	 * @return the result object returned by the action, if any
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(ConnectionCallback<T> action) throws DataAccessException;

	/**
	 * Execute a request on an EIS with CCI, implemented as callback action
	 * working on a CCI Interaction. This allows for implementing arbitrary
	 * data access operations on a single Interaction, within Spring's managed
	 * CCI environment: that is, participating in Spring-managed transactions
	 * and converting JCA ResourceExceptions into Spring's DataAccessException
	 * hierarchy.
	 * <p>The callback action can return a result object, for example a
	 * domain object or a collection of domain objects.
	 * @param action the callback object that specifies the action
	 * @return the result object returned by the action, if any
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(InteractionCallback<T> action) throws DataAccessException;

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputRecord the input record
	 * @return the output record
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	Record execute(InteractionSpec spec, Record inputRecord) throws DataAccessException;

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputRecord the input record
	 * @param outputRecord the output record
	 * @throws DataAccessException if there is any problem
	 */
	void execute(InteractionSpec spec, Record inputRecord, Record outputRecord) throws DataAccessException;

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputCreator object that creates the input record to use
	 * @return the output record
	 * @throws DataAccessException if there is any problem
	 */
	Record execute(InteractionSpec spec, RecordCreator inputCreator) throws DataAccessException;

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputRecord the input record
	 * @param outputExtractor object to convert the output record to a result object
	 * @return the output data extracted with the RecordExtractor object
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(InteractionSpec spec, Record inputRecord, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

	/**
	 * Execute the specified interaction on an EIS with CCI.
	 * @param spec the CCI InteractionSpec instance that defines
	 * the interaction (connector-specific)
	 * @param inputCreator object that creates the input record to use
	 * @param outputExtractor object to convert the output record to a result object
	 * @return the output data extracted with the RecordExtractor object
	 * @throws DataAccessException if there is any problem
	 */
	@Nullable
	<T> T execute(InteractionSpec spec, RecordCreator inputCreator, RecordExtractor<T> outputExtractor)
			throws DataAccessException;

}
