/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.Map;

import javax.jms.JMSException;
import javax.jms.Session;

import org.springframework.jms.support.converter.JmsHeaderMapper;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.messaging.support.MessageBuilder;

/**
 * A {@link javax.jms.MessageListener} adapter that invokes a configurable
 * {@link InvocableHandlerMethod}.
 *
 * <p>Wraps the incoming {@link javax.jms.Message} to Spring's {@link Message}
 * abstraction, copying the JMS standard headers using a configurable
 * {@link JmsHeaderMapper}.
 *
 * <p>The original {@link javax.jms.Message} and the {@link javax.jms.Session}
 * are provided as additional arguments so that these can be injected as
 * method arguments if necessary.
 *
 * @author Stephane Nicoll
 * @since 4.1
 * @see Message
 * @see JmsHeaderMapper
 * @see InvocableHandlerMethod
 */
public class MessagingMessageListenerAdapter extends AbstractAdaptableMessageListener {

	private InvocableHandlerMethod handlerMethod;


	/**
	 * Set the {@link InvocableHandlerMethod} to use to invoke the method
	 * processing an incoming {@link javax.jms.Message}.
	 */
	public void setHandlerMethod(InvocableHandlerMethod handlerMethod) {
		this.handlerMethod = handlerMethod;
	}

	@Override
	public void onMessage(javax.jms.Message jmsMessage, Session session) throws JMSException {
		try {
			Message<?> message = toMessagingMessage(jmsMessage);
			if (logger.isDebugEnabled()) {
				logger.debug("Processing [" + message + "]");
			}
			Object result = handlerMethod.invoke(message, jmsMessage, session);
			if (result != null) {
				handleResult(result, jmsMessage, session);
			}
			else {
				logger.trace("No result object given - no result to handle");
			}
		}
		catch (MessagingException e) {
			throw new ListenerExecutionFailedException(createMessagingErrorMessage("Listener method could not " +
					"be invoked with the incoming message"), e);
		}
		catch (Exception e) {
			throw new ListenerExecutionFailedException("Listener method '"
					+ handlerMethod.getMethod().toGenericString() + "' threw exception", e);
		}
	}

	@SuppressWarnings("unchecked")
	protected Message<?> toMessagingMessage(javax.jms.Message jmsMessage) throws JMSException {
		Map<String, Object> mappedHeaders = getHeaderMapper().toHeaders(jmsMessage);
		Object convertedObject = extractMessage(jmsMessage);
		MessageBuilder<Object> builder = (convertedObject instanceof org.springframework.messaging.Message) ?
				MessageBuilder.fromMessage((org.springframework.messaging.Message<Object>) convertedObject) :
				MessageBuilder.withPayload(convertedObject);
		return builder.copyHeadersIfAbsent(mappedHeaders).build();
	}

	private String createMessagingErrorMessage(String description) {
		StringBuilder sb = new StringBuilder(description).append("\n")
				.append("Endpoint handler details:\n")
				.append("Method [").append(handlerMethod.getMethod()).append("]\n")
				.append("Bean [").append(handlerMethod.getBean()).append("]\n");
		return sb.toString();
	}

}
