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

package org.springframework.jca.cci.connection;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.RecordFactory;

/**
 * Implementation of the CCI RecordFactory interface that always throws
 * NotSupportedException.
 *
 * <p>Useful as a placeholder for a RecordFactory argument (for example as
 * defined by the RecordCreator callback), in particular when the connector's
 * <code>ConnectionFactory.getRecordFactory()</code> implementation happens to
 * throw NotSupportedException early rather than throwing the exception from
 * RecordFactory's methods.
 *
 * @author Juergen Hoeller
 * @since 1.2.4
 * @see org.springframework.jca.cci.core.RecordCreator#createRecord(javax.resource.cci.RecordFactory)
 * @see org.springframework.jca.cci.core.CciTemplate#getRecordFactory(javax.resource.cci.ConnectionFactory)
 * @see javax.resource.cci.ConnectionFactory#getRecordFactory()
 * @see javax.resource.NotSupportedException
 */
public class NotSupportedRecordFactory implements RecordFactory {

	public MappedRecord createMappedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

	public IndexedRecord createIndexedRecord(String name) throws ResourceException {
		throw new NotSupportedException("The RecordFactory facility is not supported by the connector");
	}

}
