/*
 * Copyright 2002-2019 the original author or authors.
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

import javax.jms.Connection;
import javax.jms.Destination;
import javax.jms.ExceptionListener;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageListener;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.Topic;

import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.util.ErrorHandler;

/**
 * Abstract base class for Spring message listener container implementations.
 * Can either host a standard JMS {@link javax.jms.MessageListener} or Spring's
 * {@link SessionAwareMessageListener} for actual message processing.
 *
 * <p>Usually holds a single JMS {@link Connection} that all listeners are supposed
 * to be registered on, which is the standard JMS way of managing listener sessions.
 * Can alternatively also be used with a fresh Connection per listener, for Java EE
 * style XA-aware JMS messaging. The actual registration process is up to concrete
 * subclasses.
 *
 * <p><b>NOTE:</b> The default behavior of this message listener container is to
 * <b>never</b> propagate an exception thrown by a message listener up to the JMS
 * provider. Instead, it will log any such exception at the error level.
 * This means that from the perspective of the attendant JMS provider no such
 * listener will ever fail. However, if error handling is necessary, then
 * any implementation of the {@link ErrorHandler} strategy may be provided to
 * the {@link #setErrorHandler(ErrorHandler)} method. Note that JMSExceptions
 * <b>will</b> be passed to the ErrorHandler in addition to (but after) being
 * passed to an {@link ExceptionListener}, if one has been provided.
 *
 * <p>The listener container offers the following message acknowledgment options:
 * <ul>
 * <li>"sessionAcknowledgeMode" set to "AUTO_ACKNOWLEDGE" (default):
 * This mode is container-dependent: For {@link DefaultMessageListenerContainer},
 * it means automatic message acknowledgment <i>before</i> listener execution, with
 * no redelivery in case of an exception and no redelivery in case of other listener
 * execution interruptions either. For {@link SimpleMessageListenerContainer},
 * it means automatic message acknowledgment <i>after</i> listener execution, with
 * no redelivery in case of a user exception thrown but potential redelivery in case
 * of the JVM dying during listener execution. In order to consistently arrange for
 * redelivery with any container variant, consider "CLIENT_ACKNOWLEDGE" mode or -
 * preferably - setting "sessionTransacted" to "true" instead.
 * <li>"sessionAcknowledgeMode" set to "DUPS_OK_ACKNOWLEDGE":
 * <i>Lazy</i> message acknowledgment during ({@link DefaultMessageListenerContainer})
 * or shortly after ({@link SimpleMessageListenerContainer}) listener execution;
 * no redelivery in case of a user exception thrown but potential redelivery in case
 * of the JVM dying during listener execution. In order to consistently arrange for
 * redelivery with any container variant, consider "CLIENT_ACKNOWLEDGE" mode or -
 * preferably - setting "sessionTransacted" to "true" instead.
 * <li>"sessionAcknowledgeMode" set to "CLIENT_ACKNOWLEDGE":
 * Automatic message acknowledgment <i>after</i> successful listener execution;
 * best-effort redelivery in case of a user exception thrown as well as in case
 * of other listener execution interruptions (such as the JVM dying).
 * <li>"sessionTransacted" set to "true":
 * Transactional acknowledgment after successful listener execution;
 * <i>guaranteed redelivery</i> in case of a user exception thrown as well as
 * in case of other listener execution interruptions (such as the JVM dying).
 * </ul>
 *
 * <p>There are two solutions to the duplicate message processing problem:
 * <ul>
 * <li>Either add <i>duplicate message detection</i> to your listener, in the
 * form of a business entity existence check or a protocol table check. This
 * usually just needs to be done in case of the JMSRedelivered flag being
 * set on the incoming message (otherwise just process straightforwardly).
 * Note that with "sessionTransacted" set to "true", duplicate messages will
 * only appear in case of the JVM dying at the most unfortunate point possible
 * (i.e. after your business logic executed but before the JMS part got committed),
 * so duplicate message detection is just there to cover a corner case.
 * <li>Or wrap your <i>entire processing with an XA transaction</i>, covering the
 * reception of the JMS message as well as the execution of the business logic in
 * your message listener (including database operations etc). This is only
 * supported by {@link DefaultMessageListenerContainer}, through specifying
 * an external "transactionManager" (typically a
 * {@link org.springframework.transaction.jta.JtaTransactionManager}, with
 * a corresponding XA-aware JMS {@link javax.jms.ConnectionFactory} passed in
 * as "connectionFactory").
 * </ul>
 * Note that XA transaction coordination adds significant runtime overhead,
 * so it might be feasible to avoid it unless absolutely necessary.
 *
 * <p><b>Recommendations:</b>
 * <ul>
 * <li>The general recommendation is to set "sessionTransacted" to "true",
 * typically in combination with local database transactions triggered by the
 * listener implementation, through Spring's standard transaction facilities.
 * This will work nicely in Tomcat or in a standalone environment, often
 * combined with custom duplicate message detection (if it is unacceptable
 * to ever process the same message twice).
 * <li>Alternatively, specify a
 * {@link org.springframework.transaction.jta.JtaTransactionManager} as
 * "transactionManager" for a fully XA-aware JMS provider - typically when
 * running on a Java EE server, but also for other environments with a JTA
 * transaction manager present. This will give full "exactly-once" guarantees
 * without custom duplicate message checks, at the price of additional
 * runtime processing overhead.
 * </ul>
 *
 * <p>Note that the "sessionTransacted" flag is strongly recommended over
 * {@link org.springframework.jms.connection.JmsTransactionManager}, provided
 * that transactions do not need to be managed externally. As a consequence,
 * set the transaction manager only if you are using JTA or if you need to
 * synchronize with custom external transaction arrangements.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setMessageListener
 * @see javax.jms.MessageListener
 * @see SessionAwareMessageListener
 * @see #handleListenerException
 * @see DefaultMessageListenerContainer
 * @see SimpleMessageListenerContainer
 * @see org.springframework.jms.listener.endpoint.JmsMessageEndpointManager
 */
