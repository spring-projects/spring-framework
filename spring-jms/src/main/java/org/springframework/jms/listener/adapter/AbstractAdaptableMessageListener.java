/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import javax.jms.BytesMessage;
import javax.jms.Destination;
import javax.jms.InvalidDestinationException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.QosSettings;
import org.springframework.jms.support.SimpleJmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.MessagingMessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.converter.SmartMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

/**
 * An abstract JMS {@link MessageListener} adapter providing the necessary
 * infrastructure to extract the payload of a JMS {@link Message}.
 *
 * @author Juergen Hoeller
 * @author Stephane Nicoll
 * @since 4.1
 * @see MessageListener
 * @see SessionAwareMessageListener
 */
public abstract class AbstractAdaptableMessageListener
		implements MessageListener, SessionAwareMessageListener<Message> {

	/** Logger available to subclasses. */
	protected final Log logger = LogFactory.getLog(getClass());

	@Nullable
	private Object defaultResponseDestination;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	@Nullable
	private MessageConverter messageConverter = new SimpleMessageConverter();

	private final MessagingMessageConverterAdapter messagingMessageConverter = new MessagingMessageConverterAdapter();

	@Nullable
	private QosSettings responseQosSettings;


	/**
	 * Set the default destination to send response messages to. This will be applied
	 * in case of a request message that does not carry a "JMSReplyTo" field.
	 * <p>Response destinations are only relevant for listener methods that return
	 * result objects, which will be wrapped in a response message and sent to a
	 * response destination.
	 * <p>Alternatively, specify a "defaultResponseQueueName" or "defaultResponseTopicName",
	 * to be dynamically resolved via the DestinationResolver.
	 * @see #setDefaultResponseQueueName(String)
	 * @see #setDefaultResponseTopicName(String)
	 * @see #getResponseDestination
	 */
	public void setDefaultResponseDestination(Destination destination) {
		this.defaultResponseDestination = destination;
	}

	/**
	 * Set the name of the default response queue to send response messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultResponseDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultResponseDestination(javax.jms.Destination)
	 */
	public void setDefaultResponseQueueName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, false);
	}

	/**
	 * Set the name of the default response topic to send response messages to.
	 * This will be applied in case of a request message that does not carry a
	 * "JMSReplyTo" field.
	 * <p>Alternatively, specify a JMS Destination object as "defaultResponseDestination".
	 * @see #setDestinationResolver
	 * @see #setDefaultResponseDestination(javax.jms.Destination)
	 */
	public void setDefaultResponseTopicName(String destinationName) {
		this.defaultResponseDestination = new DestinationNameHolder(destinationName, true);
	}

	/**
	 * Set the DestinationResolver that should be used to resolve response
	 * destination names for this adapter.
	 * <p>The default resolver is a DynamicDestinationResolver. Specify a
	 * JndiDestinationResolver for resolving destination names as JNDI locations.
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(DestinationResolver destinationResolver) {
		Assert.notNull(destinationResolver, "DestinationResolver must not be null");
		this.destinationResolver = destinationResolver;
	}

	/**
	 * Return the DestinationResolver for this adapter.
	 */
	protected DestinationResolver getDestinationResolver() {
		return this.destinationResolver;
	}

	/**
	 * Set the converter that will convert incoming JMS messages to
	 * listener method arguments, and objects returned from listener
	 * methods back to JMS messages.
	 * <p>The default converter is a {@link SimpleMessageConverter}, which is able
	 * to handle {@link javax.jms.BytesMessage BytesMessages},
	 * {@link javax.jms.TextMessage TextMessages} and
	 * {@link javax.jms.ObjectMessage ObjectMessages}.
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the converter that will convert incoming JMS messages to
	 * listener method arguments, and objects returned from listener
	 * methods back to JMS messages.
	 */
	@Nullable
	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * Set the {@link JmsHeaderMapper} implementation to use to map the standard
	 * JMS headers. By default, a {@link SimpleJmsHeaderMapper} is used.
	 * @see SimpleJmsHeaderMapper
	 */
	public void setHeaderMapper(JmsHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "HeaderMapper must not be null");
		this.messagingMessageConverter.setHeaderMapper(headerMapper);
	}

	/**
	 * Return the {@link MessagingMessageConverter} for this listener,
	 * being able to convert {@link org.springframework.messaging.Message}.
	 */
	protected final MessagingMessageConverter getMessagingMessageConverter() {
		return this.messagingMessageConverter;
	}

	/**
	 * Set the {@link QosSettings} to use when sending a response. Can be set to
	 * {@code null} to indicate that the broker's defaults should be used.
	 * @param responseQosSettings the QoS settings to use when sending a response or
	 * {@code null} to use the default values.
	 * @since 5.0
	 */
	public void setResponseQosSettings(@Nullable QosSettings responseQosSettings) {
		this.responseQosSettings = responseQosSettings;
	}

	/**
	 * Return the {@link QosSettings} to use when sending a response,
	 * or {@code null} if the defaults should be used.
	 * @since 5.0
	 */
	@Nullable
	protected QosSettings getResponseQosSettings() {
		return this.responseQosSettings;
	}


	/**
	 * Standard JMS {@link MessageListener} entry point.
	 * <p>Delegates the message to the target listener method, with appropriate
	 * conversion of the message argument. In case of an exception, the
	 * {@link #handleListenerException(Throwable)} method will be invoked.
	 * <p><b>Note:</b> Does not support sending response messages based on
	 * result objects returned from listener methods. Use the
	 * {@link SessionAwareMessageListener} entry point (typically through a Spring
	 * message listener container) for handling result objects as well.
	 * @param message the incoming JMS message
	 * @see #handleListenerException
	 * @see #onMessage(javax.jms.Message, javax.jms.Session)
	 */
	@Override
	public void onMessage(Message message) {
		try {
			onMessage(message, null);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	@Override
	public abstract void onMessage(Message message, @Nullable Session session) throws JMSException;

	/**
	 * Handle the given exception that arose during listener execution.
	 * The default implementation logs the exception at error level.
	 * <p>This method only applies when used as standard JMS {@link MessageListener}.
	 * In case of the Spring {@link SessionAwareMessageListener} mechanism,
	 * exceptions get handled by the caller instead.
	 * @param ex the exception to handle
	 * @see #onMessage(javax.jms.Message)
	 */
	protected void handleListenerException(Throwable ex) {
		logger.error("Listener execution failed", ex);
	}


	/**
	 * Extract the message body from the given JMS message.
	 * @param message the JMS {@code Message}
	 * @return the content of the message, to be passed into the listener method
	 * as an argument
	 * @throws MessageConversionException if the message could not be extracted
	 */
	protected Object extractMessage(Message message)  {
		try {
			MessageConverter converter = getMessageConverter();
			if (converter != null) {
				return converter.fromMessage(message);
			}
			return message;
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * Handle the given result object returned from the listener method,
	 * sending a response message back.
	 * @param result the result object to handle (never {@code null})
	 * @param request the original request message
	 * @param session the JMS Session to operate on (may be {@code null})
	 * @throws ReplyFailureException if the response message could not be sent
	 * @see #buildMessage
	 * @see #postProcessResponse
	 * @see #getResponseDestination
	 * @see #sendResponse
	 */
	protected void handleResult(Object result, Message request, @Nullable Session session) {
		if (session != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Listener method returned result [" + result +
						"] - generating response message for it");
			}
			try {
				Message response = buildMessage(session, result);
				postProcessResponse(request, response);
				Destination destination = getResponseDestination(request, response, session, result);
				sendResponse(session, destination, response);
			}
			catch (Exception ex) {
				throw new ReplyFailureException("Failed to send reply with payload [" + result + "]", ex);
			}
		}

		else {
			// No JMS Session available
			if (logger.isWarnEnabled()) {
				logger.warn("Listener method returned result [" + result +
						"]: not generating response message for it because of no JMS Session given");
			}
		}
	}

	/**
	 * Build a JMS message to be sent as response based on the given result object.
	 * @param session the JMS Session to operate on
	 * @param result the content of the message, as returned from the listener method
	 * @return the JMS {@code Message} (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setMessageConverter
	 */
	protected Message buildMessage(Session session, Object result) throws JMSException {
		Object content = preProcessResponse(result instanceof JmsResponse
				? ((JmsResponse<?>) result).getResponse() : result);

		MessageConverter converter = getMessageConverter();
		if (converter != null) {
			if (content instanceof org.springframework.messaging.Message) {
				return this.messagingMessageConverter.toMessage(content, session);
			}
			else {
				return converter.toMessage(content, session);
			}
		}

		if (!(content instanceof Message)) {
			throw new MessageConversionException(
					"No MessageConverter specified - cannot handle message [" + content + "]");
		}
		return (Message) content;
	}

	/**
	 * Pre-process the given result before it is converted to a {@link Message}.
	 * @param result the result of the invocation
	 * @return the payload response to handle, either the {@code result} argument
	 * or any other object (for instance wrapping the result).
	 * @since 4.3
	 */
	protected Object preProcessResponse(Object result) {
		return result;
	}

	/**
	 * Post-process the given response message before it will be sent.
	 * <p>The default implementation sets the response's correlation id
	 * to the request message's correlation id, if any; otherwise to the
	 * request message id.
	 * @param request the original incoming JMS message
	 * @param response the outgoing JMS message about to be sent
	 * @throws JMSException if thrown by JMS API methods
	 * @see javax.jms.Message#setJMSCorrelationID
	 */
	protected void postProcessResponse(Message request, Message response) throws JMSException {
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
	}

	private Destination getResponseDestination(Message request, Message response, Session session, Object result)
			throws JMSException {

		if (result instanceof JmsResponse) {
			JmsResponse<?> jmsResponse = (JmsResponse) result;
			Destination destination = jmsResponse.resolveDestination(getDestinationResolver(), session);
			if (destination != null) {
				return destination;
			}
		}
		return getResponseDestination(request, response, session);
	}

	/**
	 * Determine a response destination for the given message.
	 * <p>The default implementation first checks the JMS Reply-To
	 * {@link Destination} of the supplied request; if that is not {@code null}
	 * it is returned; if it is {@code null}, then the configured
	 * {@link #resolveDefaultResponseDestination default response destination}
	 * is returned; if this too is {@code null}, then an
	 * {@link javax.jms.InvalidDestinationException} is thrown.
	 * @param request the original incoming JMS message
	 * @param response the outgoing JMS message about to be sent
	 * @param session the JMS Session to operate on
	 * @return the response destination (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @throws javax.jms.InvalidDestinationException if no {@link Destination} can be determined
	 * @see #setDefaultResponseDestination
	 * @see javax.jms.Message#getJMSReplyTo()
	 */
	protected Destination getResponseDestination(Message request, Message response, Session session)
			throws JMSException {

		Destination replyTo = request.getJMSReplyTo();
		if (replyTo == null) {
			replyTo = resolveDefaultResponseDestination(session);
			if (replyTo == null) {
				throw new InvalidDestinationException("Cannot determine response destination: " +
						"Request message does not contain reply-to destination, and no default response destination set.");
			}
		}
		return replyTo;
	}

	/**
	 * Resolve the default response destination into a JMS {@link Destination}, using this
	 * accessor's {@link DestinationResolver} in case of a destination name.
	 * @return the located {@link Destination}
	 * @throws javax.jms.JMSException if resolution failed
	 * @see #setDefaultResponseDestination
	 * @see #setDefaultResponseQueueName
	 * @see #setDefaultResponseTopicName
	 * @see #setDestinationResolver
	 */
	@Nullable
	protected Destination resolveDefaultResponseDestination(Session session) throws JMSException {
		if (this.defaultResponseDestination instanceof Destination) {
			return (Destination) this.defaultResponseDestination;
		}
		if (this.defaultResponseDestination instanceof DestinationNameHolder) {
			DestinationNameHolder nameHolder = (DestinationNameHolder) this.defaultResponseDestination;
			return getDestinationResolver().resolveDestinationName(session, nameHolder.name, nameHolder.isTopic);
		}
		return null;
	}

	/**
	 * Send the given response message to the given destination.
	 * @param response the JMS message to send
	 * @param destination the JMS destination to send to
	 * @param session the JMS session to operate on
	 * @throws JMSException if thrown by JMS API methods
	 * @see #postProcessProducer
	 * @see javax.jms.Session#createProducer
	 * @see javax.jms.MessageProducer#send
	 */
	protected void sendResponse(Session session, Destination destination, Message response) throws JMSException {
		MessageProducer producer = session.createProducer(destination);
		try {
			postProcessProducer(producer, response);
			QosSettings settings = getResponseQosSettings();
			if (settings != null) {
				producer.send(response, settings.getDeliveryMode(), settings.getPriority(),
						settings.getTimeToLive());
			}
			else {
				producer.send(response);
			}
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * Post-process the given message producer before using it to send the response.
	 * <p>The default implementation is empty.
	 * @param producer the JMS message producer that will be used to send the message
	 * @param response the outgoing JMS message about to be sent
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected void postProcessProducer(MessageProducer producer, Message response) throws JMSException {
	}


	/**
	 * A {@link MessagingMessageConverter} that lazily invoke payload extraction and
	 * delegate it to {@link #extractMessage(javax.jms.Message)} in order to enforce
	 * backward compatibility.
	 */
	private class MessagingMessageConverterAdapter extends MessagingMessageConverter {

		@SuppressWarnings("unchecked")
		@Override
		public Object fromMessage(javax.jms.Message message) throws JMSException, MessageConversionException {
			return new LazyResolutionMessage(message);
		}

		@Override
		protected Object extractPayload(Message message) throws JMSException {
			Object payload = extractMessage(message);
			if (message instanceof BytesMessage) {
				try {
					// In case the BytesMessage is going to be received as a user argument:
					// reset it, otherwise it would appear empty to such processing code...
					((BytesMessage) message).reset();
				}
				catch (JMSException ex) {
					// Continue since the BytesMessage typically won't be used any further.
					logger.debug("Failed to reset BytesMessage after payload extraction", ex);
				}
			}
			return payload;
		}

		@Override
		protected Message createMessageForPayload(Object payload, Session session, @Nullable Object conversionHint)
				throws JMSException {

			MessageConverter converter = getMessageConverter();
			if (converter == null) {
				throw new IllegalStateException("No message converter, cannot handle '" + payload + "'");
			}
			if (converter instanceof SmartMessageConverter) {
				return ((SmartMessageConverter) converter).toMessage(payload, session, conversionHint);

			}
			return converter.toMessage(payload, session);
		}


		protected class LazyResolutionMessage implements org.springframework.messaging.Message<Object> {

			private final javax.jms.Message message;

			@Nullable
			private Object payload;

			@Nullable
			private MessageHeaders headers;

			public LazyResolutionMessage(javax.jms.Message message) {
				this.message = message;
			}

			@Override
			public Object getPayload() {
				if (this.payload == null) {
					try {
						this.payload = unwrapPayload();
					}
					catch (JMSException ex) {
						throw new MessageConversionException(
								"Failed to extract payload from [" + this.message + "]", ex);
					}
				}
				return this.payload;
			}

			/**
			 * Extract the payload of the current message. Since we deferred the resolution
			 * of the payload, a custom converter may still return a full message for it. In
			 * this case, its payload is returned.
			 * @return the payload of the message
			 */
			private Object unwrapPayload() throws JMSException {
				Object payload = extractPayload(this.message);
				if (payload instanceof org.springframework.messaging.Message) {
					return ((org.springframework.messaging.Message) payload).getPayload();
				}
				return payload;
			}

			@Override
			public MessageHeaders getHeaders() {
				if (this.headers == null) {
					this.headers = extractHeaders(this.message);
				}
				return this.headers;
			}
		}
	}


	/**
	 * Internal class combining a destination name
	 * and its target destination type (queue or topic).
	 */
	private static class DestinationNameHolder {

		public final String name;

		public final boolean isTopic;

		public DestinationNameHolder(String name, boolean isTopic) {
			this.name = name;
			this.isTopic = isTopic;
		}
	}

}
