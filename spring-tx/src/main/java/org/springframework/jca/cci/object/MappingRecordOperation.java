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

import java.sql.SQLException;

import javax.resource.ResourceException;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.core.RecordExtractor;

/**
 * EIS operation object that expects mapped input and output objects,
 * converting to and from CCI Records.
 *
 * <p>Concrete subclasses must implement the abstract
 * <code>createInputRecord(RecordFactory, Object)</code> and
 * <code>extractOutputData(Record)</code> methods, to create an input
 * Record from an object and to convert an output Record into an object,
 * respectively.
 *
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @since 1.2
 * @see #createInputRecord(javax.resource.cci.RecordFactory, Object)
 * @see #extractOutputData(javax.resource.cci.Record)
 */
public abstract class MappingRecordOperation extends EisOperation {

	/**
	 * Constructor that allows use as a JavaBean.
	 */
	public MappingRecordOperation() {
	}

	/**
	 * Convenient constructor with ConnectionFactory and specifications
	 * (connection and interaction).
	 * @param connectionFactory ConnectionFactory to use to obtain connections
	 */
	public MappingRecordOperation(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
		getCciTemplate().setConnectionFactory(connectionFactory);
		setInteractionSpec(interactionSpec);
	}

	/**
	 * Set a RecordCreator that should be used for creating default output Records.
	 * <p>Default is none: CCI's <code>Interaction.execute</code> variant
	 * that returns an output Record will be called.
	 * <p>Specify a RecordCreator here if you always need to call CCI's
	 * <code>Interaction.execute</code> variant with a passed-in output Record.
	 * This RecordCreator will then be invoked to create a default output Record instance.
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record)
	 * @see javax.resource.cci.Interaction#execute(javax.resource.cci.InteractionSpec, Record, Record)
	 * @see org.springframework.jca.cci.core.CciTemplate#setOutputRecordCreator
	 */
	public void setOutputRecordCreator(RecordCreator creator) {
		getCciTemplate().setOutputRecordCreator(creator);
	}

	/**
	 * Execute the interaction encapsulated by this operation object.
	 * @param inputObject the input data, to be converted to a Record
	 * by the <code>createInputRecord</code> method
	 * @return the output data extracted with the <code>extractOutputData</code> method
	 * @throws DataAccessException if there is any problem
	 * @see #createInputRecord
	 * @see #extractOutputData
	 */
	public Object execute(Object inputObject) throws DataAccessException {
		return getCciTemplate().execute(
				getInteractionSpec(), new RecordCreatorImpl(inputObject), new RecordExtractorImpl());
	}


	/**
	 * Subclasses must implement this method to generate an input Record
	 * from an input object passed into the <code>execute</code> method.
	 * @param inputObject the passed-in input object
	 * @return the CCI input Record
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @see #execute(Object)
	 */
	protected abstract Record createInputRecord(RecordFactory recordFactory, Object inputObject)
			throws ResourceException, DataAccessException;

	/**
	 * Subclasses must implement this method to convert the Record returned
	 * by CCI execution into a result object for the <code>execute</code> method.
	 * @param outputRecord the Record returned by CCI execution
	 * @return the result object
	 * @throws ResourceException if thrown by a CCI method, to be auto-converted
	 * to a DataAccessException
	 * @see #execute(Object)
	 */
	protected abstract Object extractOutputData(Record outputRecord)
			throws ResourceException, SQLException, DataAccessException;


	/**
	 * Implementation of RecordCreator that calls the enclosing
	 * class's <code>createInputRecord</code> method.
	 */
	protected class RecordCreatorImpl implements RecordCreator {

		private final Object inputObject;

		public RecordCreatorImpl(Object inObject) {
			this.inputObject = inObject;
		}

		public Record createRecord(RecordFactory recordFactory) throws ResourceException, DataAccessException {
			return createInputRecord(recordFactory, this.inputObject);
		}
	}


	/**
	 * Implementation of RecordExtractor that calls the enclosing
	 * class's <code>extractOutputData</code> method.
	 */
	protected class RecordExtractorImpl implements RecordExtractor<Object> {

		public Object extractData(Record record) throws ResourceException, SQLException, DataAccessException {
			return extractOutputData(record);
		}
	}

}
