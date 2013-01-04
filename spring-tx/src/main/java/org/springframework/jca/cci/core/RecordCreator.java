/*
 * Copyright 2002-2012 the original author or authors.
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

import javax.resource.ResourceException;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;

/**
 * Callback interface for creating a CCI Record instance,
 * usually based on the passed-in CCI RecordFactory.
 *
 * <p>Used for input Record creation in CciTemplate. Alternatively,
 * Record instances can be passed into CciTemplate's corresponding
 * {@code execute} methods directly, either instantiated manually
 * or created through CciTemplate's Record factory methods.
 *
 * <P>Also used for creating default output Records in CciTemplate.
 * This is useful when the JCA connector needs an explicit output Record
 * instance, but no output Records should be passed into CciTemplate's
 * {@code execute} methods.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator)
 * @see CciTemplate#execute(javax.resource.cci.InteractionSpec, RecordCreator, RecordExtractor)
 * @see CciTemplate#createIndexedRecord(String)
 * @see CciTemplate#createMappedRecord(String)
 * @see CciTemplate#setOutputRecordCreator(RecordCreator)
 */
public interface RecordCreator {

	/**
	 * Create a CCI Record instance, usually based on the passed-in CCI RecordFactory.
	 * <p>For use as <i>input</i> creator with CciTemplate's {@code execute} methods,
	 * this method should create a <i>populated</i> Record instance. For use as
	 * <i>output</i> Record creator, it should return an <i>empty</i> Record instance.
	 * @param recordFactory the CCI RecordFactory (never {@code null}, but not guaranteed to be
	 * supported by the connector: its create methods might throw NotSupportedException)
	 * @return the Record instance
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @throws DataAccessException in case of custom exceptions
	 */
	Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException;

}
