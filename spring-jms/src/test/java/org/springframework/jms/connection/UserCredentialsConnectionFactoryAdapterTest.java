package org.springframework.jms.connection;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.mockito.Mockito;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.QueueConnectionFactory;
import javax.jms.TopicConnectionFactory;

/**
 * Tests for {@link UserCredentialsConnectionFactoryAdapter}.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("UserCredentialsConnectionFactoryAdapter")
class UserCredentialsConnectionFactoryAdapterTest {

	private static final String SAMPLE_USERNAME = "username";
	private static final String SAMPLE_PASSWORD = "password";

	@Test
	@DisplayName("Uses username and password, when provided")
	void connectionFactoryUsesUsernames() throws JMSException {
		final UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setUsername(SAMPLE_USERNAME);
		adapter.setPassword(SAMPLE_PASSWORD);
		final QTConnectionFactory connectionFactory = Mockito.mock(QTConnectionFactory.class);
		adapter.setTargetConnectionFactory(connectionFactory);

		adapter.createConnection();
		Mockito.verify(connectionFactory).createConnection(SAMPLE_USERNAME, SAMPLE_PASSWORD);
		adapter.createContext();
		Mockito.verify(connectionFactory).createContext(SAMPLE_USERNAME, SAMPLE_PASSWORD, JMSContext.AUTO_ACKNOWLEDGE);
		adapter.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
		Mockito.verify(connectionFactory).createContext(SAMPLE_USERNAME, SAMPLE_PASSWORD, JMSContext.CLIENT_ACKNOWLEDGE);
		adapter.createQueueConnection();
		Mockito.verify(connectionFactory).createQueueConnection(SAMPLE_USERNAME, SAMPLE_PASSWORD);
		adapter.createTopicConnection();
		Mockito.verify(connectionFactory).createTopicConnection(SAMPLE_USERNAME, SAMPLE_PASSWORD);
	}

	@Test
	@DisplayName("Overridden credentials are used.")
	void credentialsOverridden() throws JMSException {
		final String usernameOverride = "1";
		final String passwordOverride = "2";
		final UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setUsername(SAMPLE_USERNAME);
		adapter.setPassword(SAMPLE_PASSWORD);
		final QTConnectionFactory connectionFactory = Mockito.mock(QTConnectionFactory.class);
		adapter.setTargetConnectionFactory(connectionFactory);

		adapter.createConnection(usernameOverride, passwordOverride);
		Mockito.verify(connectionFactory).createConnection(usernameOverride, passwordOverride);
		adapter.createContext(usernameOverride, passwordOverride);
		Mockito.verify(connectionFactory).createContext(usernameOverride, passwordOverride);
		adapter.createContext(usernameOverride, passwordOverride, JMSContext.CLIENT_ACKNOWLEDGE);
		Mockito.verify(connectionFactory).createContext(usernameOverride, passwordOverride, JMSContext.CLIENT_ACKNOWLEDGE);
		adapter.createQueueConnection(usernameOverride, passwordOverride);
		Mockito.verify(connectionFactory).createQueueConnection(usernameOverride, passwordOverride);
		adapter.createTopicConnection(usernameOverride, passwordOverride);
		Mockito.verify(connectionFactory).createTopicConnection(usernameOverride, passwordOverride);
	}

	@Test
	@DisplayName("Does not use password, when username is not specified")
	void connectionFactoryNoUsernameSpecified() throws JMSException {
		final UserCredentialsConnectionFactoryAdapter adapter = new UserCredentialsConnectionFactoryAdapter();
		adapter.setPassword(SAMPLE_PASSWORD);
		final QTConnectionFactory connectionFactory = Mockito.mock(QTConnectionFactory.class);
		adapter.setTargetConnectionFactory(connectionFactory);
		adapter.createConnection();
		Mockito.verify(connectionFactory).createConnection();
		adapter.createContext();
		Mockito.verify(connectionFactory).createContext(JMSContext.AUTO_ACKNOWLEDGE);
		adapter.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
		Mockito.verify(connectionFactory).createContext(JMSContext.CLIENT_ACKNOWLEDGE);
		adapter.createQueueConnection();
		Mockito.verify(connectionFactory).createQueueConnection();
		adapter.createTopicConnection();
		Mockito.verify(connectionFactory).createTopicConnection();
	}

	/**
	 * Interface to simplify the testing.
	 */
	private interface QTConnectionFactory extends QueueConnectionFactory, TopicConnectionFactory {}
}
