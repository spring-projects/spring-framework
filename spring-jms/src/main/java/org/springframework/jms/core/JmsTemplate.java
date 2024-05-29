/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.jms.core;

import io.micrometer.jakarta9.instrument.jms.JmsInstrumentation;
import io.micrometer.observation.ObservationRegistry;
import jakarta.jms.Connection;
import jakarta.jms.ConnectionFactory;
import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.JMSException;
import jakarta.jms.Message;
import jakarta.jms.MessageConsumer;
import jakarta.jms.MessageProducer;
import jakarta.jms.Queue;
import jakarta.jms.QueueBrowser;
import jakarta.jms.Session;
import jakarta.jms.TemporaryQueue;

import org.springframework.jms.JmsException;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.connection.JmsResourceHolder;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Helper class that simplifies synchronous JMS access code.
 *
 * <p>If you want to use dynamic destination creation, you must specify
 * the type of JMS destination to create, using the "pubSubDomain" property.
 * For other operations, this is not necessary. Point-to-Point (Queues) is the default
 * domain.
 *
 * <p>Default settings for JMS Sessions are "not transacted" and "auto-acknowledge".
 * As defined by the Jakarta EE specification, the transaction and acknowledgement
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
 * purpose of sending messages via this template. In a Jakarta EE environment,
 * make sure that the {@code ConnectionFactory} is obtained from the
 * application's environment naming context via JNDI; application servers
 * typically expose pooled, transaction-aware factories there.
 *
 * @author Mark Pollack
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 1.1
 * @see #setConnectionFactory
 * @see #setPubSubDomain
 * @see #setDestinationResolver
 * @see #setMessageConverter
 * @see jakarta.jms.MessageProducer
 * @see jakarta.jms.MessageConsumer
 */
public class JmsTemplate extends JmsDestinationAccessor implements JmsOperations {

	private static final boolean micrometerJakartaPresent = ClassUtils.isPresent(
			"io.micrometer.jakarta9.instrument.jms.JmsInstrumentation", JmsTemplate.class.getClassLoader());

	/** Internal ResourceFactory adapter for interacting with ConnectionFactoryUtils. */
	private final JmsTemplateResourceFactory transactionalResourceFactory = new JmsTemplateResourceFactory();


	@Nullable
	private Object defaultDestination;

	@Nullable
	private MessageConverter messageConverter;


	private boolean messageIdEnabled = true;

	private boolean messageTimestampEnabled = true;

	private boolean pubSubNoLocal = false;

	private long receiveTimeout = RECEIVE_TIMEOUT_INDEFINITE_WAIT;

	private long deliveryDelay = -1;


	private boolean explicitQosEnabled = false;

	private int deliveryMode = Message.DEFAULT_DELIVERY_MODE;

	private int priority = Message.DEFAULT_PRIORITY;

	private long timeToLive = Message.DEFAULT_TIME_TO_LIVE;

	@Nullable
	private ObservationRegistry observationRegistry;


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
	public void setDefaultDestination(@Nullable Destination destination) {
		this.defaultDestination = destination;
	}

	/**
	 * Return the destination to be used on send/receive operations that do not
	 * have a destination parameter.
	 */
	@Nullable
	public Destination getDefaultDestination() {
		return (this.defaultDestination instanceof Destination dest ? dest : null);
	}

	@Nullable
	private Queue getDefaultQueue() {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination == null) {
			return null;
		}
		if (!(defaultDestination instanceof Queue queue)) {
			throw new IllegalStateException(
					"'defaultDestination' does not correspond to a Queue. Check configuration of JmsTemplate.");
		}
		return queue;
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
	 * @see #setDefaultDestination(jakarta.jms.Destination)
	 */
	public void setDefaultDestinationName(@Nullable String destinationName) {
		this.defaultDestination = destinationName;
	}

