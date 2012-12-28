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

package org.springframework.jms.core;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.QueueBrowser;
import javax.jms.Session;

import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;

/**
 * Helper class that simplifies synchronous JMS access code.
 *
 * <p>If you want to use dynamic destination creation, you must specify
 * the type of JMS destination to create, using the "pubSubDomain" property.
 * For other operations, this is not necessary, in contrast to when working
 * with {@link JmsTemplate102}. Point-to-Point (Queues) is the default domain.
 *
 * <p>Default settings for JMS Sessions are "not transacted" and "auto-acknowledge".
 * As defined by the J2EE specification, the transaction and acknowledgement
 * parameters are ignored when a JMS Session is created inside an active
 * transaction, no matter if a JTA transaction or a Spring-managed transaction.
 * To configure them for native JMS usage, specify appropriate values for
 * the "sessionTransacted" and "sessionAcknowledgeMode" bean properties.
 *
 * <p>This template uses a
 * {@link org.springframework.jms.support.destination.DynamicDestinationResolver}
 * and a {@link org.springframework.jms.support.converter.SimpleMessageConverter}
 * as default strategies for resolving a destination name or converting a message,
 * respectively. These defaults can be overridden through the "destinationResolver"
 * and "messageConverter" bean properties.
 *
 * <p><b>NOTE: The {@code ConnectionFactory} used with this template should
 * return pooled Connections (or a single shared Connection) as well as pooled
 * Sessions and MessageProducers. Otherwise, performance of ad-hoc JMS operations
 * is going to suffer.</b> The simplest option is to use the Spring-provided
 * {@link org.springframework.jms.connection.SingleConnectionFactory} as a
 * decorator for your target {@code ConnectionFactory}, reusing a single
 * JMS Connection in a thread-safe fashion; this is often good enough for the
 * purpose of sending messages via this template. In a J2EE environment,
 * make sure that the {@code ConnectionFactory} is obtained from the
 * application's environment naming context via JNDI; application servers
 * typically expose pooled, transaction-aware factories there.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @since 1.1
 * @see #setConnectionFactory
 * @see #setPubSubDomain
 * @see #setDestinationResolver
 * @see #setMessageConverter
 * @see javax.jms.MessageProducer
 * @see javax.jms.MessageConsumer
 */
public class JmsTemplate extends JmsDestinationAccessor implements JmsOperations {

	/**
	 * Timeout value indicating that a receive operation should
	 * check if a message is immediately available without blocking.
	 */
	public static final long RECEIVE_TIMEOUT_NO_WAIT = -1;

	/**
	 * Timeout value indicating a blocking receive without timeout.
	 */
	public static final long RECEIVE_TIMEOUT_INDEFINITE_WAIT = 0;


	/** Internal ResourceFactory adapter for interacting with ConnectionFactoryUtils */
	private final JmsTemplateResourceFactory transactionalResourceFactory = new JmsTemplateResourceFactory();


	private Object defaultDestination;

	private MessageConverter messageConverter;


	private boolean messageIdEnabled = true;

	private boolean messageTimestampEnabled = true;

	private boolean pubSubNoLocal = false;

	private long receiveTimeout = RECEIVE_TIMEOUT_INDEFINITE_WAIT;


	private boolean explicitQosEnabled = false;

	private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

	private int priority = Message.DEFAULT_PRIORITY;

	private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;


	/**
	 * Create a new JmsTemplate for bean-style usage.
	 * <p>Note: The ConnectionFactory has to be set before using the instance.
	 * This constructor can be used to prepare a JmsTemplate via a BeanFactory,
	 * typically setting the ConnectionFactory via setConnectionFactory.
	 * @see #setConnectionFactory
	 */
	public JmsTemplate() {
		initDefaultStrategies();
	}

	/**
	 * Create a new JmsTemplate, given a ConnectionFactory.
	 * @param connectionFactory the ConnectionFactory to obtain Connections from
	 */
	public JmsTemplate(ConnectionFactory connectionFactory) {
		this();
		setConnectionFactory(connectionFactory);
		afterPropertiesSet();
	}

