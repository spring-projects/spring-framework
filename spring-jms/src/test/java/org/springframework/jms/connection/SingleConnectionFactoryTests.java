/*
 * Copyright 2002-2011 the original author or authors.
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
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.QueueConnection;
import javax.jms.QueueConnectionFactory;
import javax.jms.QueueSession;
import javax.jms.Session;
import javax.jms.TopicConnection;
import javax.jms.TopicConnectionFactory;
import javax.jms.TopicSession;

import junit.framework.TestCase;
import org.easymock.MockControl;

/**
 * @author Juergen Hoeller
 * @since 26.07.2004
 */
public class SingleConnectionFactoryTests extends TestCase {

	public void testWithConnection() throws JMSException {
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.stop();  // should be ignored
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();  // should be ignored
		con2.stop();  // should be ignored
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		conControl.verify();
	}

	public void testWithQueueConnection() throws JMSException {
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		Connection con = (QueueConnection) conControl.getMock();

		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		QueueConnection con1 = scf.createQueueConnection();
		con1.start();
		con1.stop();  // should be ignored
		con1.close();  // should be ignored
		QueueConnection con2 = scf.createQueueConnection();
		con2.start();
		con2.stop();  // should be ignored
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		conControl.verify();
	}

	public void testWithTopicConnection() throws JMSException {
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		Connection con = (TopicConnection) conControl.getMock();

		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		TopicConnection con1 = scf.createTopicConnection();
		con1.start();
		con1.stop();  // should be ignored
		con1.close();  // should be ignored
		TopicConnection con2 = scf.createTopicConnection();
		con2.start();
		con2.stop();  // should be ignored
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		conControl.verify();
	}

