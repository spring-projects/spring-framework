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

package org.springframework.jms.listener.adapter;

import jakarta.jms.JMSException;
import jakarta.jms.Session;
import org.jspecify.annotations.Nullable;

import org.springframework.core.MethodParameter;
import org.springframework.jms.listener.SubscriptionNameProvider;
import org.springframework.jms.support.JmsHeaderMapper;
import org.springframework.jms.support.converter.MessageConversionException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.AbstractMessageSendingTemplate;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * A {@link jakarta.jms.MessageListener} adapter that invokes a configurable
 * {@link InvocableHandlerMethod}.
 *
 * <p>Wraps the incoming {@link jakarta.jms.Message} in Spring's {@link Message}
 * abstraction, copying the JMS standard headers using a configurable
 * {@link JmsHeaderMapper}.
 *
 * <p>The original {@link jakarta.jms.Message} and the {@link jakarta.jms.Session}
 * are provided as additional arguments so that these can be injected as
 * method arguments if necessary.
 *
 * <p>Note that {@code MessagingMessageListenerAdapter} implements
 * {@link SubscriptionNameProvider} in order to provide a meaningful default
 * subscription name. See {@link #getSubscriptionName()} for details.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 * @since 4.1
 * @see Message
 * @see JmsHeaderMapper
 * @see InvocableHandlerMethod
 */
public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener
		implements SubscriptionNameProvider {

	private @Nullable InvocableHandlerMethod handlerMethod;


	/**
	 * Set the {@link InvocableHandlerMethod} to use to invoke the method
	 * processing an incoming {@link jakarta.jms.Message}.
	 */
	public void setHandlerMethod(InvocableHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	private InvocableHandlerMethod getHandlerMethod() {
		Assert.state(this.handlerMethod != null, "No HandlerMethod set");
		return this.handlerMethod;
	}


	@Override
	public void onMessage(jakarta.jms.Message jmsMessage, @Nullable Session session) throws JMSException {
		Message<?> message = toMessagingMessage(jmsMessage);
		if (logger.isDebugEnabled()) {
			logger.debug("Processing [" + message + "]");
		}
		Object result = invokeHandler(jmsMessage, session, message);
		if (result != null) {
			handleResult(result, jmsMessage, session);
		}
		else {
			logger.trace("No result object given - no result to handle");
		}
	}

	protected Message<?> toMessagingMessage(jakarta.jms.Message jmsMessage) {
		try {
			return (Message<?>) getMessagingMessageConverter().fromMessage(jmsMessage);
		}
		catch (JMSException ex) {
			throw new MessageConversionException("Could not convert JMS message", ex);
		}
	}

	/**
	 * Invoke the handler, wrapping any exception in a {@link ListenerExecutionFailedException}
	 * with a dedicated error message.
	 */
	private @Nullable Object invokeHandler(jakarta.jms.Message jmsMessage, @Nullable Session session, Message<?> message) {
		InvocableHandlerMethod handlerMethod = getHandlerMethod();
		try {
			return handlerMethod.invoke(message, jmsMessage, session);
		}
		catch (MessagingException ex) {
			throw new ListenerExecutionFailedException(
					createMessagingErrorMessage("Listener method could not be invoked with incoming message"), ex);
		}
		catch (Exception ex) {
			throw new ListenerExecutionFailedException("Listener method '" +
					handlerMethod.getMethod().toGenericString() + "' threw exception", ex);
		}
	}

	private String createMessagingErrorMessage(String description) {
		InvocableHandlerMethod handlerMethod = getHandlerMethod();
		StringBuilder sb = new StringBuilder(description).append('\n')
				.append("Endpoint handler details:\n")
				.append("Method [").append(handlerMethod.getMethod()).append("]\n")
				.append("Bean [").append(handlerMethod.getBean()).append("]\n");
		return sb.toString();
	}

	@Override
	protected Object preProcessResponse(Object result) {
		MethodParameter returnType = getHandlerMethod().getReturnType();
		MessageBuilder<?> messageBuilder = (result instanceof Message<?> message ?
				MessageBuilder.fromMessage(message) :
				MessageBuilder.withPayload(result));
		return messageBuilder
				.setHeader(AbstractMessageSendingTemplate.CONVERSION_HINT_HEADER, returnType)
				.build();
	}

	/**
	 * Generate a subscription name for this {@code MessageListener} adapter based
	 * on the following rules.
	 * <ul>
	 * <li>If the {@link #setHandlerMethod(InvocableHandlerMethod) handlerMethod}
	 * has been set, the generated subscription name takes the form of
	 * {@code handlerMethod.getBeanType().getName() + "." + handlerMethod.getMethod().getName()}.</li>
	 * <li>Otherwise, the generated subscription name is the result of invoking
	 * {@code getClass().getName()}, which aligns with the default behavior of
	 * {@link org.springframework.jms.listener.AbstractMessageListenerContainer}.</li>
	 * </ul>
	 * @since 5.3.26
	 * @see SubscriptionNameProvider#getSubscriptionName()
	 */
	@Override
	public String getSubscriptionName() {
		if (this.handlerMethod != null) {
			return this.handlerMethod.getBeanType().getName() + "." + this.handlerMethod.getMethod().getName();
		}
		else {
			return getClass().getName();
		}
	}

}