	/**
	 * Initialize the default implementations for the template's strategies:
	 * DynamicDestinationResolver and SimpleMessageConverter.
	 * @see #setDestinationResolver
	 * @see #setMessageConverter
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 */
	protected void initDefaultStrategies() {
		setMessageConverter(new SimpleMessageConverter());
	}


	/**
	 * Set the destination to be used on send/receive operations that do not
	 * have a destination parameter.
	 * <p>Alternatively, specify a "defaultDestinationName", to be
	 * dynamically resolved via the DestinationResolver.
	 * @see #send(MessageCreator)
	 * @see #convertAndSend(Object)
	 * @see #convertAndSend(Object, MessagePostProcessor)
	 * @see #setDefaultDestinationName(String)
	 */
	public void setDefaultDestination(Destination destination) {
		this.defaultDestination = destination;
	}

	/**
	 * Return the destination to be used on send/receive operations that do not
	 * have a destination parameter.
	 */
	public Destination getDefaultDestination() {
		return (this.defaultDestination instanceof Destination ? (Destination) this.defaultDestination : null);
	}

	private Queue getDefaultQueue() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null && !(defaultDestination instanceof Queue)) {
			throw new IllegalStateException(
					"'defaultDestination' does not correspond to a Queue. Check configuration of JmsTemplate.");
		}
		return (Queue) defaultDestination;
	}

	/**
	 * Set the destination name to be used on send/receive operations that
	 * do not have a destination parameter. The specified name will be
	 * dynamically resolved via the DestinationResolver.
	 * <p>Alternatively, specify a JMS Destination object as "defaultDestination".
	 * @see #send(MessageCreator)
	 * @see #convertAndSend(Object)
	 * @see #convertAndSend(Object, MessagePostProcessor)
	 * @see #setDestinationResolver
	 * @see #setDefaultDestination(javax.jms.Destination)
	 */
	public void setDefaultDestinationName(String destinationName) {
		this.defaultDestination = destinationName;
	}

	/**
	 * Return the destination name to be used on send/receive operations that
	 * do not have a destination parameter.
	 */
	public String getDefaultDestinationName() {
		return (this.defaultDestination instanceof String ? (String) this.defaultDestination : null);
	}

	private String getRequiredDefaultDestinationName() throws IllegalStateException {
		String name = getDefaultDestinationName();
		if (name == null) {
			throw new IllegalStateException(
					"No 'defaultDestination' or 'defaultDestinationName' specified. Check configuration of JmsTemplate.");
		}
		return name;
	}

	/**
	 * Set the message converter for this template. Used to resolve
	 * Object parameters to convertAndSend methods and Object results
	 * from receiveAndConvert methods.
	 * <p>The default converter is a SimpleMessageConverter, which is able
	 * to handle BytesMessages, TextMessages and ObjectMessages.
	 * @see #convertAndSend
	 * @see #receiveAndConvert
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the message converter for this template.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	private MessageConverter getRequiredMessageConverter() throws IllegalStateException {
		MessageConverter converter = getMessageConverter();
		if (converter == null) {
			throw new IllegalStateException("No 'messageConverter' specified. Check configuration of JmsTemplate.");
		}
		return converter;
	}


	/**
	 * Set whether message IDs are enabled. Default is "true".
	 * <p>This is only a hint to the JMS producer.
	 * See the JMS javadocs for details.
	 * @see javax.jms.MessageProducer#setDisableMessageID
	 */
	public void setMessageIdEnabled(boolean messageIdEnabled) {
		this.messageIdEnabled = messageIdEnabled;
	}

	/**
	 * Return whether message IDs are enabled.
	 */
	public boolean isMessageIdEnabled() {
		return this.messageIdEnabled;
	}

	/**
	 * Set whether message timestamps are enabled. Default is "true".
	 * <p>This is only a hint to the JMS producer.
	 * See the JMS javadocs for details.
	 * @see javax.jms.MessageProducer#setDisableMessageTimestamp
	 */
	public void setMessageTimestampEnabled(boolean messageTimestampEnabled) {
		this.messageTimestampEnabled = messageTimestampEnabled;
	}

	/**
	 * Return whether message timestamps are enabled.
	 */
	public boolean isMessageTimestampEnabled() {
		return this.messageTimestampEnabled;
	}

	/**
	 * Set whether to inhibit the delivery of messages published by its own connection.
	 * Default is "false".
	 * @see javax.jms.TopicSession#createSubscriber(javax.jms.Topic, String, boolean)
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * Return whether to inhibit the delivery of messages published by its own connection.
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * Set the timeout to use for receive calls (in milliseconds).
	 * <p>The default is {@link #RECEIVE_TIMEOUT_INDEFINITE_WAIT}, which indicates
	 * a blocking receive without timeout.
	 * <p>Specify {@link #RECEIVE_TIMEOUT_NO_WAIT} to inidicate that a receive operation
	 * should check if a message is immediately available without blocking.
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receive()
	 * @see javax.jms.MessageConsumer#receiveNoWait()
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Return the timeout to use for receive calls (in milliseconds).
	 */
	public long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	/**
	 * Set if the QOS values (deliveryMode, priority, timeToLive)
	 * should be used for sending a message.
	 * @see #setDeliveryMode
	 * @see #setPriority
	 * @see #setTimeToLive
	 */
	public void setExplicitQosEnabled(boolean explicitQosEnabled) {
		this.explicitQosEnabled = explicitQosEnabled;
	}

	/**
	 * If "true", then the values of deliveryMode, priority, and timeToLive
	 * will be used when sending a message. Otherwise, the default values,
	 * that may be set administratively, will be used.
	 * @return true if overriding default values of QOS parameters
	 * (deliveryMode, priority, and timeToLive)
	 * @see #setDeliveryMode
	 * @see #setPriority
	 * @see #setTimeToLive
	 */
	public boolean isExplicitQosEnabled() {
		return this.explicitQosEnabled;
	}

	/**
	 * Set whether message delivery should be persistent or non-persistent,
	 * specified as boolean value ("true" or "false"). This will set the delivery
	 * mode accordingly, to either "PERSISTENT" (1) or "NON_PERSISTENT" (2).
	 * <p>Default it "true" aka delivery mode "PERSISTENT".
	 * @see #setDeliveryMode(int)
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * Set the delivery mode to use when sending a message.
	 * Default is the Message default: "PERSISTENT".
	 * <p>Since a default value may be defined administratively,
	 * this is only used when "isExplicitQosEnabled" equals "true".
	 * @param deliveryMode the delivery mode to use
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.DeliveryMode#PERSISTENT
	 * @see javax.jms.DeliveryMode#NON_PERSISTENT
	 * @see javax.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setDeliveryMode(int deliveryMode) {
		this.deliveryMode = deliveryMode;
	}

	/**
	 * Return the delivery mode to use when sending a message.
	 */
	public int getDeliveryMode() {
		return this.deliveryMode;
	}

	/**
	 * Set the priority of a message when sending.
	 * <p>Since a default value may be defined administratively,
	 * this is only used when "isExplicitQosEnabled" equals "true".
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.Message#DEFAULT_PRIORITY
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setPriority(int priority) {
		this.priority = priority;
	}

	/**
	 * Return the priority of a message when sending.
	 */
	public int getPriority() {
		return this.priority;
	}

	/**
	 * Set the time-to-live of the message when sending.
	 * <p>Since a default value may be defined administratively,
	 * this is only used when "isExplicitQosEnabled" equals "true".
	 * @param timeToLive the message's lifetime (in milliseconds)
	 * @see #isExplicitQosEnabled
	 * @see javax.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see javax.jms.MessageProducer#send(javax.jms.Message, int, int, long)
	 */
	public void setTimeToLive(long timeToLive) {
		this.timeToLive = timeToLive;
	}

	/**
	 * Return the time-to-live of the message when sending.
	 */
	public long getTimeToLive() {
		return this.timeToLive;
	}


	//-------------------------------------------------------------------------
	// JmsOperations execute methods
	//-------------------------------------------------------------------------

	@Override
	public <T> T execute(SessionCallback<T> action) throws JmsException {
		return execute(action, false);
	}

	/**
	 * Execute the action specified by the given action object within a
	 * JMS Session. Generalized version of {@code execute(SessionCallback)},
	 * allowing the JMS Connection to be started on the fly.
	 * <p>Use {@code execute(SessionCallback)} for the general case.
	 * Starting the JMS Connection is just necessary for receiving messages,
	 * which is preferably achieved through the {@code receive} methods.
	 * @param action callback object that exposes the Session
	 * @param startConnection whether to start the Connection
	 * @return the result object from working with the Session
	 * @throws JmsException if there is any problem
	 * @see #execute(SessionCallback)
	 * @see #receive
	 */
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
					getConnectionFactory(), this.transactionalResourceFactory, startConnection);
			if (sessionToUse == null) {
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				if (startConnection) {
					conToClose.start();
				}
				sessionToUse = sessionToClose;
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + sessionToUse);
			}
			return action.doInJms(sessionToUse);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			ConnectionFactoryUtils.releaseConnection(conToClose, getConnectionFactory(), startConnection);
		}
	}

	@Override
	public <T> T execute(ProducerCallback<T> action) throws JmsException {
		String defaultDestinationName = getDefaultDestinationName();
		if (defaultDestinationName != null) {
			return execute(defaultDestinationName, action);
		}
		else {
			return execute(getDefaultDestination(), action);
		}
	}

	@Override
	public <T> T execute(final Destination destination, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				MessageProducer producer = createProducer(session, destination);
				try {
					return action.doInJms(session, producer);
				}
				finally {
					JmsUtils.closeMessageProducer(producer);
				}
			}
		}, false);
	}

	@Override
	public <T> T execute(final String destinationName, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				MessageProducer producer = createProducer(session, destination);
				try {
					return action.doInJms(session, producer);
				}
				finally {
					JmsUtils.closeMessageProducer(producer);
				}
			}
		}, false);
	}


	//-------------------------------------------------------------------------
	// Convenience methods for sending messages
	//-------------------------------------------------------------------------

	@Override
	public void send(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			send(defaultDestination, messageCreator);
		}
		else {
			send(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	public void send(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		execute(new SessionCallback<Object>() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				doSend(session, destination, messageCreator);
				return null;
			}
		}, false);
	}

	@Override
	public void send(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		execute(new SessionCallback<Object>() {
			@Override
			public Object doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				doSend(session, destination, messageCreator);
				return null;
			}
		}, false);
	}

	/**
	 * Send the given JMS message.
	 * @param session the JMS Session to operate on
	 * @param destination the JMS Destination to send to
	 * @param messageCreator callback to create a JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void doSend(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		MessageProducer producer = createProducer(session, destination);
		try {
			Message message = messageCreator.createMessage(session);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + message);
			}
			doSend(producer, message);
			// Check commit - avoid commit call within a JTA transaction.
			if (session.getTransacted() && isSessionLocallyTransacted(session)) {
				// Transacted session created by this template -> commit.
				JmsUtils.commitIfNecessary(session);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * Actually send the given JMS message.
	 * @param producer the JMS MessageProducer to send with
	 * @param message the JMS Message to send
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void doSend(MessageProducer producer, Message message) throws JMSException {
		if (isExplicitQosEnabled()) {
			producer.send(message, getDeliveryMode(), getPriority(), getTimeToLive());
		}
		else {
			producer.send(message);
		}
	}


	//-------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//-------------------------------------------------------------------------

	@Override
	public void convertAndSend(Object message) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message);
		}
	}

	@Override
	public void convertAndSend(Destination destination, final Object message) throws JmsException {
		send(destination, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return getRequiredMessageConverter().toMessage(message, session);
			}
		});
	}

	@Override
	public void convertAndSend(String destinationName, final Object message) throws JmsException {
		send(destinationName, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				return getRequiredMessageConverter().toMessage(message, session);
			}
		});
	}

	@Override
	public void convertAndSend(Object message, MessagePostProcessor postProcessor) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			convertAndSend(defaultDestination, message, postProcessor);
		}
		else {
			convertAndSend(getRequiredDefaultDestinationName(), message, postProcessor);
		}
	}

	@Override
	public void convertAndSend(
			Destination destination, final Object message, final MessagePostProcessor postProcessor)
			throws JmsException {

		send(destination, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				Message msg = getRequiredMessageConverter().toMessage(message, session);
				return postProcessor.postProcessMessage(msg);
			}
		});
	}

	@Override
	public void convertAndSend(
			String destinationName, final Object message, final MessagePostProcessor postProcessor)
		throws JmsException {

		send(destinationName, new MessageCreator() {
			@Override
			public Message createMessage(Session session) throws JMSException {
				Message msg = getRequiredMessageConverter().toMessage(message, session);
				return postProcessor.postProcessMessage(msg);
			}
		});
	}


	//-------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//-------------------------------------------------------------------------

	@Override
	public Message receive() throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receive(defaultDestination);
		}
		else {
			return receive(getRequiredDefaultDestinationName());
		}
	}

	@Override
	public Message receive(Destination destination) throws JmsException {
		return receiveSelected(destination, null);
	}

	@Override
	public Message receive(String destinationName) throws JmsException {
		return receiveSelected(destinationName, null);
	}

	@Override
	public Message receiveSelected(String messageSelector) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return receiveSelected(defaultDestination, messageSelector);
		}
		else {
			return receiveSelected(getRequiredDefaultDestinationName(), messageSelector);
		}
	}

	@Override
	public Message receiveSelected(final Destination destination, final String messageSelector) throws JmsException {
		return execute(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				return doReceive(session, destination, messageSelector);
			}
		}, true);
	}

	@Override
	public Message receiveSelected(final String destinationName, final String messageSelector) throws JmsException {
		return execute(new SessionCallback<Message>() {
			@Override
			public Message doInJms(Session session) throws JMSException {
				Destination destination = resolveDestinationName(session, destinationName);
				return doReceive(session, destination, messageSelector);
			}
		}, true);
	}

	/**
	 * Receive a JMS message.
	 * @param session the JMS Session to operate on
	 * @param destination the JMS Destination to receive from
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the JMS Message received, or {@code null} if none
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected Message doReceive(Session session, Destination destination, String messageSelector)
			throws JMSException {

		return doReceive(session, createConsumer(session, destination, messageSelector));
	}

	/**
	 * Actually receive a JMS message.
	 * @param session the JMS Session to operate on
	 * @param consumer the JMS MessageConsumer to receive with
	 * @return the JMS Message received, or {@code null} if none
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected Message doReceive(Session session, MessageConsumer consumer) throws JMSException {
		try {
			// Use transaction timeout (if available).
			long timeout = getReceiveTimeout();
			JmsResourceHolder resourceHolder =
					(JmsResourceHolder) TransactionSynchronizationManager.getResource(getConnectionFactory());
			if (resourceHolder != null && resourceHolder.hasTimeout()) {
				timeout = Math.min(timeout, resourceHolder.getTimeToLiveInMillis());
			}
			Message message = doReceive(consumer, timeout);
			if (session.getTransacted()) {
				// Commit necessary - but avoid commit call within a JTA transaction.
				if (isSessionLocallyTransacted(session)) {
					// Transacted session created by this template -> commit.
					JmsUtils.commitIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				// Manually acknowledge message, if any.
				if (message != null) {
					message.acknowledge();
				}
			}
			return message;
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
		}
	}

	/**
	 * Actually receive a message from the given consumer.
	 * @param consumer the JMS MessageConsumer to receive with
	 * @param timeout the receive timeout
	 * @return the JMS Message received, or {@code null} if none
	 * @throws JMSException if thrown by JMS API methods
	 */
	private Message doReceive(MessageConsumer consumer, long timeout) throws JMSException {
		if (timeout == RECEIVE_TIMEOUT_NO_WAIT) {
			return consumer.receiveNoWait();
		}
		else if (timeout > 0) {
			return consumer.receive(timeout);
		}
		else {
			return consumer.receive();
		}
	}


	//-------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//-------------------------------------------------------------------------

	@Override
	public Object receiveAndConvert() throws JmsException {
		return doConvertFromMessage(receive());
	}

	@Override
	public Object receiveAndConvert(Destination destination) throws JmsException {
		return doConvertFromMessage(receive(destination));
	}

	@Override
	public Object receiveAndConvert(String destinationName) throws JmsException {
		return doConvertFromMessage(receive(destinationName));
	}

	@Override
	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(messageSelector));
	}

	@Override
	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destination, messageSelector));
	}

	@Override
	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destinationName, messageSelector));
	}

	/**
	 * Extract the content from the given JMS message.
	 * @param message the JMS Message to convert (can be {@code null})
	 * @return the content of the message, or {@code null} if none
	 */
	protected Object doConvertFromMessage(Message message) {
		if (message != null) {
			try {
				return getRequiredMessageConverter().fromMessage(message);
			}
			catch (JMSException ex) {
				throw convertJmsAccessException(ex);
			}
		}
		return null;
	}


	//-------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//-------------------------------------------------------------------------

	@Override
	public <T> T browse(BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browse(defaultQueue, action);
		}
		else {
			return browse(getRequiredDefaultDestinationName(), action);
		}
	}

	@Override
	public <T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queue, null, action);
	}

	@Override
	public <T> T browse(String queueName, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queueName, null, action);
	}

	@Override
	public <T> T browseSelected(String messageSelector, BrowserCallback<T> action) throws JmsException {
		Queue defaultQueue = getDefaultQueue();
		if (defaultQueue != null) {
			return browseSelected(defaultQueue, messageSelector, action);
		}
		else {
			return browseSelected(getRequiredDefaultDestinationName(), messageSelector, action);
		}
	}

	@Override
	public <T> T browseSelected(final Queue queue, final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				QueueBrowser browser = createBrowser(session, queue, messageSelector);
				try {
					return action.doInJms(session, browser);
				}
				finally {
					JmsUtils.closeQueueBrowser(browser);
				}
			}
		}, true);
	}

	@Override
	public <T> T browseSelected(final String queueName, final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(new SessionCallback<T>() {
			@Override
			public T doInJms(Session session) throws JMSException {
				Queue queue = (Queue) getDestinationResolver().resolveDestinationName(session, queueName, false);
				QueueBrowser browser = createBrowser(session, queue, messageSelector);
				try {
					return action.doInJms(session, browser);
				}
				finally {
					JmsUtils.closeQueueBrowser(browser);
				}
			}
		}, true);
	}


	//-------------------------------------------------------------------------
	// JMS 1.1 factory methods, potentially overridden for JMS 1.0.2
	//-------------------------------------------------------------------------

	/**
	 * Fetch an appropriate Connection from the given JmsResourceHolder.
	 * <p>This implementation accepts any JMS 1.1 Connection.
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Connection fetched from the holder,
	 * or {@code null} if none found
	 */
	protected Connection getConnection(JmsResourceHolder holder) {
		return holder.getConnection();
	}

	/**
	 * Fetch an appropriate Session from the given JmsResourceHolder.
	 * <p>This implementation accepts any JMS 1.1 Session.
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Session fetched from the holder,
	 * or {@code null} if none found
	 */
	protected Session getSession(JmsResourceHolder holder) {
		return holder.getSession();
	}

	/**
	 * Check whether the given Session is locally transacted, that is, whether
	 * its transaction is managed by this listener container's Session handling
	 * and not by an external transaction coordinator.
	 * <p>Note: The Session's own transacted flag will already have been checked
	 * before. This method is about finding out whether the Session's transaction
	 * is local or externally coordinated.
	 * @param session the Session to check
	 * @return whether the given Session is locally transacted
	 * @see #isSessionTransacted()
	 * @see org.springframework.jms.connection.ConnectionFactoryUtils#isSessionTransactional
	 */
	protected boolean isSessionLocallyTransacted(Session session) {
		return isSessionTransacted() &&
				!ConnectionFactoryUtils.isSessionTransactional(session, getConnectionFactory());
	}

	/**
	 * Create a JMS MessageProducer for the given Session and Destination,
	 * configuring it to disable message ids and/or timestamps (if necessary).
	 * <p>Delegates to {@link #doCreateProducer} for creation of the raw
	 * JMS MessageProducer, which needs to be specific to JMS 1.1 or 1.0.2.
	 * @param session the JMS Session to create a MessageProducer for
	 * @param destination the JMS Destination to create a MessageProducer for
	 * @return the new JMS MessageProducer
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected MessageProducer createProducer(Session session, Destination destination) throws JMSException {
		MessageProducer producer = doCreateProducer(session, destination);
		if (!isMessageIdEnabled()) {
			producer.setDisableMessageID(true);
		}
		if (!isMessageTimestampEnabled()) {
			producer.setDisableMessageTimestamp(true);
		}
		return producer;
	}

	/**
	 * Create a raw JMS MessageProducer for the given Session and Destination.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param session the JMS Session to create a MessageProducer for
	 * @param destination the JMS Destination to create a MessageProducer for
	 * @return the new JMS MessageProducer
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected MessageProducer doCreateProducer(Session session, Destination destination) throws JMSException {
		return session.createProducer(destination);
	}

	/**
	 * Create a JMS MessageConsumer for the given Session and Destination.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param session the JMS Session to create a MessageConsumer for
	 * @param destination the JMS Destination to create a MessageConsumer for
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the new JMS MessageConsumer
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination, String messageSelector)
			throws JMSException {

		// Only pass in the NoLocal flag in case of a Topic:
		// Some JMS providers, such as WebSphere MQ 6.0, throw IllegalStateException
		// in case of the NoLocal flag being specified for a Queue.
		if (isPubSubDomain()) {
			return session.createConsumer(destination, messageSelector, isPubSubNoLocal());
		}
		else {
			return session.createConsumer(destination, messageSelector);
		}
	}

	/**
	 * Create a JMS MessageProducer for the given Session and Destination,
	 * configuring it to disable message ids and/or timestamps (if necessary).
	 * <p>Delegates to {@link #doCreateProducer} for creation of the raw
	 * JMS MessageProducer, which needs to be specific to JMS 1.1 or 1.0.2.
	 * @param session the JMS Session to create a QueueBrowser for
	 * @param queue the JMS Queue to create a QueueBrowser for
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the new JMS QueueBrowser
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected QueueBrowser createBrowser(Session session, Queue queue, String messageSelector)
			throws JMSException {

		return session.createBrowser(queue, messageSelector);
	}


	/**
	 * ResourceFactory implementation that delegates to this template's protected callback methods.
	 */
	private class JmsTemplateResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		public Connection getConnection(JmsResourceHolder holder) {
			return JmsTemplate.this.getConnection(holder);
		}

		@Override
		public Session getSession(JmsResourceHolder holder) {
			return JmsTemplate.this.getSession(holder);
		}

		@Override
		public Connection createConnection() throws JMSException {
			return JmsTemplate.this.createConnection();
		}

		@Override
		public Session createSession(Connection con) throws JMSException {
			return JmsTemplate.this.createSession(con);
		}

		@Override
		public boolean isSynchedLocalTransactionAllowed() {
			return JmsTemplate.this.isSessionTransacted();
		}
	}

}
