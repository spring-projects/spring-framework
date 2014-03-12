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

package org.springframework.jms.config;

import java.lang.reflect.Method;

import org.springframework.jms.listener.MessageListenerContainer;
import org.springframework.jms.listener.adapter.MessagingMessageListenerAdapter;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.handler.invocation.InvocableHandlerMethod;
import org.springframework.util.Assert;

/**
 * A {@link JmsListenerEndpoint} providing the method to invoke to process
 * an incoming message for this endpoint.
 *
 * @author Stephane Nicoll
 * @since 4.1
 */
public class MethodJmsListenerEndpoint extends AbstractJmsListenerEndpoint {

	private Object bean;

	private Method method;

	private String responseDestination;

	private JmsHandlerMethodFactory jmsHandlerMethodFactory;

	/**
	 * Set the object instance that should manage this endpoint.
	 */
	public void setBean(Object bean) {
		this.bean = bean;
	}

	public Object getBean() {
		return bean;
	}

	/**
	 * Set the method to invoke to process a message managed by this
	 * endpoint.
	 */
	public void setMethod(Method method) {
		this.method = method;
	}

	public Method getMethod() {
		return method;
	}

	/**
	 * Set the name of the default response destination to send response messages to.
	 */
	public void setResponseDestination(String responseDestination) {
		this.responseDestination = responseDestination;
	}

	/**
	 * Return the name of the default response destination to send response messages to.
	 */
	public String getResponseDestination() {
		return responseDestination;
	}

	/**
	 * Set the {@link DefaultJmsHandlerMethodFactory} to use to build the
	 * {@link InvocableHandlerMethod} responsible to manage the invocation
	 * of this endpoint.
	 */
	public void setJmsHandlerMethodFactory(JmsHandlerMethodFactory jmsHandlerMethodFactory) {
		this.jmsHandlerMethodFactory = jmsHandlerMethodFactory;
	}

	@Override
	protected MessagingMessageListenerAdapter createMessageListener(MessageListenerContainer container) {
		Assert.state(jmsHandlerMethodFactory != null,
				"Could not create message listener, message listener factory not set.");
		MessagingMessageListenerAdapter messageListener = new MessagingMessageListenerAdapter();
		InvocableHandlerMethod invocableHandlerMethod =
				jmsHandlerMethodFactory.createInvocableHandlerMethod(getBean(), getMethod());
		messageListener.setHandlerMethod(invocableHandlerMethod);
		String responseDestination = getResponseDestination();
		if (responseDestination != null) {
			if (isQueue()) {
				messageListener.setDefaultResponseQueueName(responseDestination);
			}
			else {
				messageListener.setDefaultResponseTopicName(responseDestination);
			}
		}
		MessageConverter messageConverter = container.getMessageConverter();
		if (messageConverter != null) {
			messageListener.setMessageConverter(messageConverter);
		}
		return messageListener;
	}

	@Override
	protected StringBuilder getEndpointDescription() {
		return super.getEndpointDescription()
				.append(" | bean='")
				.append(this.bean)
				.append("'")
				.append(" | method='")
				.append(this.method)
				.append("'");
	}

}
