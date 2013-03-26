/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.jca.cci;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import org.junit.Test;
import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.object.MappingRecordOperation;
import org.springframework.jca.cci.object.SimpleRecordOperation;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thierry Templier
 * @author Chris Beams
 */
public class EisOperationTests {

	@Test
	public void testSimpleRecordOperation() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);

		query.execute(inputRecord);

		verify(interaction).execute(interactionSpec, inputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testSimpleRecordOperationWithExplicitOutputRecord() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		SimpleRecordOperation operation = new SimpleRecordOperation(connectionFactory, interactionSpec);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);

		operation.execute(inputRecord, outputRecord);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testSimpleRecordOperationWithInputOutputRecord() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputOutputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputOutputRecord, inputOutputRecord)).willReturn(true);

		query.execute(inputOutputRecord, inputOutputRecord);

		verify(interaction).execute(interactionSpec, inputOutputRecord, inputOutputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testMappingRecordOperation() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordFactory recordFactory = mock(RecordFactory.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		QueryCallDetector callDetector = mock(QueryCallDetector.class);

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(callDetector.callCreateInputRecord(recordFactory, inObj)).willReturn(inputRecord);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);
		given(callDetector.callExtractOutputData(outputRecord)).willReturn(outObj);

		assertSame(outObj, query.execute(inObj));
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testMappingRecordOperationWithOutputRecordCreator() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordFactory recordFactory = mock(RecordFactory.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		RecordCreator outputCreator = mock(RecordCreator.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		QueryCallDetector callDetector = mock(QueryCallDetector.class);

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setOutputRecordCreator(outputCreator);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(callDetector.callCreateInputRecord(recordFactory, inObj)).willReturn(inputRecord);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(outputCreator.createRecord(recordFactory)).willReturn(outputRecord);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);
		given(callDetector.callExtractOutputData(outputRecord)).willReturn(outObj);

		assertSame(outObj, query.execute(inObj));
		verify(interaction).close();
		verify(connection).close();
	}


	private class MappingRecordOperationImpl extends MappingRecordOperation {

		private QueryCallDetector callDetector;

		public MappingRecordOperationImpl(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
			super(connectionFactory, interactionSpec);
		}

		public void setCallDetector(QueryCallDetector callDetector) {
			this.callDetector = callDetector;
		}

		@Override
		protected Record createInputRecord(RecordFactory recordFactory, Object inputObject) {
			return this.callDetector.callCreateInputRecord(recordFactory, inputObject);
		}

		@Override
		protected Object extractOutputData(Record outputRecord) throws ResourceException {
			return this.callDetector.callExtractOutputData(outputRecord);
		}
	}


	private interface QueryCallDetector {

		Record callCreateInputRecord(RecordFactory recordFactory, Object inputObject);

		Object callExtractOutputData(Record outputRecord);
	}

}