	/**
	 * Return the destination name to be used on send/receive operations that
	 * do not have a destination parameter.
	 */
	@Nullable
	public String getDefaultDestinationName() {
		return (this.defaultDestination instanceof String name ? name : null);
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
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the message converter for this template.
	 */
	@Nullable
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
	 * @see jakarta.jms.MessageProducer#setDisableMessageID
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
	 * @see jakarta.jms.MessageProducer#setDisableMessageTimestamp
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
	 * @see jakarta.jms.Session#createConsumer(jakarta.jms.Destination, String, boolean)
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
	 * <p>Specify {@link #RECEIVE_TIMEOUT_NO_WAIT} (or any other negative value)
	 * to indicate that a receive operation should check if a message is
	 * immediately available without blocking.
	 * @see #receiveFromConsumer(MessageConsumer, long)
	 * @see jakarta.jms.MessageConsumer#receive(long)
	 * @see jakarta.jms.MessageConsumer#receiveNoWait()
	 * @see jakarta.jms.MessageConsumer#receive()
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
	 * Set the delivery delay to use for send calls (in milliseconds).
	 * <p>The default is -1 (no delivery delay passed on to the broker).
	 * Note that this feature requires JMS 2.0.
	 */
	public void setDeliveryDelay(long deliveryDelay) {
		this.deliveryDelay = deliveryDelay;
	}

	/**
	 * Return the delivery delay to use for send calls (in milliseconds).
	 */
	public long getDeliveryDelay() {
		return this.deliveryDelay;
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
	 * Set the {@link QosSettings} to use when sending a message.
	 * @param settings the deliveryMode, priority, and timeToLive settings to use
	 * @since 5.0
	 * @see #setExplicitQosEnabled(boolean)
	 * @see #setDeliveryMode(int)
	 * @see #setPriority(int)
	 * @see #setTimeToLive(long)
	 */
	public void setQosSettings(QosSettings settings) {
		Assert.notNull(settings, "Settings must not be null");
		setExplicitQosEnabled(true);
		setDeliveryMode(settings.getDeliveryMode());
		setPriority(settings.getPriority());
		setTimeToLive(settings.getTimeToLive());
	}

	/**
	 * Set whether message delivery should be persistent or non-persistent,
	 * specified as boolean value ("true" or "false"). This will set the delivery
	 * mode accordingly, to either "PERSISTENT" (2) or "NON_PERSISTENT" (1).
	 * <p>Default is "true" a.k.a. delivery mode "PERSISTENT".
	 * @see #setDeliveryMode(int)
	 * @see jakarta.jms.DeliveryMode#PERSISTENT
	 * @see jakarta.jms.DeliveryMode#NON_PERSISTENT
	 */
	public void setDeliveryPersistent(boolean deliveryPersistent) {
		this.deliveryMode = (deliveryPersistent ? DeliveryMode.PERSISTENT : DeliveryMode.NON_PERSISTENT);
	}

	/**
	 * Set the delivery mode to use when sending a message.
	 * Default is the JMS Message default: "PERSISTENT".
	 * <p>Since a default value may be defined administratively,
	 * this is only used when "isExplicitQosEnabled" equals "true".
	 * @param deliveryMode the delivery mode to use
	 * @see #isExplicitQosEnabled
	 * @see jakarta.jms.DeliveryMode#PERSISTENT
	 * @see jakarta.jms.DeliveryMode#NON_PERSISTENT
	 * @see jakarta.jms.Message#DEFAULT_DELIVERY_MODE
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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
	 * @see jakarta.jms.Message#DEFAULT_PRIORITY
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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
	 * @see jakarta.jms.Message#DEFAULT_TIME_TO_LIVE
	 * @see jakarta.jms.MessageProducer#send(jakarta.jms.Message, int, int, long)
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

	/**
	 * Configure the {@link ObservationRegistry} to use for recording JMS observations.
	 * @param observationRegistry the observation registry to use.
	 * @since 6.1
	 * @see io.micrometer.jakarta9.instrument.jms.JmsInstrumentation
	 */
	public void setObservationRegistry(ObservationRegistry observationRegistry) {
		this.observationRegistry = observationRegistry;
	}

	//---------------------------------------------------------------------------------------
	// JmsOperations execute methods
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
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
	@SuppressWarnings("resource")
	@Nullable
	public <T> T execute(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = ConnectionFactoryUtils.doGetTransactionalSession(
					obtainConnectionFactory(), this.transactionalResourceFactory, startConnection);
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
			if (micrometerJakartaPresent && this.observationRegistry != null) {
				sessionToUse = MicrometerInstrumentation.instrumentSession(sessionToUse, this.observationRegistry);
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
	@Nullable
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
	@Nullable
	public <T> T execute(final @Nullable Destination destination, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			MessageProducer producer = createProducer(session, destination);
			try {
				return action.doInJms(session, producer);
			}
			finally {
				JmsUtils.closeMessageProducer(producer);
			}
		}, false);
	}

	@Override
	@Nullable
	public <T> T execute(final String destinationName, final ProducerCallback<T> action) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			MessageProducer producer = createProducer(session, destination);
			try {
				return action.doInJms(session, producer);
			}
			finally {
				JmsUtils.closeMessageProducer(producer);
			}
		}, false);
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages
	//---------------------------------------------------------------------------------------

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
		execute(session -> {
			doSend(session, destination, messageCreator);
			return null;
		}, false);
	}