	public void testWithConnectionFactory() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithQueueConnectionFactoryAndJms11Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(QueueConnectionFactory.class);
		QueueConnectionFactory cf = (QueueConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		QueueConnection con = (QueueConnection) conControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(QueueConnectionFactory.class);
		QueueConnectionFactory cf = (QueueConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		QueueConnection con = (QueueConnection) conControl.getMock();

		cf.createQueueConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createQueueConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createQueueConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithTopicConnectionFactoryAndJms11Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(TopicConnectionFactory.class);
		TopicConnectionFactory cf = (TopicConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		TopicConnection con = (TopicConnection) conControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(TopicConnectionFactory.class);
		TopicConnectionFactory cf = (TopicConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		TopicConnection con = (TopicConnection) conControl.getMock();

		cf.createTopicConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createTopicConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createTopicConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithConnectionFactoryAndClientId() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.setClientID("myId");
		conControl.setVoidCallable(1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setClientId("myId");
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithConnectionFactoryAndExceptionListener() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();

		ExceptionListener listener = new ChainedExceptionListener();
		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.setExceptionListener(listener);
		conControl.setVoidCallable(1);
		con.getExceptionListener();
		conControl.setReturnValue(listener, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setExceptionListener(listener);
		Connection con1 = scf.createConnection();
		assertEquals(listener, con1.getExceptionListener());
		con1.start();
		con1.stop();  // should be ignored
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.stop();  // should be ignored
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testWithConnectionFactoryAndReconnectOnException() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		TestConnection con = new TestConnection();

		cf.createConnection();
		cfControl.setReturnValue(con, 2);
		cfControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setReconnectOnException(true);
		Connection con1 = scf.createConnection();
		assertNull(con1.getExceptionListener());
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = scf.createConnection();
		con2.start();
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		assertEquals(2, con.getStartCount());
		assertEquals(2, con.getCloseCount());
	}

	public void testWithConnectionFactoryAndExceptionListenerAndReconnectOnException() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		TestConnection con = new TestConnection();

		TestExceptionListener listener = new TestExceptionListener();
		cf.createConnection();
		cfControl.setReturnValue(con, 2);
		cfControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setExceptionListener(listener);
		scf.setReconnectOnException(true);
		Connection con1 = scf.createConnection();
		assertSame(listener, con1.getExceptionListener());
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = scf.createConnection();
		con2.start();
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		assertEquals(2, con.getStartCount());
		assertEquals(2, con.getCloseCount());
		assertEquals(1, listener.getCount());
	}

	public void testConnectionFactory102WithQueue() throws JMSException {
		MockControl cfControl = MockControl.createControl(QueueConnectionFactory.class);
		QueueConnectionFactory cf = (QueueConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		QueueConnection con = (QueueConnection) conControl.getMock();

		cf.createQueueConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory102(cf, false);
		QueueConnection con1 = scf.createQueueConnection();
		con1.start();
		con1.close();  // should be ignored
		QueueConnection con2 = scf.createQueueConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testConnectionFactory102WithTopic() throws JMSException {
		MockControl cfControl = MockControl.createControl(TopicConnectionFactory.class);
		TopicConnectionFactory cf = (TopicConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		TopicConnection con = (TopicConnection) conControl.getMock();

		cf.createTopicConnection();
		cfControl.setReturnValue(con, 1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();

		SingleConnectionFactory scf = new SingleConnectionFactory102(cf, true);
		TopicConnection con1 = scf.createTopicConnection();
		con1.start();
		con1.close();  // should be ignored
		TopicConnection con2 = scf.createTopicConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
	}

	public void testCachingConnectionFactory() throws JMSException {
		MockControl cfControl = MockControl.createControl(ConnectionFactory.class);
		ConnectionFactory cf = (ConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(Connection.class);
		Connection con = (Connection) conControl.getMock();
		MockControl txSessionControl = MockControl.createControl(Session.class);
		Session txSession = (Session) txSessionControl.getMock();
		MockControl nonTxSessionControl = MockControl.createControl(Session.class);
		Session nonTxSession = (Session) nonTxSessionControl.getMock();

		cf.createConnection();
		cfControl.setReturnValue(con, 1);
		con.createSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(txSession, 1);
		txSession.getTransacted();
		txSessionControl.setReturnValue(true, 1);
		txSession.commit();
		txSessionControl.setVoidCallable(1);
		txSession.close();
		txSessionControl.setVoidCallable(1);
		con.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		conControl.setReturnValue(nonTxSession, 1);
		nonTxSession.close();
		nonTxSessionControl.setVoidCallable(1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();
		txSessionControl.replay();
		nonTxSessionControl.replay();

		CachingConnectionFactory scf = new CachingConnectionFactory(cf);
		scf.setReconnectOnException(false);
		Connection con1 = scf.createConnection();
		Session session1 = con1.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session1.getTransacted();
		session1.close();  // should lead to rollback
		session1 = con1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session1.close();  // should be ignored
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		Session session2 = con2.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();  // should be ignored
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.commit();
		session2.close();  // should be ignored
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
		txSessionControl.verify();
		nonTxSessionControl.verify();
	}

	public void testCachingConnectionFactoryWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(QueueConnectionFactory.class);
		QueueConnectionFactory cf = (QueueConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(QueueConnection.class);
		QueueConnection con = (QueueConnection) conControl.getMock();
		MockControl txSessionControl = MockControl.createControl(QueueSession.class);
		QueueSession txSession = (QueueSession) txSessionControl.getMock();
		MockControl nonTxSessionControl = MockControl.createControl(QueueSession.class);
		QueueSession nonTxSession = (QueueSession) nonTxSessionControl.getMock();

		cf.createQueueConnection();
		cfControl.setReturnValue(con, 1);
		con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(txSession, 1);
		txSession.getTransacted();
		txSessionControl.setReturnValue(true, 1);
		txSession.rollback();
		txSessionControl.setVoidCallable(1);
		txSession.close();
		txSessionControl.setVoidCallable(1);
		con.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		conControl.setReturnValue(nonTxSession, 1);
		nonTxSession.close();
		nonTxSessionControl.setVoidCallable(1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();
		txSessionControl.replay();
		nonTxSessionControl.replay();

		CachingConnectionFactory scf = new CachingConnectionFactory(cf);
		scf.setReconnectOnException(false);
		Connection con1 = scf.createQueueConnection();
		Session session1 = con1.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session1.rollback();
		session1.close();  // should be ignored
		session1 = con1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session1.close();  // should be ignored
		con1.start();
		con1.close();  // should be ignored
		QueueConnection con2 = scf.createQueueConnection();
		Session session2 = con2.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();  // should be ignored
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.getTransacted();
		session2.close();  // should lead to rollback
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
		txSessionControl.verify();
		nonTxSessionControl.verify();
	}

	public void testCachingConnectionFactoryWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		MockControl cfControl = MockControl.createControl(TopicConnectionFactory.class);
		TopicConnectionFactory cf = (TopicConnectionFactory) cfControl.getMock();
		MockControl conControl = MockControl.createControl(TopicConnection.class);
		TopicConnection con = (TopicConnection) conControl.getMock();
		MockControl txSessionControl = MockControl.createControl(TopicSession.class);
		TopicSession txSession = (TopicSession) txSessionControl.getMock();
		MockControl nonTxSessionControl = MockControl.createControl(TopicSession.class);
		TopicSession nonTxSession = (TopicSession) nonTxSessionControl.getMock();

		cf.createTopicConnection();
		cfControl.setReturnValue(con, 1);
		con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE);
		conControl.setReturnValue(txSession, 1);
		txSession.getTransacted();
		txSessionControl.setReturnValue(true, 2);
		txSession.close();
		txSessionControl.setVoidCallable(1);
		con.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
		conControl.setReturnValue(nonTxSession, 1);
		nonTxSession.close();
		nonTxSessionControl.setVoidCallable(1);
		con.start();
		conControl.setVoidCallable(1);
		con.stop();
		conControl.setVoidCallable(1);
		con.close();
		conControl.setVoidCallable(1);

		cfControl.replay();
		conControl.replay();
		txSessionControl.replay();
		nonTxSessionControl.replay();

		CachingConnectionFactory scf = new CachingConnectionFactory(cf);
		scf.setReconnectOnException(false);
		Connection con1 = scf.createTopicConnection();
		Session session1 = con1.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session1.getTransacted();
		session1.close();  // should lead to rollback
		session1 = con1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session1.close();  // should be ignored
		con1.start();
		con1.close();  // should be ignored
		TopicConnection con2 = scf.createTopicConnection();
		Session session2 = con2.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();  // should be ignored
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.getTransacted();
		session2.close();  // should be ignored
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		cfControl.verify();
		conControl.verify();
		txSessionControl.verify();
		nonTxSessionControl.verify();
	}

}
