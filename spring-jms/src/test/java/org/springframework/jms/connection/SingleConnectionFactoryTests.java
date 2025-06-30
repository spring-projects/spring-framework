/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.jms.connection;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicInteger;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.QueueConnection;
import jakarta.jms.QueueConnectionFactory;
import jakarta.jms.QueueSession;
import jakarta.jms.Session;
import jakarta.jms.TopicConnection;
import jakarta.jms.TopicConnectionFactory;
import jakarta.jms.TopicSession;
import org.junit.jupiter.api.Test;

import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * @author Juergen Hoeller
 * @since 26.07.2004
 */
class SingleConnectionFactoryTests {

	@Test
	void testWithConnection() throws JMSException {
		Connection con = mock();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		Connection con1 = scf.createConnection();
		con1.start();
		con1.stop();
		con1.close();
		Connection con2 = scf.createConnection();
		con2.start();
		con2.stop();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithQueueConnection() throws JMSException {
		QueueConnection con = mock();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		QueueConnection con1 = scf.createQueueConnection();
		con1.start();
		con1.stop();
		con1.close();
		QueueConnection con2 = scf.createQueueConnection();
		con2.start();
		con2.stop();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithTopicConnection() throws JMSException {
		TopicConnection con = mock();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		TopicConnection con1 = scf.createTopicConnection();
		con1.start();
		con1.stop();
		con1.close();
		TopicConnection con2 = scf.createTopicConnection();
		con2.start();
		con2.stop();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithConnectionFactory() throws JMSException {
		ConnectionFactory cf = mock();
		Connection con = mock();

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		Connection con2 = scf.createConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithQueueConnectionFactoryAndJms11Usage() throws JMSException {
		QueueConnectionFactory cf = mock();
		QueueConnection con = mock();

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		Connection con2 = scf.createConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		QueueConnectionFactory cf = mock();
		QueueConnection con = mock();

		given(cf.createQueueConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createQueueConnection();
		Connection con2 = scf.createQueueConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithTopicConnectionFactoryAndJms11Usage() throws JMSException {
		TopicConnectionFactory cf = mock();
		TopicConnection con = mock();

		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createConnection();
		Connection con2 = scf.createConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		TopicConnectionFactory cf = mock();
		TopicConnection con = mock();

		given(cf.createTopicConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		Connection con1 = scf.createTopicConnection();
		Connection con2 = scf.createTopicConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithConnectionAggregatedStartStop() throws JMSException {
		Connection con = mock();

		SingleConnectionFactory scf = new SingleConnectionFactory(con);
		Connection con1 = scf.createConnection();
		con1.start();
		verify(con).start();
		con1.stop();
		verify(con).stop();
		Connection con2 = scf.createConnection();
		con2.start();
		verify(con, times(2)).start();
		con2.stop();
		verify(con, times(2)).stop();
		con2.start();
		verify(con, times(3)).start();
		con1.start();
		con2.stop();
		con1.stop();
		verify(con, times(3)).stop();
		con1.start();
		verify(con, times(4)).start();
		con1.close();
		verify(con, times(4)).stop();
		con2.close();
		scf.destroy();
		verify(con).close();

		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithConnectionFactoryAndClientId() throws JMSException {
		ConnectionFactory cf = mock();
		Connection con = mock();
		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setClientId("myId");
		Connection con1 = scf.createConnection();
		Connection con2 = scf.createConnection();
		con1.start();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).setClientID("myId");
		verify(con).start();
		verify(con).stop();
		verify(con).close();
		verifyNoMoreInteractions(con);
	}

	@Test
	void testWithConnectionFactoryAndExceptionListener() throws JMSException {
		ConnectionFactory cf = mock();
		Connection con = mock();

		ExceptionListener listener = new ChainedExceptionListener();
		given(cf.createConnection()).willReturn(con);
		given(con.getExceptionListener()).willReturn(listener);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setExceptionListener(listener);
		Connection con1 = scf.createConnection();
		assertThat(con1.getExceptionListener()).isEqualTo(listener);
		con1.start();
		con1.stop();
		con1.close();
		Connection con2 = scf.createConnection();
		con2.start();
		con2.stop();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(con).setExceptionListener(listener);
		verify(con, times(2)).start();
		verify(con, times(2)).stop();
		verify(con).close();
	}

	@Test
	void testWithConnectionFactoryAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock();
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setReconnectOnException(true);
		Connection con1 = scf.createConnection();
		assertThat(con1.getExceptionListener()).isNull();
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = scf.createConnection();
		con2.start();
		scf.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(2);
		assertThat(con.getCloseCount()).isEqualTo(2);
	}

	@Test
	void testWithConnectionFactoryAndExceptionListenerAndReconnectOnException() throws JMSException {
		ConnectionFactory cf = mock();
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		TestExceptionListener listener = new TestExceptionListener();
		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setExceptionListener(listener);
		scf.setReconnectOnException(true);
		Connection con1 = scf.createConnection();
		assertThat(con1.getExceptionListener()).isSameAs(listener);
		con1.start();
		con.getExceptionListener().onException(new JMSException(""));
		Connection con2 = scf.createConnection();
		con2.start();
		scf.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(2);
		assertThat(con.getCloseCount()).isEqualTo(2);
		assertThat(listener.getCount()).isEqualTo(1);
	}

	@Test
	void testWithConnectionFactoryAndExceptionListenerAndReconnectOnExceptionWithJMSException() throws Exception {
		// Throws JMSException on setExceptionListener() method, but only at the first time
		class FailingTestConnection extends TestConnection {
			private int setExceptionListenerInvocationCounter;

			@Override
			public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
				setExceptionListenerInvocationCounter++;
				// Throw JMSException on first invocation
				if (setExceptionListenerInvocationCounter == 1) {
					throw new JMSException("Test JMSException (setExceptionListener())");
				}
				super.setExceptionListener(exceptionListener);
			}
		}

		// Prepare base JMS ConnectionFactory
		// - createConnection(1st) -> TestConnection,
		// - createConnection(2nd and next) -> FailingTestConnection
		FailingTestConnection failingCon = new FailingTestConnection();
		AtomicInteger createConnectionMethodCounter = new AtomicInteger();
		ConnectionFactory cf = mock(ConnectionFactory.class);
		given(cf.createConnection()).willAnswer(invocation -> {
			int methodInvocationCounter = createConnectionMethodCounter.incrementAndGet();
			return (methodInvocationCounter >= 4 ? failingCon : new TestConnection());
		});

		// Prepare SingleConnectionFactory (setReconnectOnException())
		// - internal connection exception listener should be registered
		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setReconnectOnException(true);
		Field conField = ReflectionUtils.findField(SingleConnectionFactory.class, "connection");
		conField.setAccessible(true);
		assertThat(scf.isRunning()).isFalse();

		// Get connection (1st)
		Connection con1 = scf.getConnection();
		assertThat(createConnectionMethodCounter.get()).isEqualTo(1);
		assertThat(con1.getExceptionListener()).isNotNull();
		assertThat(con1).isSameAs(conField.get(scf));
		assertThat(scf.isRunning()).isTrue();

		// Get connection again, the same should be returned (shared connection till some problem)
		Connection con2 = scf.getConnection();
		assertThat(createConnectionMethodCounter.get()).isEqualTo(1);
		assertThat(con2.getExceptionListener()).isNotNull();
		assertThat(con2).isSameAs(con1);
		assertThat(scf.isRunning()).isTrue();

		// Explicit stop should reset connection
		scf.stop();
		assertThat(conField.get(scf)).isNull();
		assertThat(scf.isRunning()).isFalse();
		Connection con3 = scf.getConnection();
		assertThat(createConnectionMethodCounter.get()).isEqualTo(2);
		assertThat(con3.getExceptionListener()).isNotNull();
		assertThat(con3).isNotSameAs(con2);
		assertThat(scf.isRunning()).isTrue();

		// Explicit stop-and-restart should refresh connection
		scf.stop();
		assertThat(conField.get(scf)).isNull();
		assertThat(scf.isRunning()).isFalse();
		scf.start();
		assertThat(scf.isRunning()).isTrue();
		assertThat(conField.get(scf)).isNotNull();
		Connection con4 = scf.getConnection();
		assertThat(createConnectionMethodCounter.get()).isEqualTo(3);
		assertThat(con4.getExceptionListener()).isNotNull();
		assertThat(con4).isNotSameAs(con3);

		// Invoke reset connection to simulate problem with connection
		// - SCF exception listener should be invoked -> connection should be set to null
		// - next attempt to invoke getConnection() must create new connection
		scf.resetConnection();
		assertThat(conField.get(scf)).isNull();

		// Attempt to get connection again
		// - JMSException should be returned from FailingTestConnection
		// - connection should be still null (no new connection without exception listener like before fix)
		assertThatExceptionOfType(JMSException.class).isThrownBy(() -> scf.getConnection());
		assertThat(createConnectionMethodCounter.get()).isEqualTo(4);
		assertThat(conField.get(scf)).isNull();

		// Attempt to get connection again -> FailingTestConnection should be returned
		// - no JMSException is thrown, exception listener should be present
		Connection con5 = scf.getConnection();
		assertThat(createConnectionMethodCounter.get()).isEqualTo(5);
		assertThat(con5).isNotNull();
		assertThat(con5).isSameAs(failingCon);
		assertThat(con5.getExceptionListener()).isNotNull();
		assertThat(con5).isNotSameAs(con4);

		scf.destroy();
		assertThat(conField.get(scf)).isNull();
		assertThat(scf.isRunning()).isFalse();
	}

	@Test
	void testWithConnectionFactoryAndLocalExceptionListenerWithCleanup() throws JMSException {
		ConnectionFactory cf = mock();
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		TestExceptionListener listener0 = new TestExceptionListener();
		TestExceptionListener listener1 = new TestExceptionListener();
		TestExceptionListener listener2 = new TestExceptionListener();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf) {
			@Override
			public void onException(JMSException ex) {
				// no-op
			}
		};
		scf.setReconnectOnException(true);
		scf.setExceptionListener(listener0);
		Connection con1 = scf.createConnection();
		con1.setExceptionListener(listener1);
		assertThat(con1.getExceptionListener()).isSameAs(listener1);
		Connection con2 = scf.createConnection();
		con2.setExceptionListener(listener2);
		assertThat(con2.getExceptionListener()).isSameAs(listener2);
		con.getExceptionListener().onException(new JMSException(""));
		con2.close();
		con.getExceptionListener().onException(new JMSException(""));
		con1.close();
		con.getExceptionListener().onException(new JMSException(""));
		scf.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(0);
		assertThat(con.getCloseCount()).isEqualTo(1);
		assertThat(listener0.getCount()).isEqualTo(3);
		assertThat(listener1.getCount()).isEqualTo(2);
		assertThat(listener2.getCount()).isEqualTo(1);
	}

	@Test
	void testWithConnectionFactoryAndLocalExceptionListenerWithReconnect() throws JMSException {
		ConnectionFactory cf = mock();
		TestConnection con = new TestConnection();
		given(cf.createConnection()).willReturn(con);

		TestExceptionListener listener0 = new TestExceptionListener();
		TestExceptionListener listener1 = new TestExceptionListener();
		TestExceptionListener listener2 = new TestExceptionListener();

		SingleConnectionFactory scf = new SingleConnectionFactory(cf);
		scf.setReconnectOnException(true);
		scf.setExceptionListener(listener0);
		Connection con1 = scf.createConnection();
		con1.setExceptionListener(listener1);
		assertThat(con1.getExceptionListener()).isSameAs(listener1);
		con1.start();
		Connection con2 = scf.createConnection();
		con2.setExceptionListener(listener2);
		assertThat(con2.getExceptionListener()).isSameAs(listener2);
		con.getExceptionListener().onException(new JMSException(""));
		con2.close();
		con1.getMetaData();
		con.getExceptionListener().onException(new JMSException(""));
		con1.close();
		scf.destroy();  // should trigger actual close

		assertThat(con.getStartCount()).isEqualTo(2);
		assertThat(con.getCloseCount()).isEqualTo(2);
		assertThat(listener0.getCount()).isEqualTo(2);
		assertThat(listener1.getCount()).isEqualTo(2);
		assertThat(listener2.getCount()).isEqualTo(1);
	}

	@Test
	void testCachingConnectionFactory() throws JMSException {
		ConnectionFactory cf = mock();
		Connection con = mock();
		Session txSession = mock();
		Session nonTxSession = mock();

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
		session1.close();
		con1.start();
		Connection con2 = scf.createConnection();
		Session session2 = con2.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.commit();
		session2.close();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(txSession).commit();
		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

	@Test
	void testCachingConnectionFactoryWithQueueConnectionFactoryAndJms102Usage() throws JMSException {
		QueueConnectionFactory cf = mock();
		QueueConnection con = mock();
		QueueSession txSession = mock();
		QueueSession nonTxSession = mock();

		given(cf.createQueueConnection()).willReturn(con);
		given(con.createQueueSession(true, Session.AUTO_ACKNOWLEDGE)).willReturn(txSession);
		given(txSession.getTransacted()).willReturn(true);
		given(con.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE)).willReturn(nonTxSession);

		CachingConnectionFactory scf = new CachingConnectionFactory(cf);
		scf.setReconnectOnException(false);
		Connection con1 = scf.createQueueConnection();
		Session session1 = con1.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session1.rollback();
		session1.close();
		session1 = con1.createSession(false, Session.CLIENT_ACKNOWLEDGE);
		session1.close();
		con1.start();
		QueueConnection con2 = scf.createQueueConnection();
		Session session2 = con2.createQueueSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.getTransacted();
		session2.close();  // should lead to rollback
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(txSession).rollback();
		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

	@Test
	void testCachingConnectionFactoryWithTopicConnectionFactoryAndJms102Usage() throws JMSException {
		TopicConnectionFactory cf = mock();
		TopicConnection con = mock();
		TopicSession txSession = mock();
		TopicSession nonTxSession = mock();

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
		session1.close();
		con1.start();
		TopicConnection con2 = scf.createTopicConnection();
		Session session2 = con2.createTopicSession(false, Session.CLIENT_ACKNOWLEDGE);
		session2.close();
		session2 = con2.createSession(true, Session.AUTO_ACKNOWLEDGE);
		session2.getTransacted();
		session2.close();
		con2.start();
		con1.close();
		con2.close();
		scf.destroy();  // should trigger actual close

		verify(txSession).close();
		verify(nonTxSession).close();
		verify(con).start();
		verify(con).stop();
		verify(con).close();
	}

}
