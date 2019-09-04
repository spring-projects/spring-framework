/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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

import org.junit.Test;

import org.springframework.jca.cci.connection.ConnectionSpecConnectionFactoryAdapter;
import org.springframework.jca.cci.connection.NotSupportedRecordFactory;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.jca.cci.core.ConnectionCallback;
import org.springframework.jca.cci.core.InteractionCallback;
import org.springframework.jca.cci.core.RecordCreator;
import org.springframework.jca.cci.core.RecordExtractor;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Thierry Templier
 * @author Juergen Hoeller
 * @author Chris Beams
 */
public class CciTemplateTests {

	@Test
	public void testCreateIndexedRecord() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		IndexedRecord indexedRecord = mock(IndexedRecord.class);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(recordFactory.createIndexedRecord("name")).willReturn(indexedRecord);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createIndexedRecord("name");

		verify(recordFactory).createIndexedRecord("name");
	}

	@Test
	public void testCreateMappedRecord() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		MappedRecord mappedRecord = mock(MappedRecord.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(recordFactory.createMappedRecord("name")).willReturn(mappedRecord);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.createMappedRecord("name");

		verify(recordFactory).createMappedRecord("name");
	}

	@Test
	public void testTemplateExecuteInputOutput() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);


		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteWithCreatorAndRecordFactoryNotSupported()
			throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		final Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connectionFactory.getRecordFactory()).willThrow(new NotSupportedException("not supported"));
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(new RecordCreator() {
			@Override
			public Record createRecord(RecordFactory recordFactory) {
				assertTrue(recordFactory instanceof NotSupportedRecordFactory);
				return outputRecord;
			}
		});
		ct.execute(interactionSpec, inputRecord);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputTrueWithCreator2()
			throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator creator = mock(RecordCreator.class);

		Record inputRecord = mock(Record.class);
		final Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(connection.createInteraction()).willReturn(interaction);
		given(creator.createRecord(recordFactory)).willReturn(outputRecord);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputFalse() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord);

		verify(interaction).execute(interactionSpec, inputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputExtractorTrueWithCreator()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordExtractor<Object> extractor = mock(RecordExtractor.class);
		RecordCreator creator = mock(RecordCreator.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(creator.createRecord(recordFactory)).willReturn(outputRecord);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);
		given(extractor.extractData(outputRecord)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputRecord, extractor);

		verify(extractor).extractData(outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputExtractorFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordExtractor<Object> extractor = mock(RecordExtractor.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);
		given(extractor.extractData(outputRecord)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputRecord, extractor);

		verify(extractor).extractData(outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputGeneratorTrueWithCreator()
			throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator generator = mock(RecordCreator.class);
		RecordCreator creator = mock(RecordCreator.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(generator.createRecord(recordFactory)).willReturn(inputRecord);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(creator.createRecord(recordFactory)).willReturn(outputRecord);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);


		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, generator);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputGeneratorFalse()
			throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator generator = mock(RecordCreator.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(generator.createRecord(recordFactory)).willReturn(inputRecord);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator);

		verify(interaction).execute(interactionSpec, inputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputGeneratorExtractorTrueWithCreator()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator generator = mock(RecordCreator.class);
		RecordExtractor<Object> extractor = mock(RecordExtractor.class);
		RecordCreator creator = mock(RecordCreator.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		Object obj = new Object();

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(creator.createRecord(recordFactory)).willReturn(outputRecord);
		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(generator.createRecord(recordFactory)).willReturn(inputRecord);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);
		given(extractor.extractData(outputRecord)).willReturn(obj);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		assertEquals(obj, ct.execute(interactionSpec, generator, extractor));

		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputGeneratorExtractorFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator generator = mock(RecordCreator.class);
		RecordExtractor<Object> extractor = mock(RecordExtractor.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(generator.createRecord(recordFactory)).willReturn(inputRecord);
		given(interaction.execute(interactionSpec, inputRecord)).willReturn(outputRecord);
		given(extractor.extractData(outputRecord)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		verify(extractor).extractData(outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputOutputConnectionSpec() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		ConnectionSpec connectionSpec = mock(ConnectionSpec.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);

		Record inputRecord = mock(Record.class);
		Record outputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection(connectionSpec)).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputRecord, outputRecord)).willReturn(true);

		ConnectionSpecConnectionFactoryAdapter adapter = new ConnectionSpecConnectionFactoryAdapter();
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.setConnectionSpec(connectionSpec);
		CciTemplate ct = new CciTemplate(adapter);
		ct.execute(interactionSpec, inputRecord, outputRecord);

		verify(interaction).execute(interactionSpec, inputRecord, outputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInputOutputResultsSetFalse()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		RecordFactory recordFactory = mock(RecordFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		Record record = mock(Record.class);
		ResultSet resultset = mock(ResultSet.class);
		RecordCreator generator = mock(RecordCreator.class);
		RecordExtractor<Object> extractor = mock(RecordExtractor.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getRecordFactory()).willReturn(recordFactory);
		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(generator.createRecord(recordFactory)).willReturn(record);
		given(interaction.execute(interactionSpec, record)).willReturn(resultset);
		given(extractor.extractData(resultset)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, generator, extractor);

		verify(extractor).extractData(resultset);
		verify(resultset).close();
		verify(interaction).close();
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteConnectionCallback()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		ConnectionCallback<Object> connectionCallback = mock(ConnectionCallback.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connectionCallback.doInConnection(connection, connectionFactory)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(connectionCallback);

		verify(connectionCallback).doInConnection(connection, connectionFactory);
		verify(connection).close();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testTemplateExecuteInteractionCallback()
			throws ResourceException, SQLException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		InteractionCallback<Object> interactionCallback = mock(InteractionCallback.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interactionCallback.doInInteraction(interaction,connectionFactory)).willReturn(new Object());

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionCallback);

		verify(interactionCallback).doInInteraction(interaction,connectionFactory);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputTrueTrueWithCreator()
			throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		RecordCreator creator = mock(RecordCreator.class);

		Record inputOutputRecord = mock(Record.class);

		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputOutputRecord, inputOutputRecord)).willReturn(true);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.setOutputRecordCreator(creator);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		verify(interaction).execute(interactionSpec, inputOutputRecord, inputOutputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputTrueTrue() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		Record inputOutputRecord = mock(Record.class);
		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputOutputRecord, inputOutputRecord)).willReturn(true);

		CciTemplate ct = new CciTemplate(connectionFactory);
		ct.execute(interactionSpec, inputOutputRecord, inputOutputRecord);

		verify(interaction).execute(interactionSpec, inputOutputRecord, inputOutputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

	@Test
	public void testTemplateExecuteInputFalseTrue() throws ResourceException {
		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		Connection connection = mock(Connection.class);
		Interaction interaction = mock(Interaction.class);
		Record inputOutputRecord = mock(Record.class);
		InteractionSpec interactionSpec = mock(InteractionSpec.class);

		given(connectionFactory.getConnection()).willReturn(connection);
		given(connection.createInteraction()).willReturn(interaction);
		given(interaction.execute(interactionSpec, inputOutputRecord)).willReturn(null);

		CciTemplate ct = new CciTemplate(connectionFactory);
		Record tmpOutputRecord = ct.execute(interactionSpec, inputOutputRecord);
		assertNull(tmpOutputRecord);

		verify(interaction).execute(interactionSpec, inputOutputRecord);
		verify(interaction).close();
		verify(connection).close();
	}

}
