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

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

import org.junit.Test;
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
 * @author Chris Beams
 */
public class CciTemplateTests {

	@Test
	public void testCreateIndexedRecord() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		IndexedRecord indexedRecord = createMock(IndexedRecord.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(recordFactory.createIndexedRecord("name")).andReturn(
				indexedRecord);

		replay(connectionFactory, recordFactory);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createIndexedRecord("name");

		verify(connectionFactory, recordFactory);
	}

	@Test
	public void testCreateMappedRecord() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		MappedRecord mappedRecord = createMock(MappedRecord.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(recordFactory.createMappedRecord("name"))
				.andReturn(mappedRecord);

		replay(connectionFactory, recordFactory);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createMappedRecord("name");

		verify(connectionFactory, recordFactory);
	}

	@Test
	public void testTemplateExecuteInputOutput() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testTemplateExecuteWithCreatorAndRecordFactoryNotSupported()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		final Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connectionFactory.getRecordFactory()).andThrow(
				new NotSupportedException("not supported"));

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(new RecordCreator() {
			@Override
			public Record createRecord(RecordFactory recordFactory) {
				assertTrue(recordFactory instanceof NotSupportedRecordFactory);
				return outputRecord;
			}
		});
		ct.execute(interactionSpec, inputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testTemplateExecuteInputTrueWithCreator2()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator creator = createMock(RecordCreator.class);

		Record inputRecord = createMock(Record.class);
		final Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(creator.createRecord(recordFactory)).andReturn(outputRecord);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, creator);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord);

		verify(connectionFactory, connection, interaction, creator);
	}

	@Test
	public void testTemplateExecuteInputFalse() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(
				outputRecord);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputExtractorTrueWithCreator()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordExtractor<Object> extractor = createMock(RecordExtractor.class);
		RecordCreator creator = createMock(RecordCreator.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(creator.createRecord(recordFactory)).andReturn(outputRecord);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		expect(extractor.extractData(outputRecord)).andStubReturn(new Object());

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, extractor, creator);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord, extractor);

		verify(connectionFactory, connection, interaction, extractor, creator);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputExtractorFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordExtractor<Object> extractor = createMock(RecordExtractor.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(
				outputRecord);

		expect(extractor.extractData(outputRecord)).andStubReturn(new Object());

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, extractor);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, extractor);

		verify(connectionFactory, connection, interaction, extractor);
	}

	@Test
	public void testTemplateExecuteInputGeneratorTrueWithCreator()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator generator = createMock(RecordCreator.class);
		RecordCreator creator = createMock(RecordCreator.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(generator.createRecord(recordFactory)).andReturn(inputRecord);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(creator.createRecord(recordFactory)).andReturn(outputRecord);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, generator, creator);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, generator);

		verify(connectionFactory, connection, interaction, generator, creator);
	}

	@Test
	public void testTemplateExecuteInputGeneratorFalse()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator generator = createMock(RecordCreator.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(generator.createRecord(recordFactory)).andReturn(inputRecord);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(
				outputRecord);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, generator);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator);

		verify(connectionFactory, connection, interaction, generator);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputGeneratorExtractorTrueWithCreator()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator generator = createMock(RecordCreator.class);
		RecordExtractor<Object> extractor = createMock(RecordExtractor.class);
		RecordCreator creator = createMock(RecordCreator.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		Object obj = new Object();

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(creator.createRecord(recordFactory)).andReturn(outputRecord);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(generator.createRecord(recordFactory)).andReturn(inputRecord);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		expect(extractor.extractData(outputRecord)).andStubReturn(obj);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, generator, creator,
				extractor);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		assertEquals(obj, ct.execute(interactionSpec, generator, extractor));

		verify(connectionFactory, connection, interaction, generator, creator,
				extractor);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputGeneratorExtractorFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator generator = createMock(RecordCreator.class);
		RecordExtractor<Object> extractor = createMock(RecordExtractor.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(generator.createRecord(recordFactory)).andReturn(inputRecord);

		expect(interaction.execute(interactionSpec, inputRecord)).andReturn(
				outputRecord);

		expect(extractor.extractData(outputRecord)).andStubReturn(new Object());

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, generator, extractor);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		verify(connectionFactory, connection, interaction, generator, extractor);
	}

	@Test
	public void testTemplateExecuteInputOutputConnectionSpec()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		ConnectionSpec connectionSpec = createMock(ConnectionSpec.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputRecord = createMock(Record.class);
		Record outputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection(connectionSpec)).andReturn(
				connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputRecord, outputRecord))
				.andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		ConnectionSpecConnectionFactoryAdapter adapter = new ConnectionSpecConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setConnectionSpec(connectionSpec);
		CciTemplate ct = new CciTemplate(adapter);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputOutputResultsSetFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		RecordFactory recordFactory = createMock(RecordFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		Record record = createMock(Record.class);
		ResultSet resultset = createMock(ResultSet.class);
		RecordCreator generator = createMock(RecordCreator.class);
		RecordExtractor<Object> extractor = createMock(RecordExtractor.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getRecordFactory()).andReturn(recordFactory);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(generator.createRecord(recordFactory)).andReturn(record);

		expect(interaction.execute(interactionSpec, record)).andReturn(
				resultset);

		expect(extractor.extractData(resultset)).andStubReturn(new Object());

		resultset.close();

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, generator,
				extractor, resultset);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		verify(connectionFactory, connection, interaction, generator,
				extractor, resultset);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteConnectionCallback()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		ConnectionCallback<Object> connectionCallback = createMock(ConnectionCallback.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connectionCallback.doInConnection(connection, connectionFactory))
				.andStubReturn(new Object());

		connection.close();

		replay(connectionFactory, connection, connectionCallback);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(connectionCallback);

		verify(connectionFactory, connection, connectionCallback);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInteractionCallback()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		InteractionCallback<Object> interactionCallback = createMock(InteractionCallback.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(
				interactionCallback.doInInteraction(interaction,
						connectionFactory)).andStubReturn(new Object());

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, interactionCallback);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionCallback);

		verify(connectionFactory, connection, interaction, interactionCallback);
	}

	@Test
	public void testTemplateExecuteInputTrueTrueWithCreator()
			throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		RecordCreator creator = createMock(RecordCreator.class);

		Record inputOutputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(
				interaction.execute(interactionSpec, inputOutputRecord,
						inputOutputRecord)).andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction, creator);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		verify(connectionFactory, connection, interaction, creator);
	}

	@Test
	public void testTemplateExecuteInputTrueTrue() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputOutputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(
				interaction.execute(interactionSpec, inputOutputRecord,
						inputOutputRecord)).andReturn(true);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		verify(connectionFactory, connection, interaction);
	}

	@Test
	public void testTemplateExecuteInputFalseTrue() throws ResourceException {
		ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);

		Record inputOutputRecord = createMock(Record.class);

		InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, inputOutputRecord))
				.andReturn(null);

		interaction.close();

		connection.close();

		replay(connectionFactory, connection, interaction);

		CciTemplate ct = new CciTemplate(connectionFactory);
		Record tmpOutputRecord = ct.execute(interactionSpec,
				inputOutputRecord);
		assertNull(tmpOutputRecord);

		verify(connectionFactory, connection, interaction);
	}

}
