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

package org.springframework.jca.cci.core;

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.cci.Record;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

/**
 * Callback interface for extracting a result object from a CCI Record instance.
 *
 * <p>Used for output object creation in CciTemplate. Alternatively, output
 * Records can also be returned to client code as-is. In case of a CCI ResultSet
 * as execution result, you will almost always want to implement a RecordExtractor,
 * to be able to read the ResultSet in a managed fashion, with the CCI Connection
 * still open while reading the ResultSet.
 *
 * <p>Implementations of this interface perform the actual work of extracting
 * results, but don't need to worry about exception handling. ResourceExceptions
 * will be caught and handled correctly by the CciTemplate class.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, Record, RecordExtractor)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 * @see javax.resource.cci.ResultSet
 */
@FunctionalInterface
public interface RecordExtractor<T> {

	/**
	 * Process the data in the given Record, creating a corresponding result object.
	 * @param record the Record to extract data from
	 * (possibly a CCI ResultSet)
	 * @return an arbitrary result object, or {@code null} if none
	 * (the extractor will typically be stateful in the latter case)
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @throws SQLException if thrown by a ResultSet method, to be auto-converted
	 * to a DataAccessException
	 * @throws DataAccessException in case of custom exceptions
	 * @see javax.resource.cci.ResultSet
	 */
	@Nullable
	T extractData(Record record) throws ResourceException, SQLException, DataAccessException;

}
