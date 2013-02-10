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

package org.springframework.jms.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

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

import org.junit.Test;

/**
 * @author Juergen Hoeller
 * @since 26.07.2004
 */
public class SingleConnectionFactoryTests {

	@Test
	public void testWithConnection() throws JMSException {
		Connection con = mock(Connection.class);

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

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithQueueConnection() throws JMSException {
		Connection con = mock(QueueConnection.class);

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

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithTopicConnection() throws JMSException {
		Connection con = mock(TopicConnection.class);

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

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithConnectionFactory() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithQueueConnectionFactoryAndJms11Usage() throws JMSException {
		QueueConnectionFactory cf = mock(QueueConnectionFactory.class);
		QueueConnection con = mock(QueueConnection.class);

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		QueueConnectionFactory cf = mock(QueueConnectionFactory.class);
		QueueConnection con = mock(QueueConnection.class);

		given(cf.createQueueConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createQueueConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createQueueConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithTopicConnectionFactoryAndJms11Usage() throws JMSException {
		TopicConnectionFactory cf = mock(TopicConnectionFactory.class);
		TopicConnection con = mock(TopicConnection.class);

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		TopicConnectionFactory cf = mock(TopicConnectionFactory.class);
		TopicConnection con = mock(TopicConnection.class);

		given(cf.createTopicConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createTopicConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createTopicConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithConnectionFactoryAndClientId() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setClientId("myId");
		Connection con1 = scf.createConnection();
		con1.start();
		con1.close();  // should be ignored
		Connection con2 = scf.createConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).setClientID("myId");
		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testWithConnectionFactoryAndExceptionListener() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);

		ExceptionListener listener = new ChainedExceptionListener();
		given(cf.createConnection()).willReturn(con);
		given(con.getExceptionListener()).willReturn(listener);

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

		verify(con).setExceptionListener(listener);
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

	@Test
	public void testWithConnectionFactoryAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		TestConnection con = new TestConnection();

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setReconnectOnException(true);
		Connection con1 = scf.createConnection();
		assertNull(con1.getExceptionListener());
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = scf.createConnection();
		con2.start();
		scf.destroy();  // should trigger actual close

		assertEquals(2, con.getStartCount());
		assertEquals(2, con.getCloseCount());
	}

	@Test
	public void testWithConnectionFactoryAndExceptionListenerAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		TestConnection con = new TestConnection();

		given(cf.createConnection()).willReturn(con);

		TestExceptionListener listener = new TestExceptionListener();
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

		assertEquals(2, con.getStartCount());
		assertEquals(2, con.getCloseCount());
		assertEquals(1, listener.getCount());
	}

	@Test
	public void testConnectionFactory102WithQueue() throws JMSException {
		QueueConnectionFactory cf = mock(QueueConnectionFactory.class);
		QueueConnection con = mock(QueueConnection.class);

		given(cf.createQueueConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory102(cf, false);
		QueueConnection con1 = scf.createQueueConnection();
		con1.start();
		con1.close();  // should be ignored
		QueueConnection con2 = scf.createQueueConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testConnectionFactory102WithTopic() throws JMSException {
		TopicConnectionFactory cf = mock(TopicConnectionFactory.class);
		TopicConnection con = mock(TopicConnection.class);

		given(cf.createTopicConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory102(cf, true);
		TopicConnection con1 = scf.createTopicConnection();
		con1.start();
		con1.close();  // should be ignored
		TopicConnection con2 = scf.createTopicConnection();
		con2.start();
		con2.close();  // should be ignored
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	public void testCachingConnectionFactory() throws JMSException {
		ConnectionFactory cf = mock(ConnectionFactory.class);
		Connection con = mock(Connection.class);
		Session txSession = mock(Session.class);
		Session nonTxSession = mock(Session.class);

		given(cf.createConnection()).willReturn(con);
		given(con.createSession(true, Session.AUTO_ACKNOWLEDGE)).willReturn(txSession);
		given(txSession.getTransacted()).willReturn(true);
		given(con.createSession(false, Session.CLIENT_ACKNOWLEDGE)).willReturn(nonTxSession);

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

		verify(txSession).commit();
		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

	@Test
	public void testCachingConnectionFactoryWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		QueueConnectionFactory cf = mock(QueueConnectionFactory.class);
		QueueConnection con = mock(QueueConnection.class);
		QueueSession txSession = mock(QueueSession.class);
		QueueSession nonTxSession = mock(QueueSession.class);

		given(cf.createQueueConnection()).willReturn(con);
		given(con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE)).willReturn(txSession);
		given(txSession.getTransacted()).willReturn(true);
		given(con.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)).willReturn(nonTxSession);

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

		verify(txSession).rollback();
		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

	@Test
	public void testCachingConnectionFactoryWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		TopicConnectionFactory cf = mock(TopicConnectionFactory.class);
		TopicConnection con = mock(TopicConnection.class);
		TopicSession txSession = mock(TopicSession.class);
		TopicSession nonTxSession = mock(TopicSession.class);

		given(cf.createTopicConnection()).willReturn(con);
		given(con.createTopicSession(true, Session.AUTO_ACKNOWLEDGE)).willReturn(txSession);
		given(txSession.getTransacted()).willReturn(true);
		given(con.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE)).willReturn(nonTxSession);

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

		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

}
