/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.jms.listener;

import java.util.HashSet;
import java.util.Set;

import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.ExceptionListener;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageListener;
import jakarta.jms.Session;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.jms.StubQueue;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * @author Rick Evans
 * @author Juergen Hoeller
 * @author Chris Beams
 * @author Mark Fisher
 */
class SimpleMessageListenerContainerTests {

	private static final String DESTINATION_NAME = "foo";

	private static final String EXCEPTION_MESSAGE = "This.Is.It";

	private static final StubQueue QUEUE_DESTINATION = new StubQueue();

	private final SimpleMessageListenerContainer container = new SimpleMessageListenerContainer();


	@Test
	void testSettingMessageListenerToANullType() {
		this.container.setMessageListener(null);
		assertThat(this.container.getMessageListener()).isNull();
	}

	@Test
	void testSettingMessageListenerToAnUnsupportedType() {
		assertThatIllegalArgumentException().isThrownBy(() ->
				this.container.setMessageListener("Bingo"));
	}

	@Test
	void testSessionTransactedModeReallyDoesDefaultToFalse() {
		assertThat(this.container.isPubSubNoLocal()).as("The [pubSubLocal] property of SimpleMessageListenerContainer " +
				"must default to false. Change this test (and the attendant javadoc) if you have changed the default.").isFalse();
	}

