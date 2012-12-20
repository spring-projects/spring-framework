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

package org.springframework.jca.cci;

import static org.easymock.EasyMock.*;
import static org.junit.Assert.assertTrue;

import javax.resource.ResourceException;
import javax.resource.cci.Connection;
import javax.resource.cci.ConnectionFactory;
import javax.resource.cci.Interaction;
import javax.resource.cci.InteractionSpec;
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.Record;

import org.junit.Test;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Thierry Templier
 * @author Chris Beams
 */
public class CciLocalTransactionTests {

	/**
	 * Test if a transaction ( begin / commit ) is executed on the
	 * LocalTransaction when CciLocalTransactionManager is specified as
	 * transaction manager.
	 */
	@Test
	public void testLocalTransactionCommit() throws ResourceException {
		final ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		LocalTransaction localTransaction = createMock(LocalTransaction.class);
		final Record record = createMock(Record.class);
		final InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.getLocalTransaction()).andReturn(localTransaction);

		localTransaction.begin();

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, record, record)).andReturn(true);

		interaction.close();

		expect(connection.getLocalTransaction()).andReturn(localTransaction);

		localTransaction.commit();

		connection.close();

		replay(connectionFactory, connection, localTransaction, interaction, record);

		org.springframework.jca.cci.connection.CciLocalTransactionManager tm = new org.springframework.jca.cci.connection.CciLocalTransactionManager();
		tm.setConnectionFactory(connectionFactory);
		TransactionTemplate tt = new TransactionTemplate(tm);

		tt.execute(new TransactionCallbackWithoutResult() {
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(connectionFactory));
				CciTemplate ct = new CciTemplate(connectionFactory);
				ct.execute(interactionSpec, record, record);
			}
		});

		verify(connectionFactory, connection, localTransaction, interaction, record);
	}

	/**
	 * Test if a transaction ( begin / rollback ) is executed on the
	 * LocalTransaction when CciLocalTransactionManager is specified as
	 * transaction manager and a non-checked exception is thrown.
	 */
	@Test
	public void testLocalTransactionRollback() throws ResourceException {
		final ConnectionFactory connectionFactory = createMock(ConnectionFactory.class);
		Connection connection = createMock(Connection.class);
		Interaction interaction = createMock(Interaction.class);
		LocalTransaction localTransaction = createMock(LocalTransaction.class);
		final Record record = createMock(Record.class);
		final InteractionSpec interactionSpec = createMock(InteractionSpec.class);

		expect(connectionFactory.getConnection()).andReturn(connection);

		expect(connection.getLocalTransaction()).andReturn(localTransaction);

		localTransaction.begin();

		expect(connection.createInteraction()).andReturn(interaction);

		expect(interaction.execute(interactionSpec, record, record)).andReturn(true);

		interaction.close();

		expect(connection.getLocalTransaction()).andReturn(localTransaction);

		localTransaction.rollback();

		connection.close();

		replay(connectionFactory, connection, localTransaction, interaction, record);

		org.springframework.jca.cci.connection.CciLocalTransactionManager tm = new org.springframework.jca.cci.connection.CciLocalTransactionManager();
		tm.setConnectionFactory(connectionFactory);
		TransactionTemplate tt = new TransactionTemplate(tm);

		try {
			tt.execute(new TransactionCallback() {
				public Object doInTransaction(TransactionStatus status) {
					assertTrue("Has thread connection", TransactionSynchronizationManager.hasResource(connectionFactory));
					CciTemplate ct = new CciTemplate(connectionFactory);
					ct.execute(interactionSpec, record, record);
					throw new DataRetrievalFailureException("error");
				}
			});
		}
		catch (Exception ex) {
		}

		verify(connectionFactory, connection, localTransaction, interaction, record);
	}
}
