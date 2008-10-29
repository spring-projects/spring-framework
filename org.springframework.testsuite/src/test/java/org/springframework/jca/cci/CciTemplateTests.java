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

import java.sql.SQLException;

import javax.resource.NotSupportedException;
import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.ConnectionSpec;
import javax.resource.cci.IndexedRecord;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.MappedRecord;
import javax.resource.cci.Record;
import javax.resource.cci.RecordFactory;
import javax.resource.cci.ResultSet;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jca.cci.connection.ConnectionSpecConnectionFactoryAdapter;
import org.springframework.jca.cci.connection.NotSupportedRecordFactory;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.jca.cci.core.ConnectionCallback;
import org.springframework.jca.cci.core.InteractionCallback;
import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.core.RecordExtractor;

/**
 * @author Thierry Templier
 * @author Juergen Hoeller
 */
public class CciTemplateTests extends TestCase {

	public void testCreateIndexedRecord() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl indexedRecordControl = MockControl.createStrictControl(IndexedRecord.class);
		IndexedRecord indexedRecord = (IndexedRecord) indexedRecordControl.getMock();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		recordFactory.createIndexedRecord("name");
		recordFactoryControl.setReturnValue(indexedRecord, 1);

		connectionFactoryControl.replay();
		recordFactoryControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createIndexedRecord("name");

