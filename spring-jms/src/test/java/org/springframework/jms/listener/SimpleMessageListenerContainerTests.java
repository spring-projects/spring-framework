/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.jms.StubQueue;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Mark Fisher
 */
public class SimpleMessageListenerContainerTests extends AbstractMessageListenerContainerTests {

	private static final String DESTINATION_NAME = "foo";

	private static final String EXCEPTION_MESSAGE = "This.Is.It";

	private static final StubQueue QUEUE_DESTINATION = new StubQueue();


	private SimpleMessageListenerContainer container;


	@Before
	public void setUp() throws Exception {
		this.container = (SimpleMessageListenerContainer) getContainer();
	}

	@Override
	protected AbstractMessageListenerContainer getContainer() {
		return new SimpleMessageListenerContainer();
	}


	@Test
	public void testSessionTransactedModeReallyDoesDefaultToFalse() throws Exception {
		assertFalse("The [pubSubLocal] property of SimpleMessageListenerContainer " +
				"must default to false. Change this test (and the " +
				"attendant Javadoc) if you have changed the default.",
				container.isPubSubNoLocal());
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSettingConcurrentConsumersToZeroIsNotAllowed() throws Exception {
		container.setConcurrentConsumers(0);
		container.afterPropertiesSet();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testSettingConcurrentConsumersToANegativeValueIsNotAllowed() throws Exception {
		container.setConcurrentConsumers(-198);
		container.afterPropertiesSet();
	}

	@Test
	public void testContextRefreshedEventDoesNotStartTheConnectionIfAutoStartIsSetToFalse() throws Exception {
		MessageConsumer messageConsumer = mock(MessageConsumer.class);
		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.setAutoStartup(false);
		this.container.afterPropertiesSet();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("messageListenerContainer", this.container);
		context.refresh();

		verify(connection).setExceptionListener(this.container);
	}

	@Test
	public void testContextRefreshedEventStartsTheConnectionByDefault() throws Exception {
		MessageConsumer messageConsumer = mock(MessageConsumer.class);
		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.afterPropertiesSet();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("messageListenerContainer", this.container);
		context.refresh();

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	public void testCorrectSessionExposedForSessionAwareMessageListenerInvocation() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		final Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);
		given(session.getAcknowledgeMode()).willReturn(Session.AUTO_ACKNOWLEDGE);

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		final Set<String> failure = new HashSet<>(1);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new SessionAwareMessageListener<Message>() {
			@Override
			public void onMessage(Message message, @Nullable Session sess) {
				try {
					// Check correct Session passed into SessionAwareMessageListener.
					assertSame(sess, session);
				}
				catch (Throwable ex) {
					failure.add("MessageListener execution failed: " + ex);
				}
			}
		});

		this.container.afterPropertiesSet();
		this.container.start();

		final Message message = mock(Message.class);
		messageConsumer.sendMessage(message);

		if (!failure.isEmpty()) {
			fail(failure.iterator().next().toString());
		}

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	public void testTaskExecutorCorrectlyInvokedWhenSpecified() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		final Session session = mock(Session.class);
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		given(session.getTransacted()).willReturn(false);
		given(session.getAcknowledgeMode()).willReturn(Session.AUTO_ACKNOWLEDGE);

		Connection connection = mock(Connection.class);
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		final ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		final TestMessageListener listener = new TestMessageListener();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(listener);
		this.container.setTaskExecutor(new TaskExecutor() {
			@Override
			public void execute(Runnable task) {
				listener.executorInvoked = true;
				assertFalse(listener.listenerInvoked);
				task.run();
				assertTrue(listener.listenerInvoked);
			}
		});
		this.container.afterPropertiesSet();
		this.container.start();

		final Message message = mock(Message.class);
		messageConsumer.sendMessage(message);

		assertTrue(listener.executorInvoked);
		assertTrue(listener.listenerInvoked);

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	public void testRegisteredExceptionListenerIsInvokedOnException() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		final JMSException theException = new JMSException(EXCEPTION_MESSAGE);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new SessionAwareMessageListener<Message>() {
			@Override
			public void onMessage(Message message, @Nullable Session session) throws JMSException {
				throw theException;
			}
		});

		ExceptionListener exceptionListener = mock(ExceptionListener.class);

		this.container.setExceptionListener(exceptionListener);
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock(Message.class);

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);


		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
		verify(exceptionListener).onException(theException);
	}

	@Test
	public void testRegisteredErrorHandlerIsInvokedOnException() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock(Session.class);

		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		final IllegalStateException theException = new IllegalStateException("intentional test failure");

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new SessionAwareMessageListener<Message>() {
			@Override
			public void onMessage(Message message, @Nullable Session session) throws JMSException {
				throw theException;
			}
		});

		ErrorHandler errorHandler = mock(ErrorHandler.class);
		this.container.setErrorHandler(errorHandler);
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		Message message = mock(Message.class);

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
		verify(errorHandler).handleError(theException);
	}

	@Test
	public void testNoRollbackOccursIfSessionIsNotTransactedAndThatExceptionsDo_NOT_Propagate() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				throw new UnsupportedOperationException();
			}
		});
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock(Message.class);

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	public void testTransactedSessionsGetRollbackLogicAppliedAndThatExceptionsStillDo_NOT_Propagate() throws Exception {
		this.container.setSessionTransacted(true);

		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(true);

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(Message message) {
				throw new UnsupportedOperationException();
			}
		});
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock(Message.class);

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		// Session is rolled back 'cos it is transacted...
		verify(session).rollback();
		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	public void testDestroyClosesConsumersSessionsAndConnectionInThatOrder() throws Exception {
		MessageConsumer messageConsumer = mock(MessageConsumer.class);
		Session session = mock(Session.class);
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer); // no MessageSelector...

		Connection connection = mock(Connection.class);
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock(ConnectionFactory.class);
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.afterPropertiesSet();
		this.container.start();
		this.container.destroy();

		verify(messageConsumer).close();
		verify(session).close();
		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
		verify(connection).close();
	}


	private static class TestMessageListener implements MessageListener {

		public boolean executorInvoked = false;

		public boolean listenerInvoked = false;

		@Override
		public void onMessage(Message message) {
			this.listenerInvoked = true;
		}
	}


	private static class SimpleMessageConsumer implements MessageConsumer {

		private MessageListener messageListener;

		public void sendMessage(Message message) throws JMSException {
			this.messageListener.onMessage(message);
		}

		@Override
		public String getMessageSelector() throws JMSException {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageListener getMessageListener() throws JMSException {
			return this.messageListener;
		}

		@Override
		public void setMessageListener(MessageListener messageListener) throws JMSException {
			this.messageListener = messageListener;
		}

		@Override
		public Message receive() throws JMSException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Message receive(long l) throws JMSException {
			throw new UnsupportedOperationException();
		}

		@Override
		public Message receiveNoWait() throws JMSException {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() throws JMSException {
			throw new UnsupportedOperationException();
		}
	}

}
