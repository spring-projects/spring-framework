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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.InvocationTargetException;
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
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.util.Assert;
import org.springframework.util.MethodInvoker;
import org.springframework.util.ObjectUtils;

/**
 * Message listener adapter that delegates the handling of messages to target
 * listener methods via reflection, with flexible message type conversion.
 * Allows listener methods to operate on message content types, completely
 * independent from the JMS API.
 *
 * <p>By default, the content of incoming JMS messages gets extracted before
 * being passed into the target listener method, to let the target method
 * operate on message content types such as String or byte array instead of
 * the raw {@link Message}. Message type conversion is delegated to a Spring
 * JMS {@link MessageConverter}. By default, a {@link SimpleMessageConverter}
 * will be used. (If you do not want such automatic message conversion taking
 * place, then be sure to set the {@link #setMessageConverter MessageConverter}
 * to {@code null}.)
 *
 * <p>If a target listener method returns a non-null object (typically of a
 * message content type such as {@code String} or byte array), it will get
 * wrapped in a JMS {@code Message} and sent to the response destination
 * (either the JMS "reply-to" destination or a
 * {@link #setDefaultResponseDestination(javax.jms.Destination) specified default
 * destination}).
 *
 * <p><b>Note:</b> The sending of response messages is only available when
 * using the {@link SessionAwareMessageListener} entry point (typically through a
 * Spring message listener container). Usage as standard JMS {@link MessageListener}
 * does <i>not</i> support the generation of response messages.
 *
 * <p>Find below some examples of method signatures compliant with this
 * adapter class. This first example handles all {@code Message} types
 * and gets passed the contents of each {@code Message} type as an
 * argument. No {@code Message} will be sent back as all of these
 * methods return {@code void}.
 *
 * <pre class="code">public interface MessageContentsDelegate {
 *    void handleMessage(String text);
 *    void handleMessage(Map map);
 *    void handleMessage(byte[] bytes);
 *    void handleMessage(Serializable obj);
 * }</pre>
 *
 * This next example handles all {@code Message} types and gets
 * passed the actual (raw) {@code Message} as an argument. Again, no
 * {@code Message} will be sent back as all of these methods return
 * {@code void}.
 *
 * <pre class="code">public interface RawMessageDelegate {
 *    void handleMessage(TextMessage message);
 *    void handleMessage(MapMessage message);
 *    void handleMessage(BytesMessage message);
 *    void handleMessage(ObjectMessage message);
 * }</pre>
 *
 * This next example illustrates a {@code Message} delegate
 * that just consumes the {@code String} contents of
 * {@link javax.jms.TextMessage TextMessages}. Notice also how the
 * name of the {@code Message} handling method is different from the
 * {@link #ORIGINAL_DEFAULT_LISTENER_METHOD original} (this will have to
 * be configured in the attandant bean definition). Again, no {@code Message}
 * will be sent back as the method returns {@code void}.
 *
 * <pre class="code">public interface TextMessageContentDelegate {
 *    void onMessage(String text);
 * }</pre>
 *
 * This final example illustrates a {@code Message} delegate
 * that just consumes the {@code String} contents of
 * {@link javax.jms.TextMessage TextMessages}. Notice how the return type
 * of this method is {@code String}: This will result in the configured
 * {@link MessageListenerAdapter} sending a {@link javax.jms.TextMessage} in response.
 *
 * <pre class="code">public interface ResponsiveTextMessageContentDelegate {
 *    String handleMessage(String text);
 * }</pre>
 *
 * For further examples and discussion please do refer to the Spring
 * reference documentation which describes this class (and it's attendant
 * XML configuration) in detail.
 *
 * @author Juergen Hoeller
 * @since 2.0
 * @see #setDelegate
 * @see #setDefaultListenerMethod
 * @see #setDefaultResponseDestination
 * @see #setMessageConverter
 * @see org.springframework.jms.support.converter.SimpleMessageConverter
 * @see org.springframework.jms.listener.SessionAwareMessageListener
 * @see org.springframework.jms.listener.AbstractMessageListenerContainer#setMessageListener
 */
