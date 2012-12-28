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

package org.springframework.jca.cci;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertSame;

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

/**
 * @author Thierry Templier
 * @author Chris Beams
 */
public class EisOperationTests {

	@Test
	public void testSimpleRecordOperation() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(outputRecord);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		query.execute(inputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testSimpleRecordOperationWithExplicitOutputRecord() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		SimpleRecordOperation operation = new SimpleRecordOperation(connectionFactory, interactionSpec);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord)).andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		operation.execute(inputRecord, outputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testSimpleRecordOperationWithInputOutputRecord() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputOutputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputOutputRecord, inputOutputRecord)).andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		query.execute(inputOutputRecord, inputOutputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testMappingRecordOperation() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		QueryCallDetector callDetector = createMock(QueryCallDetector.class);

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(callDetector.callCreateInputRecord(recordFactory, inObj)).andReturn(inputRecord);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(outputRecord);

		expect(callDetector.callExtractOutputData(outputRecord)).andReturn(outObj);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, callDetector);

		assertSame(outObj, query.execute(inObj));

		verify(connectionFactory, connection, interaction, callDetector);
	}

	@Test
	public void testMappingRecordOperationWithOutputRecordCreator() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		RecordCreator outputCreator = createMock(RecordCreator.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		QueryCallDetector callDetector = createMock(QueryCallDetector.class);

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setOutputRecordCreator(outputCreator);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(callDetector.callCreateInputRecord(recordFactory, inObj)).andReturn(inputRecord);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(outputCreator.createRecord(recordFactory)).andReturn(outputRecord);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord)).andReturn(true);

		expect(callDetector.callExtractOutputData(outputRecord)).andReturn(outObj);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, outputCreator, callDetector);

		assertSame(outObj, query.execute(inObj));

		verify(connectionFactory, connection, interaction, outputCreator, callDetector);
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
