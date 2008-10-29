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

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.object.MappingRecordOperation;
import org.springframework.jca.cci.object.SimpleRecordOperation;

/**
 * @author Thierry TEMPLIER
 */
public class EisOperationTests extends TestCase {

	public void testSimpleRecordOperation() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, inputRecord);
		interactionControl.setReturnValue(outputRecord, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();

		query.execute(inputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testSimpleRecordOperationWithExplicitOutputRecord() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		SimpleRecordOperation operation = new SimpleRecordOperation(connectionFactory, interactionSpec);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection, 1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction, 1);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();

		operation.execute(inputRecord, outputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testSimpleRecordOperationWithInputOutputRecord() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputOutputRecordControl = MockControl.createControl(Record.class);
		Record inputOutputRecord = (Record) inputOutputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		SimpleRecordOperation query = new SimpleRecordOperation(connectionFactory, interactionSpec);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, inputOutputRecord, inputOutputRecord);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();

		query.execute(inputOutputRecord, inputOutputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testMappingRecordOperation() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		MockControl callDetectorControl = MockControl.createControl(QueryCallDetector.class);
		QueryCallDetector callDetector = (QueryCallDetector) callDetectorControl.getMock();

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		callDetector.callCreateInputRecord(recordFactory, inObj);
		callDetectorControl.setReturnValue(inputRecord, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection, 1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction, 1);

		interaction.execute(interactionSpec, inputRecord);
		interactionControl.setReturnValue(outputRecord, 1);

		callDetector.callExtractOutputData(outputRecord);
		callDetectorControl.setReturnValue(outObj, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		callDetectorControl.replay();

		assertSame(outObj, query.execute(inObj));

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		callDetectorControl.verify();
	}

	public void testMappingRecordOperationWithOutputRecordCreator() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();
		MockControl outputCreatorControl = MockControl.createControl(RecordCreator.class);
		RecordCreator outputCreator = (RecordCreator) outputCreatorControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		MockControl callDetectorControl = MockControl.createControl(QueryCallDetector.class);
		QueryCallDetector callDetector = (QueryCallDetector) callDetectorControl.getMock();

		MappingRecordOperationImpl query = new MappingRecordOperationImpl(connectionFactory, interactionSpec);
		query.setOutputRecordCreator(outputCreator);
		query.setCallDetector(callDetector);

		Object inObj = new Object();
		Object outObj = new Object();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		callDetector.callCreateInputRecord(recordFactory, inObj);
		callDetectorControl.setReturnValue(inputRecord, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection, 1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction, 1);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		outputCreator.createRecord(recordFactory);
		outputCreatorControl.setReturnValue(outputRecord, 1);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		callDetector.callExtractOutputData(outputRecord);
		callDetectorControl.setReturnValue(outObj, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		outputCreatorControl.replay();
		callDetectorControl.replay();

		assertSame(outObj, query.execute(inObj));

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		outputCreatorControl.verify();
		callDetectorControl.verify();
	}


	private class MappingRecordOperationImpl extends MappingRecordOperation {

		private QueryCallDetector callDetector;

		public MappingRecordOperationImpl(ConnectionFactory connectionFactory, InteractionSpec interactionSpec) {
			super(connectionFactory, interactionSpec);
		}

		public void setCallDetector(QueryCallDetector callDetector) {
			this.callDetector = callDetector;
		}

		protected Record createInputRecord(RecordFactory recordFactory, Object inputObject) {
			return this.callDetector.callCreateInputRecord(recordFactory, inputObject);
		}

		protected Object extractOutputData(Record outputRecord) throws ResourceException {
			return this.callDetector.callExtractOutputData(outputRecord);
		}
	}


	private interface QueryCallDetector {

		Record callCreateInputRecord(RecordFactory recordFactory, Object inputObject);

		Object callExtractOutputData(Record outputRecord);
	}

}