public class MessageListenerAdapter
		implements MessageListener, SessionAwareMessageListener<Message>, SubscriptionNameProvider {

	/**
	 * Out-of-the-box value for the default listener method: "handleMessage".
	 */
	public static final String ORIGINAL_DEFAULT_LISTENER_METHOD = "handleMessage";


	/** Logger available to subclasses */
	protected final Log logger = LogFactory.getLog(getClass());

	private Object delegate;

	private String defaultListenerMethod = ORIGINAL_DEFAULT_LISTENER_METHOD;

	private Object defaultResponseDestination;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private MessageConverter messageConverter;


	/**
	 * Create a new {@link MessageListenerAdapter} with default settings.
	 */
	public MessageListenerAdapter() {
		initDefaultStrategies();
		this.delegate = this;
	}

	/**
	 * Create a new {@link MessageListenerAdapter} for the given delegate.
	 * @param delegate the delegate object
	 */
	public MessageListenerAdapter(Object delegate) {
		initDefaultStrategies();
		setDelegate(delegate);
	}


	/**
	 * Set a target object to delegate message listening to.
	 * Specified listener methods have to be present on this target object.
	 * <p>If no explicit delegate object has been specified, listener
	 * methods are expected to present on this adapter instance, that is,
	 * on a custom subclass of this adapter, defining listener methods.
	 */
	public void setDelegate(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
	}

	/**
	 * Return the target object to delegate message listening to.
	 */
	protected Object getDelegate() {
		return this.delegate;
	}

	/**
	 * Specify the name of the default listener method to delegate to,
	 * for the case where no specific listener method has been determined.
	 * Out-of-the-box value is {@link #ORIGINAL_DEFAULT_LISTENER_METHOD "handleMessage"}.
	 * @see #getListenerMethodName
	 */
	public void setDefaultListenerMethod(String defaultListenerMethod) {
		this.defaultListenerMethod = defaultListenerMethod;
	}

	/**
	 * Return the name of the default listener method to delegate to.
	 */
	protected String getDefaultListenerMethod() {
		return this.defaultListenerMethod;
	}

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
	public void setMessageConverter(MessageConverter messageConverter) {
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the converter that will convert incoming JMS messages to
	 * listener method arguments, and objects returned from listener
	 * methods back to JMS messages.
	 */
	protected MessageConverter getMessageConverter() {
		return this.messageConverter;
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
	public void onMessage(Message message) {
		try {
			onMessage(message, null);
		}
		catch (Throwable ex) {
			handleListenerException(ex);
		}
	}

	/**
	 * Spring {@link SessionAwareMessageListener} entry point.
	 * <p>Delegates the message to the target listener method, with appropriate
	 * conversion of the message argument. If the target method returns a
	 * non-null object, wrap in a JMS message and send it back.
	 * @param message the incoming JMS message
	 * @param session the JMS session to operate on
	 * @throws JMSException if thrown by JMS API methods
	 */
	@SuppressWarnings("unchecked")
	public void onMessage(Message message, Session session) throws JMSException {
		// Check whether the delegate is a MessageListener impl itself.
		// In that case, the adapter will simply act as a pass-through.
		Object delegate = getDelegate();
		if (delegate != this) {
			if (delegate instanceof SessionAwareMessageListener) {
				if (session != null) {
					((SessionAwareMessageListener) delegate).onMessage(message, session);
					return;
				}
				else if (!(delegate instanceof MessageListener)) {
					throw new javax.jms.IllegalStateException("MessageListenerAdapter cannot handle a " +
							"SessionAwareMessageListener delegate if it hasn't been invoked with a Session itself");
				}
			}
			if (delegate instanceof MessageListener) {
				((MessageListener) delegate).onMessage(message);
				return;
			}
		}

		// Regular case: find a handler method reflectively.
		Object convertedMessage = extractMessage(message);
		String methodName = getListenerMethodName(message, convertedMessage);
		if (methodName == null) {
			throw new javax.jms.IllegalStateException("No default listener method specified: " +
					"Either specify a non-null value for the 'defaultListenerMethod' property or " +
					"override the 'getListenerMethodName' method.");
		}

		// Invoke the handler method with appropriate arguments.
		Object[] listenerArguments = buildListenerArguments(convertedMessage);
		Object result = invokeListenerMethod(methodName, listenerArguments);
		if (result != null) {
			handleResult(result, message, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	public String getSubscriptionName() {
		Object delegate = getDelegate();
		if (delegate != this && delegate instanceof SubscriptionNameProvider) {
			return ((SubscriptionNameProvider) delegate).getSubscriptionName();
		}
		else {
			return delegate.getClass().getName();
		}
	}


	/**
	 * Initialize the default implementations for the adapter's strategies.
	 * @see #setMessageConverter
	 * @see org.springframework.jms.support.converter.SimpleMessageConverter
	 */
	protected void initDefaultStrategies() {
		setMessageConverter(new SimpleMessageConverter());
	}

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
	 * @return the content of the message, to be passed into the
	 * listener method as argument
	 * @throws JMSException if thrown by JMS API methods
	 */
	protected Object extractMessage(Message message) throws JMSException {
		MessageConverter converter = getMessageConverter();
		if (converter != null) {
			return converter.fromMessage(message);
		}
		return message;
	}

	/**
	 * Determine the name of the listener method that is supposed to
	 * handle the given message.
	 * <p>The default implementation simply returns the configured
	 * default listener method, if any.
	 * @param originalMessage the JMS request message
	 * @param extractedMessage the converted JMS request message,
	 * to be passed into the listener method as argument
	 * @return the name of the listener method (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @see #setDefaultListenerMethod
	 */
	protected String getListenerMethodName(Message originalMessage, Object extractedMessage) throws JMSException {
		return getDefaultListenerMethod();
	}

	/**
	 * Build an array of arguments to be passed into the target listener method.
	 * Allows for multiple method arguments to be built from a single message object.
	 * <p>The default implementation builds an array with the given message object
	 * as sole element. This means that the extracted message will always be passed
	 * into a <i>single</i> method argument, even if it is an array, with the target
	 * method having a corresponding single argument of the array's type declared.
	 * <p>This can be overridden to treat special message content such as arrays
	 * differently, for example passing in each element of the message array
	 * as distinct method argument.
	 * @param extractedMessage the content of the message
	 * @return the array of arguments to be passed into the
	 * listener method (each element of the array corresponding
	 * to a distinct method argument)
	 */
	protected Object[] buildListenerArguments(Object extractedMessage) {
		return new Object[] {extractedMessage};
	}

	/**
	 * Invoke the specified listener method.
	 * @param methodName the name of the listener method
	 * @param arguments the message arguments to be passed in
	 * @return the result returned from the listener method
	 * @throws JMSException if thrown by JMS API methods
	 * @see #getListenerMethodName
	 * @see #buildListenerArguments
	 */
	protected Object invokeListenerMethod(String methodName, Object[] arguments) throws JMSException {
		try {
			MethodInvoker methodInvoker = new MethodInvoker();
			methodInvoker.setTargetObject(getDelegate());
			methodInvoker.setTargetMethod(methodName);
			methodInvoker.setArguments(arguments);
			methodInvoker.prepare();
			return methodInvoker.invoke();
		}
		catch (InvocationTargetException ex) {
			Throwable targetEx = ex.getTargetException();
			if (targetEx instanceof JMSException) {
				throw (JMSException) targetEx;
			}
			else {
				throw new ListenerExecutionFailedException(
						"Listener method '" + methodName + "' threw exception", targetEx);
			}
		}
		catch (Throwable ex) {
			throw new ListenerExecutionFailedException("Failed to invoke target method '" + methodName +
					"' with arguments " + ObjectUtils.nullSafeToString(arguments), ex);
		}
	}


	/**
	 * Handle the given result object returned from the listener method,
	 * sending a response message back.
	 * @param result the result object to handle (never {@code null})
	 * @param request the original request message
	 * @param session the JMS Session to operate on (may be {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @see #buildMessage
	 * @see #postProcessResponse
	 * @see #getResponseDestination
	 * @see #sendResponse
	 */
	protected void handleResult(Object result, Message request, Session session) throws JMSException {
		if (session != null) {
			if (logger.isDebugEnabled()) {
				logger.debug("Listener method returned result [" + result +
						"] - generating response message for it");
			}
			Message response = buildMessage(session, result);
			postProcessResponse(request, response);
			Destination destination = getResponseDestination(request, response, session);
			sendResponse(session, destination,  response);
		}
		else {
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
		MessageConverter converter = getMessageConverter();
		if (converter != null) {
			return converter.toMessage(result, session);
		}
		else {
			if (!(result instanceof Message)) {
				throw new MessageConversionException(
						"No MessageConverter specified - cannot handle message [" + result + "]");
			}
			return (Message) result;
		}
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

	/**
	 * Determine a response destination for the given message.
	 * <p>The default implementation first checks the JMS Reply-To
	 * {@link Destination} of the supplied request; if that is not {@code null}
	 * it is returned; if it is {@code null}, then the configured
	 * {@link #resolveDefaultResponseDestination default response destination}
	 * is returned; if this too is {@code null}, then an
	 * {@link InvalidDestinationException} is thrown.
	 * @param request the original incoming JMS message
	 * @param response the outgoing JMS message about to be sent
	 * @param session the JMS Session to operate on
	 * @return the response destination (never {@code null})
	 * @throws JMSException if thrown by JMS API methods
	 * @throws InvalidDestinationException if no {@link Destination} can be determined
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
			producer.send(response);
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
