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

package org.springframework.jms.connection;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageProducer;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import junit.framework.TestCase;
import org.easymock.MockControl;

import org.springframework.jms.StubQueue;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.core.JmsTemplate102;
import org.springframework.jms.core.MessageCreator;
import org.springframework.jms.core.SessionCallback;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.UnexpectedRollbackException;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * @author Juergen Hoeller
 * @since 26.07.2004
 */
public class JmsTransactionManagerTests extends TestCase {

	public void testTransactionCommit() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testTransactionRollback() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.rollback();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.rollback(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testParticipatingTransactionWithCommit() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		final JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jt.execute(new SessionCallback() {
					@Override
					public Object doInJms(Session sess) {
						assertTrue(sess == session);
						return null;
					}
				});
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testParticipatingTransactionWithRollbackOnly() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.rollback();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		final JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jt.execute(new SessionCallback() {
					@Override
					public Object doInJms(Session sess) {
						assertTrue(sess == session);
						return null;
					}
				});
				status.setRollbackOnly();
			}
		});
		try {
			tm.commit(ts);
			fail("Should have thrown UnexpectedRollbackException");
		}
		catch (UnexpectedRollbackException ex) {
			// expected
		}

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testSuspendedTransaction() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 2);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		con.createSession(false, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session2, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		session2.close();
		session2Control.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(2);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		final JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_NOT_SUPPORTED);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jt.execute(new SessionCallback() {
					@Override
					public Object doInJms(Session sess) {
						assertTrue(sess != session);
						return null;
					}
				});
			}
		});
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testTransactionSuspension() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		final ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		final Session session = (Session) sessionControl.getMock();
		MockControl session2Control = MockControl.createControl(Session.class);
		final Session session2 = (Session) session2Control.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 2);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session2, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session2.commit();
		session2Control.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		session2.close();
		session2Control.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(2);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		final JmsTemplate jt = new JmsTemplate(cf);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		TransactionTemplate tt = new TransactionTemplate(tm);
		tt.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
		tt.execute(new TransactionCallbackWithoutResult() {
			@Override
			protected void doInTransactionWithoutResult(TransactionStatus status) {
				jt.execute(new SessionCallback() {
					@Override
					public Object doInJms(Session sess) {
						assertTrue(sess != session);
						return null;
					}
				});
			}
		});
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testTransactionCommitWithMessageProducer() throws JMSException {
		Destination dest = new StubQueue();

		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(Session.class);
		Session session = (Session) sessionControl.getMock();
		MockControl producerControl = MockControl.createControl(MessageProducer.class);
		MessageProducer producer = (MessageProducer) producerControl.getMock();
		MockControl messageControl = MockControl.createControl(Message.class);
		final Message message = (Message) messageControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.createProducer(dest);
		sessionControl.setReturnValue(producer, 1);
		producer.send(message);
		producerControl.setVoidCallable(1);
		session.getTransacted();
		sessionControl.setReturnValue(true, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		producer.close();
		producerControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		producerControl.replay();
		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager(cf);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		JmsTemplate jt = new JmsTemplate(cf);
		jt.send(dest, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return message;
			}
		});
		tm.commit(ts);

		producerControl.verify();
		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testTransactionCommit102WithQueue() throws JMSException {
		MockControl cfControl = MockControl.createControl(QueueConnectionFactory.class);
		QueueConnectionFactory cf = (QueueConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		QueueConnection con = (QueueConnection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(QueueSession.class);
		final QueueSession session = (QueueSession) sessionControl.getMock();

		cf.createQueueConnection();
		cfControl.setReturnValue(con, 1);
		con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager102(cf, false);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		JmsTemplate jt = new JmsTemplate102(cf, false);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	public void testTransactionCommit102WithTopic() throws JMSException {
		MockControl cfControl = MockControl.createControl(TopicConnectionFactory.class);
		TopicConnectionFactory cf = (TopicConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		TopicConnection con = (TopicConnection) conControl.getMock();
		MockControl sessionControl = MockControl.createControl(TopicSession.class);
		final TopicSession session = (TopicSession) sessionControl.getMock();

		cf.createTopicConnection();
		cfControl.setReturnValue(con, 1);
		con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(session, 1);
		session.commit();
		sessionControl.setVoidCallable(1);
		session.close();
		sessionControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		sessionControl.replay();
		conControl.replay();
		cfControl.replay();

		JmsTransactionManager tm = new JmsTransactionManager102(cf, true);
		TransactionStatus ts = tm.getTransaction(new DefaultTransactionDefinition());
		JmsTemplate jt = new JmsTemplate102(cf, true);
		jt.execute(new SessionCallback() {
			@Override
			public Object doInJms(Session sess) {
				assertTrue(sess == session);
				return null;
			}
		});
		tm.commit(ts);

		sessionControl.verify();
		conControl.verify();
		cfControl.verify();
	}

	@Override
	protected void tearDown() {
		assertTrue(TransactionSynchronizationManager.getResourceMap().isEmpty());
		assertFalse(TransactionSynchronizationManager.isSynchronizationActive());
	}

}
