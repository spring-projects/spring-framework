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

package org.springframework.jca.cci.object;

import java.io.IOException;

import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jca.cci.core.support.CommAreaRecord;

/**
 * EIS operation object for access to COMMAREA records.
 * Subclass of the generic MappingRecordOperation class.
 *
 * @author Thierry Templier
 * @since 1.2
 */
public abstract class MappingCommAreaOperation extends MappingRecordOperation {

	/**
	 * Create a new MappingCommAreaQuery.
	 * @see #setConnectionFactory
	 * @see #setInteractionSpec
	 */
	public MappingCommAreaOperation() {
	}

	/**
	 * Create a new MappingCommAreaQuery.
	 * @param connectionFactory ConnectionFactory to use to obtain connections
	 * @param interactionSpec specification to configure the interaction
	 */
	public MappingCommAreaOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		super(connectionFactory, interactionSpec);
	}


	@Override
	protected final Record createInputRecord(RecordFactory recordFactory, Object inObject) {
		try {
			return new CommAreaRecord(objectToBytes(inObject));
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}

	@Override
	protected final Object extractOutputData(Record record) throws DataAccessException {
		CommAreaRecord commAreaRecord = (CommAreaRecord) record;
		try {
			return bytesToObject(commAreaRecord.toByteArray());
		}
		catch (IOException ex) {
			throw new DataRetrievalFailureException("I/O exception during bytes conversion", ex);
		}
	}


	/**
	 * Method used to convert an object into COMMAREA bytes.
	 * @param inObject the input data
	 * @return the COMMAREA's bytes
	 * @throws IOException if thrown by I/O methods
	 * @throws DataAccessException if conversion failed
	 */
	protected abstract byte[] objectToBytes(Object inObject) throws IOException, DataAccessException;

	/**
	 * Method used to convert the COMMAREA's bytes to an object.
	 * @param bytes the COMMAREA's bytes
	 * @return the output data
	 * @throws IOException if thrown by I/O methods
	 * @throws DataAccessException if conversion failed
	 */
	protected abstract Object bytesToObject(byte[] bytes) throws IOException, DataAccessException;

}