	@Override
	public void send(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			doSend(session, destination, messageCreator);
			return null;
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
		if (this.deliveryDelay >= 0) {
			producer.setDeliveryDelay(this.deliveryDelay);
		}
		if (isExplicitQosEnabled()) {
			producer.send(message, getDeliveryMode(), getPriority(), getTimeToLive());
		}
		else {
			producer.send(message);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending auto-converted messages
	//---------------------------------------------------------------------------------------

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
		send(destination, session -> getRequiredMessageConverter().toMessage(message, session));
	}

	@Override
	public void convertAndSend(String destinationName, final Object message) throws JmsException {
		send(destinationName, session -> getRequiredMessageConverter().toMessage(message, session));
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

		send(destination, session -> {
			Message msg = getRequiredMessageConverter().toMessage(message, session);
			return postProcessor.postProcessMessage(msg);
		});
	}

	@Override
	public void convertAndSend(
			String destinationName, final Object message, final MessagePostProcessor postProcessor)
		throws JmsException {

		send(destinationName, session -> {
			Message msg = getRequiredMessageConverter().toMessage(message, session);
			return postProcessor.postProcessMessage(msg);
		});
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
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
	@Nullable
	public Message receive(Destination destination) throws JmsException {
		return receiveSelected(destination, null);
	}

	@Override
	@Nullable
	public Message receive(String destinationName) throws JmsException {
		return receiveSelected(destinationName, null);
	}

	@Override
	@Nullable
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
	@Nullable
	public Message receiveSelected(final Destination destination, @Nullable final String messageSelector) throws JmsException {
		return execute(session -> doReceive(session, destination, messageSelector), true);
	}

	@Override
	@Nullable
	public Message receiveSelected(final String destinationName, @Nullable final String messageSelector) throws JmsException {
		return execute(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			return doReceive(session, destination, messageSelector);
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
	@Nullable
	protected Message doReceive(Session session, Destination destination, @Nullable String messageSelector)
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
	@Nullable
	protected Message doReceive(Session session, MessageConsumer consumer) throws JMSException {
		try {
			// Use transaction timeout (if available).
			long timeout = getReceiveTimeout();
			ConnectionFactory connectionFactory = getConnectionFactory();
			JmsResourceHolder resourceHolder = null;
			if (connectionFactory != null) {
				resourceHolder = (JmsResourceHolder) TransactionSynchronizationManager.getResource(connectionFactory);
			}
			if (resourceHolder != null && resourceHolder.hasTimeout()) {
				timeout = Math.min(timeout, resourceHolder.getTimeToLiveInMillis());
			}
			Message message = receiveFromConsumer(consumer, timeout);
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


	//---------------------------------------------------------------------------------------
	// Convenience methods for receiving auto-converted messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Object receiveAndConvert() throws JmsException {
		return doConvertFromMessage(receive());
	}

	@Override
	@Nullable
	public Object receiveAndConvert(Destination destination) throws JmsException {
		return doConvertFromMessage(receive(destination));
	}

	@Override
	@Nullable
	public Object receiveAndConvert(String destinationName) throws JmsException {
		return doConvertFromMessage(receive(destinationName));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(messageSelector));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(Destination destination, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destination, messageSelector));
	}

	@Override
	@Nullable
	public Object receiveSelectedAndConvert(String destinationName, String messageSelector) throws JmsException {
		return doConvertFromMessage(receiveSelected(destinationName, messageSelector));
	}

	/**
	 * Extract the content from the given JMS message.
	 * @param message the JMS Message to convert (can be {@code null})
	 * @return the content of the message, or {@code null} if none
	 */
	@Nullable
	protected Object doConvertFromMessage(@Nullable Message message) {
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


	//---------------------------------------------------------------------------------------
	// Convenience methods for sending messages to and receiving the reply from a destination
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
	public Message sendAndReceive(MessageCreator messageCreator) throws JmsException {
		Destination defaultDestination = getDefaultDestination();
		if (defaultDestination != null) {
			return sendAndReceive(defaultDestination, messageCreator);
		}
		else {
			return sendAndReceive(getRequiredDefaultDestinationName(), messageCreator);
		}
	}

	@Override
	@Nullable
	public Message sendAndReceive(final Destination destination, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(session -> doSendAndReceive(session, destination, messageCreator), true);
	}

	@Override
	@Nullable
	public Message sendAndReceive(final String destinationName, final MessageCreator messageCreator) throws JmsException {
		return executeLocal(session -> {
			Destination destination = resolveDestinationName(session, destinationName);
			return doSendAndReceive(session, destination, messageCreator);
		}, true);
	}

	/**
	 * Send a request message to the given {@link Destination} and block until
	 * a reply has been received on a temporary queue created on-the-fly.
	 * <p>Return the response message or {@code null} if no message has
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Nullable
	protected Message doSendAndReceive(Session session, Destination destination, MessageCreator messageCreator)
			throws JMSException {

		Assert.notNull(messageCreator, "MessageCreator must not be null");
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			Message requestMessage = messageCreator.createMessage(session);
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(destination);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			if (logger.isDebugEnabled()) {
				logger.debug("Sending created message: " + requestMessage);
			}
			doSend(producer, requestMessage);
			return receiveFromConsumer(consumer, getReceiveTimeout());
		}
		finally {
			JmsUtils.closeMessageConsumer(consumer);
			JmsUtils.closeMessageProducer(producer);
			if (responseQueue != null) {
				responseQueue.delete();
			}
		}
	}

	/**
	 * A variant of {@link #execute(SessionCallback, boolean)} that explicitly
	 * creates a non-transactional {@link Session}. The given {@link SessionCallback}
	 * does not participate in an existing transaction.
	 */
	@Nullable
	private <T> T executeLocal(SessionCallback<T> action, boolean startConnection) throws JmsException {
		Assert.notNull(action, "Callback object must not be null");
		Connection con = null;
		Session session = null;
		try {
			con = createConnection();
			session = con.createSession(false, Session.AUTO_ACKNOWLEDGE);
			if (micrometerJakartaPresent && this.observationRegistry != null) {
				session = MicrometerInstrumentation.instrumentSession(session, this.observationRegistry);
			}
			if (startConnection) {
				con.start();
			}
			if (logger.isDebugEnabled()) {
				logger.debug("Executing callback on JMS Session: " + session);
			}
			return action.doInJms(session);
		}
		catch (JMSException ex) {
			throw convertJmsAccessException(ex);
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), startConnection);
		}
	}


	//---------------------------------------------------------------------------------------
	// Convenience methods for browsing messages
	//---------------------------------------------------------------------------------------

	@Override
	@Nullable
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
	@Nullable
	public <T> T browse(Queue queue, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queue, null, action);
	}

	@Override
	@Nullable
	public <T> T browse(String queueName, BrowserCallback<T> action) throws JmsException {
		return browseSelected(queueName, null, action);
	}

	@Override
	@Nullable
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
	@Nullable
	public <T> T browseSelected(final Queue queue, @Nullable final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			QueueBrowser browser = createBrowser(session, queue, messageSelector);
			try {
				return action.doInJms(session, browser);
			}
			finally {
				JmsUtils.closeQueueBrowser(browser);
			}
		}, true);
	}

	@Override
	@Nullable
	public <T> T browseSelected(final String queueName, @Nullable final String messageSelector, final BrowserCallback<T> action)
			throws JmsException {

		Assert.notNull(action, "Callback object must not be null");
		return execute(session -> {
			Queue queue = (Queue) getDestinationResolver().resolveDestinationName(session, queueName, false);
			QueueBrowser browser = createBrowser(session, queue, messageSelector);
			try {
				return action.doInJms(session, browser);
			}
			finally {
				JmsUtils.closeQueueBrowser(browser);
			}
		}, true);
	}


	/**
	 * Fetch an appropriate Connection from the given JmsResourceHolder.
	 * <p>This implementation accepts any JMS 1.1 Connection.
	 * @param holder the JmsResourceHolder
	 * @return an appropriate Connection fetched from the holder,
	 * or {@code null} if none found
	 */
	@Nullable
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
	@Nullable
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
	 * JMS MessageProducer.
	 * @param session the JMS Session to create a MessageProducer for
	 * @param destination the JMS Destination to create a MessageProducer for
	 * @return the new JMS MessageProducer
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected MessageProducer createProducer(Session session, @Nullable Destination destination) throws JMSException {
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
	protected MessageProducer doCreateProducer(Session session, @Nullable Destination destination) throws JMSException {
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
	protected MessageConsumer createConsumer(Session session, Destination destination, @Nullable String messageSelector)
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
	 * JMS MessageProducer.
	 * @param session the JMS Session to create a QueueBrowser for
	 * @param queue the JMS Queue to create a QueueBrowser for
	 * @param messageSelector the message selector for this consumer (can be {@code null})
	 * @return the new JMS QueueBrowser
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageIdEnabled
	 * @see #setMessageTimestampEnabled
	 */
	protected QueueBrowser createBrowser(Session session, Queue queue, @Nullable String messageSelector)
			throws JMSException {

		return session.createBrowser(queue, messageSelector);
	}


	/**
	 * ResourceFactory implementation that delegates to this template's protected callback methods.
	 */
	private class JmsTemplateResourceFactory implements ConnectionFactoryUtils.ResourceFactory {

		@Override
		@Nullable
		public Connection getConnection(JmsResourceHolder holder) {
			return JmsTemplate.this.getConnection(holder);
		}

		@Override
		@Nullable
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

	private abstract static class MicrometerInstrumentation {

		static Session instrumentSession(Session session, ObservationRegistry registry) {
			return JmsInstrumentation.instrumentSession(session, registry);
		}

	}

}