		connectionFactoryControl.verify();
		recordFactoryControl.verify();
	}

	public void testCreateMappedRecord() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl mappedRecordControl = MockControl.createStrictControl(MappedRecord.class);
		MappedRecord mappedRecord = (MappedRecord) mappedRecordControl.getMock();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		recordFactory.createMappedRecord("name");
		recordFactoryControl.setReturnValue(mappedRecord, 1);

		connectionFactoryControl.replay();
		recordFactoryControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createMappedRecord("name");

		connectionFactoryControl.verify();
		recordFactoryControl.verify();
	}

	public void testTemplateExecuteInputOutput() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

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

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testTemplateExecuteWithCreatorAndRecordFactoryNotSupported() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		final Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setThrowable(new NotSupportedException("not supported"), 1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(new RecordCreator() {
			public Record createRecord(RecordFactory recordFactory) {
				assertTrue(recordFactory instanceof NotSupportedRecordFactory);
				return outputRecord;
			}
		});
		ct.execute(interactionSpec, inputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testTemplateExecuteInputTrueWithCreator() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl creatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator creator = (RecordCreator) creatorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		creator.createRecord(recordFactory);
		creatorControl.setReturnValue(outputRecord);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		creatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		creatorControl.verify();
	}

	public void testTemplateExecuteInputFalse() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

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

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testTemplateExecuteInputExtractorTrueWithCreator() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl extractorControl = MockControl.createStrictControl(RecordExtractor.class);
		RecordExtractor extractor = (RecordExtractor) extractorControl.getMock();
		MockControl creatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator creator = (RecordCreator) creatorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		Object obj = new Object();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		creator.createRecord(recordFactory);
		creatorControl.setReturnValue(outputRecord);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		extractor.extractData(outputRecord);
		extractorControl.setReturnValue(obj);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		extractorControl.replay();
		creatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord, extractor);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		extractorControl.verify();
		creatorControl.verify();
	}

	public void testTemplateExecuteInputExtractorFalse() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl extractorControl = MockControl.createStrictControl(RecordExtractor.class);
		RecordExtractor extractor = (RecordExtractor) extractorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		Object obj = new Object();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, inputRecord);
		interactionControl.setReturnValue(outputRecord, 1);

		extractor.extractData(outputRecord);
		extractorControl.setReturnValue(obj);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		extractorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, extractor);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		extractorControl.verify();
	}

	public void testTemplateExecuteInputGeneratorTrueWithCreator() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl generatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator generator = (RecordCreator) generatorControl.getMock();
		MockControl creatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator creator = (RecordCreator) creatorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		generator.createRecord(recordFactory);
		generatorControl.setReturnValue(inputRecord);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		creator.createRecord(recordFactory);
		creatorControl.setReturnValue(outputRecord);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		generatorControl.replay();
		creatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, generator);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		generatorControl.verify();
		creatorControl.verify();
	}

	public void testTemplateExecuteInputGeneratorFalse() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl generatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator generator = (RecordCreator) generatorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		generator.createRecord(recordFactory);
		generatorControl.setReturnValue(inputRecord);

		interaction.execute(interactionSpec, inputRecord);
		interactionControl.setReturnValue(outputRecord, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		generatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		generatorControl.verify();
	}

	public void testTemplateExecuteInputGeneratorExtractorTrueWithCreator() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl generatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator generator = (RecordCreator) generatorControl.getMock();
		MockControl extractorControl = MockControl.createStrictControl(RecordExtractor.class);
		RecordExtractor extractor = (RecordExtractor) extractorControl.getMock();
		MockControl creatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator creator = (RecordCreator) creatorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		Object obj = new Object();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		creator.createRecord(recordFactory);
		creatorControl.setReturnValue(outputRecord);

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		generator.createRecord(recordFactory);
		generatorControl.setReturnValue(inputRecord);

		interaction.execute(interactionSpec, inputRecord, outputRecord);
		interactionControl.setReturnValue(true, 1);

		extractor.extractData(outputRecord);
		extractorControl.setReturnValue(obj);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		generatorControl.replay();
		extractorControl.replay();
		creatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		assertEquals(obj, ct.execute(interactionSpec, generator, extractor));

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		generatorControl.verify();
		extractorControl.verify();
		creatorControl.verify();
	}

	public void testTemplateExecuteInputGeneratorExtractorFalse() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl generatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator generator = (RecordCreator) generatorControl.getMock();
		MockControl extractorControl = MockControl.createStrictControl(RecordExtractor.class);
		RecordExtractor extractor = (RecordExtractor) extractorControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		Object obj = new Object();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		generator.createRecord(recordFactory);
		generatorControl.setReturnValue(inputRecord);

		interaction.execute(interactionSpec, inputRecord);
		interactionControl.setReturnValue(outputRecord, 1);

		extractor.extractData(outputRecord);
		extractorControl.setReturnValue(obj);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		generatorControl.replay();
		extractorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		generatorControl.verify();
		extractorControl.verify();
	}

	public void testTemplateExecuteInputOutputConnectionSpec() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputRecord = (Record) inputRecordControl.getMock();
		MockControl outputRecordControl = MockControl.createStrictControl(Record.class);
		Record outputRecord = (Record) outputRecordControl.getMock();

		MockControl connectionSpecControl = MockControl.createStrictControl(ConnectionSpec.class);
		ConnectionSpec connectionSpec = (ConnectionSpec) connectionSpecControl.getMock();
		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection(connectionSpec);
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

		ConnectionSpecConnectionFactoryAdapter adapter = new ConnectionSpecConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setConnectionSpec(connectionSpec);
		CciTemplate ct = new CciTemplate(adapter);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testTemplateExecuteInputOutputResultsSetFalse() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl recordFactoryControl = MockControl.createStrictControl(RecordFactory.class);
		RecordFactory recordFactory = (RecordFactory) recordFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl recordControl = MockControl.createStrictControl(Record.class);
		Record record = (Record) recordControl.getMock();
		MockControl resultsetControl = MockControl.createStrictControl(ResultSet.class);
		ResultSet resultset = (ResultSet) resultsetControl.getMock();
		MockControl generatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator generator = (RecordCreator) generatorControl.getMock();
		MockControl extractorControl = MockControl.createStrictControl(RecordExtractor.class);
		RecordExtractor extractor = (RecordExtractor) extractorControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		Object obj = new Object();

		connectionFactory.getRecordFactory();
		connectionFactoryControl.setReturnValue(recordFactory, 1);

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		generator.createRecord(recordFactory);
		generatorControl.setReturnValue(record);

		interaction.execute(interactionSpec, record);
		interactionControl.setReturnValue(resultset, 1);

		extractor.extractData(resultset);
		extractorControl.setReturnValue(obj);

		resultset.close();
		resultsetControl.setVoidCallable(1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		generatorControl.replay();
		extractorControl.replay();
		resultsetControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		generatorControl.verify();
		extractorControl.verify();
		resultsetControl.verify();
	}

	public void testTemplateExecuteConnectionCallback() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl connectionCallbackControl = MockControl.createStrictControl(ConnectionCallback.class);
		ConnectionCallback connectionCallback = (ConnectionCallback) connectionCallbackControl.getMock();

		Object obj = new Object();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connectionCallback.doInConnection(connection, connectionFactory);
		connectionCallbackControl.setReturnValue(obj);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		connectionCallbackControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(connectionCallback);

		connectionFactoryControl.verify();
		connectionControl.verify();
		connectionCallbackControl.verify();
	}

	public void testTemplateExecuteInteractionCallback() throws ResourceException, SQLException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl interactionCallbackControl = MockControl.createStrictControl(InteractionCallback.class);
		InteractionCallback interactionCallback = (InteractionCallback) interactionCallbackControl.getMock();

		Object obj = new Object();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interactionCallback.doInInteraction(interaction, connectionFactory);
		interactionCallbackControl.setReturnValue(obj);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();
		interactionCallbackControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionCallback);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		interactionCallbackControl.verify();
	}

	public void testTemplateExecuteInputTrueTrueWithCreator() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl creatorControl = MockControl.createStrictControl(RecordCreator.class);
		RecordCreator creator = (RecordCreator) creatorControl.getMock();

		MockControl inputOutputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputOutputRecord = (Record) inputOutputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

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
		creatorControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		creatorControl.verify();
	}

	public void testTemplateExecuteInputTrueTrue() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputOutputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputOutputRecord = (Record) inputOutputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

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

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

	public void testTemplateExecuteInputFalseTrue() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createStrictControl(ConnectionFactory.class);
		ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createStrictControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createStrictControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();

		MockControl inputOutputRecordControl = MockControl.createStrictControl(Record.class);
		Record inputOutputRecord = (Record) inputOutputRecordControl.getMock();

		MockControl interactionSpecControl = MockControl.createStrictControl(InteractionSpec.class);
		InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, inputOutputRecord);
		interactionControl.setReturnValue(null, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		interactionControl.replay();

		CciTemplate ct = new CciTemplate(connectionFactory);
		Record tmpOutputRecord = (Record) ct.execute(interactionSpec, inputOutputRecord);
		assertNull(tmpOutputRecord);

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
	}

}
