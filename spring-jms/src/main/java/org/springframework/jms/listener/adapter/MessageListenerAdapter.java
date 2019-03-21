/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.jms.listener.adapter;

import java.lang.reflect.InvocationTargetException;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.Session;

import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;
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
public class MessageListenerAdapter extends AbstractAdaptableMessageListener implements SubscriptionNameProvider {

	/**
	 * Out-of-the-box value for the default listener method: "handleMessage".
	 */
	public static final String ORIGINAL_DEFAULT_LISTENER_METHOD = "handleMessage";


	private Object delegate;

	private String defaultListenerMethod = ORIGINAL_DEFAULT_LISTENER_METHOD;


	/**
	 * Create a new {@link MessageListenerAdapter} with default settings.
	 */
	public MessageListenerAdapter() {
		this.delegate = this;
	}

	/**
	 * Create a new {@link MessageListenerAdapter} for the given delegate.
	 * @param delegate the delegate object
	 */
	public MessageListenerAdapter(Object delegate) {
		Assert.notNull(delegate, "Delegate must not be null");
		this.delegate = delegate;
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
	 * Spring {@link SessionAwareMessageListener} entry point.
	 * <p>Delegates the message to the target listener method, with appropriate
	 * conversion of the message argument. If the target method returns a
	 * non-null object, wrap in a JMS message and send it back.
	 * @param message the incoming JMS message
	 * @param session the JMS session to operate on
	 * @throws JMSException if thrown by JMS API methods
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void onMessage(Message message, @Nullable Session session) throws JMSException {
		// Check whether the delegate is a MessageListener impl itself.
		// In that case, the adapter will simply act as a pass-through.
		Object delegate = getDelegate();
		if (delegate != this) {
			if (delegate instanceof SessionAwareMessageListener) {
				Assert.state(session != null, "Session is required for SessionAwareMessageListener");
				((SessionAwareMessageListener<Message>) delegate).onMessage(message, session);
				return;
			}
			if (delegate instanceof MessageListener) {
				((MessageListener) delegate).onMessage(message);
				return;
			}
		}

		// Regular case: find a handler method reflectively.
		Object convertedMessage = extractMessage(message);
		String methodName = getListenerMethodName(message, convertedMessage);

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

	@Override
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
	@Nullable
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

}