	@Test
	void testSettingConcurrentConsumersToZeroIsNotAllowed() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
				this.container.setConcurrentConsumers(0);
				this.container.afterPropertiesSet();
		});
	}

	@Test
	void testSettingConcurrentConsumersToANegativeValueIsNotAllowed() {
		assertThatIllegalArgumentException().isThrownBy(() -> {
				this.container.setConcurrentConsumers(-198);
				this.container.afterPropertiesSet();
		});
	}

	@Test
	void testContextRefreshedEventDoesNotStartTheConnectionIfAutoStartIsSetToFalse() throws Exception {
		MessageConsumer messageConsumer = mock();
		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.setAutoStartup(false);
		this.container.afterPropertiesSet();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("messageListenerContainer", this.container);
		context.refresh();
		context.close();

		verify(connection).setExceptionListener(this.container);
	}

	@Test
	void testContextRefreshedEventStartsTheConnectionByDefault() throws Exception {
		MessageConsumer messageConsumer = mock();
		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);

		this.container.setMessageListener(new TestMessageListener());
		this.container.afterPropertiesSet();
		GenericApplicationContext context = new GenericApplicationContext();
		context.getBeanFactory().registerSingleton("messageListenerContainer", this.container);
		context.refresh();
		context.close();

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	void testCorrectSessionExposedForSessionAwareMessageListenerInvocation() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		final Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);
		given(session.getAcknowledgeMode()).willReturn(Session.AUTO_ACKNOWLEDGE);

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		final ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		final Set<String> failure = new HashSet<>(1);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener((SessionAwareMessageListener<Message>) (Message message, @Nullable Session sess) -> {
			try {
				// Check correct Session passed into SessionAwareMessageListener.
				assertThat(session).isSameAs(sess);
			}
			catch (Throwable ex) {
				failure.add("MessageListener execution failed: " + ex);
			}
		});

		this.container.afterPropertiesSet();
		this.container.start();

		final Message message = mock();
		messageConsumer.sendMessage(message);

		if (!failure.isEmpty()) {
			fail(failure.iterator().next().toString());
		}

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	void testTaskExecutorCorrectlyInvokedWhenSpecified() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		final Session session = mock();
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		given(session.getTransacted()).willReturn(false);
		given(session.getAcknowledgeMode()).willReturn(Session.AUTO_ACKNOWLEDGE);

		Connection connection = mock();
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		final ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		final TestMessageListener listener = new TestMessageListener();

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener(listener);
		this.container.setTaskExecutor(task -> {
			listener.executorInvoked = true;
			assertThat(listener.listenerInvoked).isFalse();
			task.run();
			assertThat(listener.listenerInvoked).isTrue();
		});
		this.container.afterPropertiesSet();
		this.container.start();

		final Message message = mock();
		messageConsumer.sendMessage(message);

		assertThat(listener.executorInvoked).isTrue();
		assertThat(listener.listenerInvoked).isTrue();

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	void testRegisteredExceptionListenerIsInvokedOnException() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		final JMSException theException = new JMSException(EXCEPTION_MESSAGE);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener((SessionAwareMessageListener<Message>) (Message message, @Nullable Session session1) -> {
			throw theException;
		});

		ExceptionListener exceptionListener = mock();

		this.container.setExceptionListener(exceptionListener);
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);


		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
		verify(exceptionListener).onException(theException);
	}

	@Test
	void testRegisteredErrorHandlerIsInvokedOnException() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock();

		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		final IllegalStateException theException = new IllegalStateException("intentional test failure");

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener((SessionAwareMessageListener<Message>) (Message message, @Nullable Session session1) -> {
			throw theException;
		});

		ErrorHandler errorHandler = mock();
		this.container.setErrorHandler(errorHandler);
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		Message message = mock();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
		verify(errorHandler).handleError(theException);
	}

	@Test
	void testNoRollbackOccursIfSessionIsNotTransactedAndThatExceptionsDo_NOT_Propagate() throws Exception {
		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(false);

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener((MessageListener) message -> {
			throw new UnsupportedOperationException();
		});
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	void testTransactedSessionsGetRollbackLogicAppliedAndThatExceptionsStillDo_NOT_Propagate() throws Exception {
		this.container.setSessionTransacted(true);

		final SimpleMessageConsumer messageConsumer = new SimpleMessageConsumer();

		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...
		// an exception is thrown, so the rollback logic is being applied here...
		given(session.getTransacted()).willReturn(true);

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock();
		given(connectionFactory.createConnection()).willReturn(connection);

		this.container.setConnectionFactory(connectionFactory);
		this.container.setDestinationName(DESTINATION_NAME);
		this.container.setMessageListener((MessageListener) message -> {
			throw new UnsupportedOperationException();
		});
		this.container.afterPropertiesSet();
		this.container.start();

		// manually trigger an Exception with the above bad MessageListener...
		final Message message = mock();

		// a Throwable from a MessageListener MUST simply be swallowed...
		messageConsumer.sendMessage(message);

		// Session is rolled back because it is transacted...
		verify(session).rollback();
		verify(connection).setExceptionListener(this.container);
		verify(connection).start();
	}

	@Test
	void testDestroyClosesConsumersSessionsAndConnectionInThatOrder() throws Exception {
		MessageConsumer messageConsumer = mock();
		Session session = mock();
		// Queue gets created in order to create MessageConsumer for that Destination...
		given(session.createQueue(DESTINATION_NAME)).willReturn(QUEUE_DESTINATION);
		// and then the MessageConsumer gets created...
		given(session.createConsumer(QUEUE_DESTINATION, null)).willReturn(messageConsumer);  // no MessageSelector...

		Connection connection = mock();
		// session gets created in order to register MessageListener...
		given(connection.createSession(this.container.isSessionTransacted(),
				this.container.getSessionAcknowledgeMode())).willReturn(session);
		// and the connection is start()ed after the listener is registered...

		ConnectionFactory connectionFactory = mock();
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

		public void sendMessage(Message message) {
			this.messageListener.onMessage(message);
		}

		@Override
		public String getMessageSelector() {
			throw new UnsupportedOperationException();
		}

		@Override
		public MessageListener getMessageListener() {
			return this.messageListener;
		}

		@Override
		public void setMessageListener(MessageListener messageListener) {
			this.messageListener = messageListener;
		}

		@Override
		public Message receive() {
			throw new UnsupportedOperationException();
		}

		@Override
		public Message receive(long l) {
			throw new UnsupportedOperationException();
		}

		@Override
		public Message receiveNoWait() {
			throw new UnsupportedOperationException();
		}

		@Override
		public void close() {
			throw new UnsupportedOperationException();
		}
	}

}
