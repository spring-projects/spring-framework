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
import javax.resource.cci.LocalTransaction;
import javax.resource.cci.Record;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jca.cci.core.CciTemplate;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Thierry TEMPLIER
 */
public class CciLocalTransactionTests extends TestCase {

	/**
	 * Test if a transaction ( begin / commit ) is executed on the
	 * LocalTransaction when CciLocalTransactionManager is specified as
	 * transaction manager.
	 */
	public void testLocalTransactionCommit() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl localTransactionControl = MockControl.createControl(LocalTransaction.class);
		LocalTransaction localTransaction = (LocalTransaction) localTransactionControl.getMock();
		MockControl recordControl = MockControl.createControl(Record.class);
		final Record record = (Record) recordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		final InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection, 1);

		connection.getLocalTransaction();
		connectionControl.setReturnValue(localTransaction, 1);

		localTransaction.begin();
		localTransactionControl.setVoidCallable(1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, record, record);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.getLocalTransaction();
		connectionControl.setReturnValue(localTransaction);

		localTransaction.commit();
		localTransactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		localTransactionControl.replay();
		interactionControl.replay();

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

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		localTransactionControl.verify();
	}

	/**
	 * Test if a transaction ( begin / rollback ) is executed on the
	 * LocalTransaction when CciLocalTransactionManager is specified as
	 * transaction manager and a non-checked exception is thrown.
	 */
	public void testLocalTransactionRollback() throws ResourceException {
		MockControl connectionFactoryControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory connectionFactory = (ConnectionFactory) connectionFactoryControl.getMock();
		MockControl connectionControl = MockControl.createControl(Connection.class);
		Connection connection = (Connection) connectionControl.getMock();
		MockControl interactionControl = MockControl.createControl(Interaction.class);
		Interaction interaction = (Interaction) interactionControl.getMock();
		MockControl localTransactionControl = MockControl.createControl(LocalTransaction.class);
		LocalTransaction localTransaction = (LocalTransaction) localTransactionControl.getMock();
		MockControl recordControl = MockControl.createControl(Record.class);
		final Record record = (Record) recordControl.getMock();

		MockControl interactionSpecControl = MockControl.createControl(InteractionSpec.class);
		final InteractionSpec interactionSpec = (InteractionSpec) interactionSpecControl.getMock();

		connectionFactory.getConnection();
		connectionFactoryControl.setReturnValue(connection);

		connection.getLocalTransaction();
		connectionControl.setReturnValue(localTransaction);

		localTransaction.begin();
		localTransactionControl.setVoidCallable(1);

		connection.createInteraction();
		connectionControl.setReturnValue(interaction);

		interaction.execute(interactionSpec, record, record);
		interactionControl.setReturnValue(true, 1);

		interaction.close();
		interactionControl.setVoidCallable(1);

		connection.getLocalTransaction();
		connectionControl.setReturnValue(localTransaction);

		localTransaction.rollback();
		localTransactionControl.setVoidCallable(1);

		connection.close();
		connectionControl.setVoidCallable(1);

		connectionFactoryControl.replay();
		connectionControl.replay();
		localTransactionControl.replay();
		interactionControl.replay();

		org.springframework.jca.cci.connection.CciLocalTransactionManager tm = new org.springframework.jca.cci.connection.CciLocalTransactionManager();
		tm.setConnectionFactory(connectionFactory);
		TransactionTemplate tt = new TransactionTemplate(tm);

		try {
			Object result = tt.execute(new TransactionCallback() {
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

		connectionFactoryControl.verify();
		connectionControl.verify();
		interactionControl.verify();
		localTransactionControl.verify();
	}
}