public abstract class AbstractMessageListenerContainer extends AbstractJmsListeningContainer
		implements MessageListenerContainer {

	@Nullable
	private volatile Object destination;

	@Nullable
	private volatile String messageSelector;

	@Nullable
	private volatile Object messageListener;

	private boolean subscriptionDurable = false;

	private boolean subscriptionShared = false;

	@Nullable
	private String subscriptionName;

	@Nullable
	private Boolean replyPubSubDomain;

	@Nullable
	private QosSettings replyQosSettings;

	private boolean pubSubNoLocal = false;

	@Nullable
	private MessageConverter messageConverter;

	@Nullable
	private ExceptionListener exceptionListener;

	@Nullable
	private ErrorHandler errorHandler;

	private boolean exposeListenerSession = true;

	private boolean acceptMessagesWhileStopping = false;


	/**
	 * Specify concurrency limits.
	 */
	public abstract void setConcurrency(String concurrency);

	/**
	 * Set the destination to receive messages from.
	 * <p>Alternatively, specify a "destinationName", to be dynamically
	 * resolved via the {@link org.springframework.jms.support.destination.DestinationResolver}.
	 * <p>Note: The destination may be replaced at runtime, with the listener
	 * container picking up the new destination immediately (works e.g. with
	 * DefaultMessageListenerContainer, as long as the cache level is less than
	 * CACHE_CONSUMER). However, this is considered advanced usage; use it with care!
	 * @see #setDestinationName(String)
	 */
	public void setDestination(@Nullable Destination destination) {
		this.destination = destination;
		if (destination instanceof Topic && !(destination instanceof Queue)) {
			// Clearly a Topic: let's set the "pubSubDomain" flag accordingly.
			setPubSubDomain(true);
		}
	}

	/**
	 * Return the destination to receive messages from. Will be {@code null}
	 * if the configured destination is not an actual {@link Destination} type;
	 * c.f. {@link #setDestinationName(String) when the destination is a String}.
	 */
	@Nullable
	public Destination getDestination() {
		return (this.destination instanceof Destination ? (Destination) this.destination : null);
	}

	/**
	 * Set the name of the destination to receive messages from.
	 * <p>The specified name will be dynamically resolved via the configured
	 * {@link #setDestinationResolver destination resolver}.
	 * <p>Alternatively, specify a JMS {@link Destination} object as "destination".
	 * <p>Note: The destination may be replaced at runtime, with the listener
	 * container picking up the new destination immediately (works e.g. with
	 * DefaultMessageListenerContainer, as long as the cache level is less than
	 * CACHE_CONSUMER). However, this is considered advanced usage; use it with care!
	 * @see #setDestination(javax.jms.Destination)
	 */
	public void setDestinationName(@Nullable String destinationName) {
		this.destination = destinationName;
	}

	/**
	 * Return the name of the destination to receive messages from.
	 * Will be {@code null} if the configured destination is not a
	 * {@link String} type; c.f. {@link #setDestination(Destination) when
	 * it is an actual Destination}.
	 */
	@Nullable
	public String getDestinationName() {
		return (this.destination instanceof String ? (String) this.destination : null);
	}

	/**
	 * Return a descriptive String for this container's JMS destination
	 * (never {@code null}).
	 */
	protected String getDestinationDescription() {
		Object destination = this.destination;
		return (destination != null ? destination.toString() : "");
	}

	/**
	 * Set the JMS message selector expression (or {@code null} if none).
	 * Default is none.
	 * <p>See the JMS specification for a detailed definition of selector expressions.
	 * <p>Note: The message selector may be replaced at runtime, with the listener
	 * container picking up the new selector value immediately (works e.g. with
	 * DefaultMessageListenerContainer, as long as the cache level is less than
	 * CACHE_CONSUMER). However, this is considered advanced usage; use it with care!
	 */
	public void setMessageSelector(@Nullable String messageSelector) {
		this.messageSelector = messageSelector;
	}

	/**
	 * Return the JMS message selector expression (or {@code null} if none).
	 */
	@Nullable
	public String getMessageSelector() {
		return this.messageSelector;
	}


	/**
	 * Set the message listener implementation to register.
	 * This can be either a standard JMS {@link MessageListener} object
	 * or a Spring {@link SessionAwareMessageListener} object.
	 * <p>Note: The message listener may be replaced at runtime, with the listener
	 * container picking up the new listener object immediately (works e.g. with
	 * DefaultMessageListenerContainer, as long as the cache level is less than
	 * CACHE_CONSUMER). However, this is considered advanced usage; use it with care!
	 * @throws IllegalArgumentException if the supplied listener is not a
	 * {@link MessageListener} or a {@link SessionAwareMessageListener}
	 * @see javax.jms.MessageListener
	 * @see SessionAwareMessageListener
	 */
	public void setMessageListener(@Nullable Object messageListener) {
		checkMessageListener(messageListener);
		this.messageListener = messageListener;
		if (messageListener != null && this.subscriptionName == null) {
			this.subscriptionName = getDefaultSubscriptionName(messageListener);
		}
	}

	/**
	 * Return the message listener object to register.
	 */
	@Nullable
	public Object getMessageListener() {
		return this.messageListener;
	}

	/**
	 * Check the given message listener, throwing an exception
	 * if it does not correspond to a supported listener type.
	 * <p>By default, only a standard JMS {@link MessageListener} object or a
	 * Spring {@link SessionAwareMessageListener} object will be accepted.
	 * @param messageListener the message listener object to check
	 * @throws IllegalArgumentException if the supplied listener is not a
	 * {@link MessageListener} or a {@link SessionAwareMessageListener}
	 * @see javax.jms.MessageListener
	 * @see SessionAwareMessageListener
	 */
	protected void checkMessageListener(@Nullable Object messageListener) {
		if (messageListener != null && !(messageListener instanceof MessageListener ||
				messageListener instanceof SessionAwareMessageListener)) {
			throw new IllegalArgumentException(
					"Message listener needs to be of type [" + MessageListener.class.getName() +
					"] or [" + SessionAwareMessageListener.class.getName() + "]");
		}
	}

	/**
	 * Determine the default subscription name for the given message listener.
	 * @param messageListener the message listener object to check
	 * @return the default subscription name
	 * @see SubscriptionNameProvider
	 */
	protected String getDefaultSubscriptionName(Object messageListener) {
		if (messageListener instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) messageListener).getSubscriptionName();
		}
		else {
			return messageListener.getClass().getName();
		}
	}

	/**
	 * Set whether to make the subscription durable. The durable subscription name
	 * to be used can be specified through the "subscriptionName" property.
	 * <p>Default is "false". Set this to "true" to register a durable subscription,
	 * typically in combination with a "subscriptionName" value (unless
	 * your message listener class name is good enough as subscription name).
	 * <p>Only makes sense when listening to a topic (pub-sub domain),
	 * therefore this method switches the "pubSubDomain" flag as well.
	 * @see #setSubscriptionName
	 * @see #setPubSubDomain
	 */
	public void setSubscriptionDurable(boolean subscriptionDurable) {
		this.subscriptionDurable = subscriptionDurable;
		if (subscriptionDurable) {
			setPubSubDomain(true);
		}
	}

	/**
	 * Return whether to make the subscription durable.
	 */
	public boolean isSubscriptionDurable() {
		return this.subscriptionDurable;
	}

	/**
	 * Set whether to make the subscription shared. The shared subscription name
	 * to be used can be specified through the "subscriptionName" property.
	 * <p>Default is "false". Set this to "true" to register a shared subscription,
	 * typically in combination with a "subscriptionName" value (unless
	 * your message listener class name is good enough as subscription name).
	 * Note that shared subscriptions may also be durable, so this flag can
	 * (and often will) be combined with "subscriptionDurable" as well.
	 * <p>Only makes sense when listening to a topic (pub-sub domain),
	 * therefore this method switches the "pubSubDomain" flag as well.
	 * <p><b>Requires a JMS 2.0 compatible message broker.</b>
	 * @since 4.1
	 * @see #setSubscriptionName
	 * @see #setSubscriptionDurable
	 * @see #setPubSubDomain
	 */
	public void setSubscriptionShared(boolean subscriptionShared) {
		this.subscriptionShared = subscriptionShared;
		if (subscriptionShared) {
			setPubSubDomain(true);
		}
	}

	/**
	 * Return whether to make the subscription shared.
	 * @since 4.1
	 */
	public boolean isSubscriptionShared() {
		return this.subscriptionShared;
	}

	/**
	 * Set the name of a subscription to create. To be applied in case
	 * of a topic (pub-sub domain) with a shared or durable subscription.
	 * <p>The subscription name needs to be unique within this client's
	 * JMS client id. Default is the class name of the specified message listener.
	 * <p>Note: Only 1 concurrent consumer (which is the default of this
	 * message listener container) is allowed for each subscription,
	 * except for a shared subscription (which requires JMS 2.0).
	 * @since 4.1
	 * @see #setPubSubDomain
	 * @see #setSubscriptionDurable
	 * @see #setSubscriptionShared
	 * @see #setClientId
	 * @see #setMessageListener
	 */
	public void setSubscriptionName(@Nullable String subscriptionName) {
		this.subscriptionName = subscriptionName;
	}

	/**
	 * Return the name of a subscription to create, if any.
	 * @since 4.1
	 */
	@Nullable
	public String getSubscriptionName() {
		return this.subscriptionName;
	}

	/**
	 * Set the name of a durable subscription to create. This method switches
	 * to pub-sub domain mode and activates subscription durability as well.
	 * <p>The durable subscription name needs to be unique within this client's
	 * JMS client id. Default is the class name of the specified message listener.
	 * <p>Note: Only 1 concurrent consumer (which is the default of this
	 * message listener container) is allowed for each durable subscription,
	 * except for a shared durable subscription (which requires JMS 2.0).
	 * @see #setPubSubDomain
	 * @see #setSubscriptionDurable
	 * @see #setSubscriptionShared
	 * @see #setClientId
	 * @see #setMessageListener
	 */
	public void setDurableSubscriptionName(@Nullable String durableSubscriptionName) {
		this.subscriptionName = durableSubscriptionName;
		this.subscriptionDurable = (durableSubscriptionName != null);
	}

	/**
	 * Return the name of a durable subscription to create, if any.
	 */
	@Nullable
	public String getDurableSubscriptionName() {
		return (this.subscriptionDurable ? this.subscriptionName : null);
	}

	/**
	 * Set whether to inhibit the delivery of messages published by its own connection.
	 * Default is "false".
	 * @since 4.1
	 * @see javax.jms.Session#createConsumer(javax.jms.Destination, String, boolean)
	 */
	public void setPubSubNoLocal(boolean pubSubNoLocal) {
		this.pubSubNoLocal = pubSubNoLocal;
	}

	/**
	 * Return whether to inhibit the delivery of messages published by its own connection.
	 * @since 4.1
	 */
	public boolean isPubSubNoLocal() {
		return this.pubSubNoLocal;
	}

	/**
	 * Configure the reply destination type. By default, the configured {@code pubSubDomain}
	 * value is used (see {@link #isPubSubDomain()}.
	 * <p>This setting primarily indicates what type of destination to resolve if dynamic
	 * destinations are enabled.
	 * @param replyPubSubDomain "true" for the Publish/Subscribe domain ({@link Topic Topics}),
	 * "false" for the Point-to-Point domain ({@link Queue Queues})
	 * @since 4.2
	 * @see #setDestinationResolver
	 */
	public void setReplyPubSubDomain(boolean replyPubSubDomain) {
		this.replyPubSubDomain = replyPubSubDomain;
	}

	/**
	 * Return whether the Publish/Subscribe domain ({@link javax.jms.Topic Topics}) is used
	 * for replies. Otherwise, the Point-to-Point domain ({@link javax.jms.Queue Queues})
	 * is used.
	 * @since 4.2
	 */
	@Override
	public boolean isReplyPubSubDomain() {
		if (this.replyPubSubDomain != null) {
			return this.replyPubSubDomain;
		}
		else {
			return isPubSubDomain();
		}
	}

	/**
	 * Configure the {@link QosSettings} to use when sending a reply. Can be set to
	 * {@code null} to indicate that the broker's defaults should be used.
	 * @param replyQosSettings the QoS settings to use when sending a reply or {@code null}
	 * to use the default vas.
	 * @since 5.0
	 */
	public void setReplyQosSettings(@Nullable QosSettings replyQosSettings) {
		this.replyQosSettings = replyQosSettings;
	}

	@Override
	@Nullable
	public QosSettings getReplyQosSettings() {
		return this.replyQosSettings;
	}

	/**
	 * Set the {@link MessageConverter} strategy for converting JMS Messages.
	 * @since 4.1
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	@Override
	@Nullable
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * Set the JMS ExceptionListener to notify in case of a JMSException thrown
	 * by the registered message listener or the invocation infrastructure.
	 */
	public void setExceptionListener(@Nullable ExceptionListener exceptionListener) {
		this.exceptionListener = exceptionListener;
	}

	/**
	 * Return the JMS ExceptionListener to notify in case of a JMSException thrown
	 * by the registered message listener or the invocation infrastructure, if any.
	 */
	@Nullable
	public ExceptionListener getExceptionListener() {
		return this.exceptionListener;
	}

	/**
	 * Set the ErrorHandler to be invoked in case of any uncaught exceptions thrown
	 * while processing a Message.
	 * <p>By default, there will be <b>no</b> ErrorHandler so that error-level
	 * logging is the only result.
	 */
	public void setErrorHandler(@Nullable ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	/**
	 * Return the ErrorHandler to be invoked in case of any uncaught exceptions thrown
	 * while processing a Message.
	 * @since 4.1
	 */
	@Nullable
	public ErrorHandler getErrorHandler() {
		return this.errorHandler;
	}

	/**
	 * Set whether to expose the listener JMS Session to a registered
	 * {@link SessionAwareMessageListener} as well as to
	 * {@link org.springframework.jms.core.JmsTemplate} calls.
	 * <p>Default is "true", reusing the listener's {@link Session}.
	 * Turn this off to expose a fresh JMS Session fetched from the same
	 * underlying JMS {@link Connection} instead, which might be necessary
	 * on some JMS providers.
	 * <p>Note that Sessions managed by an external transaction manager will
	 * always get exposed to {@link org.springframework.jms.core.JmsTemplate}
	 * calls. So in terms of JmsTemplate exposure, this setting only affects
	 * locally transacted Sessions.
	 * @see SessionAwareMessageListener
	 */
	public void setExposeListenerSession(boolean exposeListenerSession) {
		this.exposeListenerSession = exposeListenerSession;
	}

	/**
	 * Return whether to expose the listener JMS {@link Session} to a
	 * registered {@link SessionAwareMessageListener}.
	 */
	public boolean isExposeListenerSession() {
		return this.exposeListenerSession;
	}

	/**
	 * Set whether to accept received messages while the listener container
	 * in the process of stopping.
	 * <p>Default is "false", rejecting such messages through aborting the
	 * receive attempt. Switch this flag on to fully process such messages
	 * even in the stopping phase, with the drawback that even newly sent
	 * messages might still get processed (if coming in before all receive
	 * timeouts have expired).
	 * <p><b>NOTE:</b> Aborting receive attempts for such incoming messages
	 * might lead to the provider's retry count decreasing for the affected
	 * messages. If you have a high number of concurrent consumers, make sure
	 * that the number of retries is higher than the number of consumers,
	 * to be on the safe side for all potential stopping scenarios.
	 */
	public void setAcceptMessagesWhileStopping(boolean acceptMessagesWhileStopping) {
		this.acceptMessagesWhileStopping = acceptMessagesWhileStopping;
	}

	/**
	 * Return whether to accept received messages while the listener container
	 * in the process of stopping.
	 */
	public boolean isAcceptMessagesWhileStopping() {
		return this.acceptMessagesWhileStopping;
	}

	@Override
	protected void validateConfiguration() {
		if (this.destination == null) {
			throw new IllegalArgumentException("Property 'destination' or 'destinationName' is required");
		}
	}

	@Override
	public void setupMessageListener(Object messageListener) {
		setMessageListener(messageListener);
	}


	//-------------------------------------------------------------------------
	// Template methods for listener execution
	//-------------------------------------------------------------------------

	/**
	 * Execute the specified listener,
	 * committing or rolling back the transaction afterwards (if necessary).
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @see #invokeListener
	 * @see #commitIfNecessary
	 * @see #rollbackOnExceptionIfNecessary
	 * @see #handleListenerException
	 */
	protected void executeListener(Session session, Message message) {
		try {
			doExecuteListener(session, message);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * Execute the specified listener,
	 * committing or rolling back the transaction afterwards (if necessary).
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see #invokeListener
	 * @see #commitIfNecessary
	 * @see #rollbackOnExceptionIfNecessary
	 * @see #convertJmsAccessException
	 */
	protected void doExecuteListener(Session session, Message message) throws JMSException {
		if (!isAcceptMessagesWhileStopping() && !isRunning()) {
			if (logger.isWarnEnabled()) {
				logger.warn("Rejecting received message because of the listener container " +
						"having been stopped in the meantime: " + message);
			}
			rollbackIfNecessary(session);
			throw new MessageRejectedWhileStoppingException();
		}

		try {
			invokeListener(session, message);
		}
		catch (JMSException | RuntimeException | Error ex) {
			rollbackOnExceptionIfNecessary(session, ex);
			throw ex;
		}
		commitIfNecessary(session, message);
	}

	/**
	 * Invoke the specified listener: either as standard JMS MessageListener
	 * or (preferably) as Spring SessionAwareMessageListener.
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageListener
	 */
	@SuppressWarnings("rawtypes")
	protected void invokeListener(Session session, Message message) throws JMSException {
		Object listener = getMessageListener();

		if (listener instanceof SessionAwareMessageListener) {
			doInvokeListener((SessionAwareMessageListener) listener, session, message);
		}
		else if (listener instanceof MessageListener) {
			doInvokeListener((MessageListener) listener, message);
		}
		else if (listener != null) {
			throw new IllegalArgumentException(
					"Only MessageListener and SessionAwareMessageListener supported: " + listener);
		}
		else {
			throw new IllegalStateException("No message listener specified - see property 'messageListener'");
		}
	}

	/**
	 * Invoke the specified listener as Spring SessionAwareMessageListener,
	 * exposing a new JMS Session (potentially with its own transaction)
	 * to the listener if demanded.
	 * @param listener the Spring SessionAwareMessageListener to invoke
	 * @param session the JMS Session to operate on
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see SessionAwareMessageListener
	 * @see #setExposeListenerSession
	 */
	@SuppressWarnings({"rawtypes", "unchecked"})
	protected void doInvokeListener(SessionAwareMessageListener listener, Session session, Message message)
			throws JMSException {

		Connection conToClose = null;
		Session sessionToClose = null;
		try {
			Session sessionToUse = session;
			if (!isExposeListenerSession()) {
				// We need to expose a separate Session.
				conToClose = createConnection();
				sessionToClose = createSession(conToClose);
				sessionToUse = sessionToClose;
			}
			// Actually invoke the message listener...
			listener.onMessage(message, sessionToUse);
			// Clean up specially exposed Session, if any.
			if (sessionToUse != session) {
				if (sessionToUse.getTransacted() && isSessionLocallyTransacted(sessionToUse)) {
					// Transacted session created by this container -> commit.
					JmsUtils.commitIfNecessary(sessionToUse);
				}
			}
		}
		finally {
			JmsUtils.closeSession(sessionToClose);
			JmsUtils.closeConnection(conToClose);
		}
	}

	/**
	 * Invoke the specified listener as standard JMS MessageListener.
	 * <p>Default implementation performs a plain invocation of the
	 * {@code onMessage} method.
	 * @param listener the JMS MessageListener to invoke
	 * @param message the received JMS Message
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.MessageListener#onMessage
	 */
	protected void doInvokeListener(MessageListener listener, Message message) throws JMSException {
		listener.onMessage(message);
	}

	/**
	 * Perform a commit or message acknowledgement, as appropriate.
	 * @param session the JMS Session to commit
	 * @param message the Message to acknowledge
	 * @throws javax.jms.JMSException in case of commit failure
	 */
	protected void commitIfNecessary(Session session, @Nullable Message message) throws JMSException {
		// Commit session or acknowledge message.
		if (session.getTransacted()) {
			// Commit necessary - but avoid commit call within a JTA transaction.
			if (isSessionLocallyTransacted(session)) {
				// Transacted session created by this container -> commit.
				JmsUtils.commitIfNecessary(session);
			}
		}
		else if (message != null && isClientAcknowledge(session)) {
			message.acknowledge();
		}
	}

	/**
	 * Perform a rollback, if appropriate.
	 * @param session the JMS Session to rollback
	 * @throws javax.jms.JMSException in case of a rollback error
	 */
	protected void rollbackIfNecessary(Session session) throws JMSException {
		if (session.getTransacted()) {
			if (isSessionLocallyTransacted(session)) {
				// Transacted session created by this container -> rollback.
				JmsUtils.rollbackIfNecessary(session);
			}
		}
		else if (isClientAcknowledge(session)) {
			session.recover();
		}
	}

	/**
	 * Perform a rollback, handling rollback exceptions properly.
	 * @param session the JMS Session to rollback
	 * @param ex the thrown application exception or error
	 * @throws javax.jms.JMSException in case of a rollback error
	 */
	protected void rollbackOnExceptionIfNecessary(Session session, Throwable ex) throws JMSException {
		try {
			if (session.getTransacted()) {
				if (isSessionLocallyTransacted(session)) {
					// Transacted session created by this container -> rollback.
					if (logger.isDebugEnabled()) {
						logger.debug("Initiating transaction rollback on application exception", ex);
					}
					JmsUtils.rollbackIfNecessary(session);
				}
			}
			else if (isClientAcknowledge(session)) {
				session.recover();
			}
		}
		catch (IllegalStateException ex2) {
			logger.debug("Could not roll back because Session already closed", ex2);
		}
		catch (JMSException | RuntimeException | Error ex2) {
			logger.error("Application exception overridden by rollback error", ex);
			throw ex2;
		}
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
		return isSessionTransacted();
	}

	/**
	 * Create a JMS MessageConsumer for the given Session and Destination.
	 * <p>This implementation uses JMS 1.1 API.
	 * @param session the JMS Session to create a MessageConsumer for
	 * @param destination the JMS Destination to create a MessageConsumer for
	 * @return the new JMS MessageConsumer
	 * @throws javax.jms.JMSException if thrown by JMS API methods
	 */
	protected MessageConsumer createConsumer(Session session, Destination destination) throws JMSException {
		if (isPubSubDomain() && destination instanceof Topic) {
			if (isSubscriptionShared()) {
				return (isSubscriptionDurable() ?
						session.createSharedDurableConsumer((Topic) destination, getSubscriptionName(), getMessageSelector()) :
						session.createSharedConsumer((Topic) destination, getSubscriptionName(), getMessageSelector()));
			}
			else if (isSubscriptionDurable()) {
				return session.createDurableSubscriber(
						(Topic) destination, getSubscriptionName(), getMessageSelector(), isPubSubNoLocal());
			}
			else {
				// Only pass in the NoLocal flag in case of a Topic (pub-sub mode):
				// Some JMS providers, such as WebSphere MQ 6.0, throw IllegalStateException
				// in case of the NoLocal flag being specified for a Queue.
				return session.createConsumer(destination, getMessageSelector(), isPubSubNoLocal());
			}
		}
		else {
			return session.createConsumer(destination, getMessageSelector());
		}
	}

	/**
	 * Handle the given exception that arose during listener execution.
	 * <p>The default implementation logs the exception at warn level,
	 * not propagating it to the JMS provider &mdash; assuming that all handling of
	 * acknowledgement and/or transactions is done by this listener container.
	 * This can be overridden in subclasses.
	 * @param ex the exception to handle
	 */
	protected void handleListenerException(Throwable ex) {
		if (ex instanceof MessageRejectedWhileStoppingException) {
			// Internal exception - has been handled before.
			return;
		}
		if (ex instanceof JMSException) {
			invokeExceptionListener((JMSException) ex);
		}
		if (isActive()) {
			// Regular case: failed while active.
			// Invoke ErrorHandler if available.
			invokeErrorHandler(ex);
		}
		else {
			// Rare case: listener thread failed after container shutdown.
			// Log at debug level, to avoid spamming the shutdown log.
			logger.debug("Listener exception after container shutdown", ex);
		}
	}

	/**
	 * Invoke the registered JMS ExceptionListener, if any.
	 * @param ex the exception that arose during JMS processing
	 * @see #setExceptionListener
	 */
	protected void invokeExceptionListener(JMSException ex) {
		ExceptionListener exceptionListener = getExceptionListener();
		if (exceptionListener != null) {
			exceptionListener.onException(ex);
		}
	}

	/**
	 * Invoke the registered ErrorHandler, if any. Log at warn level otherwise.
	 * @param ex the uncaught error that arose during JMS processing.
	 * @see #setErrorHandler
	 */
	protected void invokeErrorHandler(Throwable ex) {
		ErrorHandler errorHandler = getErrorHandler();
		if (errorHandler != null) {
			errorHandler.handleError(ex);
		}
		else {
			logger.warn("Execution of JMS message listener failed, and no ErrorHandler has been set.", ex);
		}
	}


	/**
	 * Internal exception class that indicates a rejected message on shutdown.
	 * Used to trigger a rollback for an external transaction manager in that case.
	 */
	@SuppressWarnings("serial")
	private static class MessageRejectedWhileStoppingException extends RuntimeException {
	}

}
