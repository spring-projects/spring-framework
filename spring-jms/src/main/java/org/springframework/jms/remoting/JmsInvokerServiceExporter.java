/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.jms.remoting;

import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Session;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.lang.Nullable;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationBasedExporter;
import org.springframework.remoting.support.RemoteInvocationResult;

/**
 * JMS message listener that exports the specified service bean as a
 * JMS service endpoint, accessible via a JMS invoker proxy.
 *
 * <p>Note that this class implements Spring's
 * {@link org.springframework.jms.listener.SessionAwareMessageListener}
 * interface, since it requires access to the active JMS Session.
 * Hence, this class can only be used with message listener containers
 * which support the SessionAwareMessageListener interface (e.g. Spring's
 * {@link org.springframework.jms.listener.DefaultMessageListenerContainer}).
 *
 * <p>Thanks to James Strachan for the original prototype that this
 * JMS invoker mechanism was inspired by!
 *
 * @author Juergen Hoeller
 * @author James Strachan
 * @since 2.0
 * @see JmsInvokerClientInterceptor
 * @see JmsInvokerProxyFactoryBean
 */
public class JmsInvokerServiceExporter extends RemoteInvocationBasedExporter
		implements SessionAwareMessageListener<Message>, InitializingBean {

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private boolean ignoreInvalidRequests = true;

	@Nullable
	private Object proxy;


	/**
	 * Specify the MessageConverter to use for turning request messages into
	 * {@link org.springframework.remoting.support.RemoteInvocation} objects,
	 * as well as {@link org.springframework.remoting.support.RemoteInvocationResult}
	 * objects into response messages.
	 * <p>Default is a {@link org.springframework.jms.support.converter.SimpleMessageConverter},
	 * using a standard JMS {@link javax.jms.ObjectMessage} for each invocation /
	 * invocation result object.
	 * <p>Custom implementations may generally adapt Serializables into
	 * special kinds of messages, or might be specifically tailored for
	 * translating RemoteInvocation(Result)s into specific kinds of messages.
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * Set whether invalidly formatted messages should be discarded.
	 * Default is "true".
	 * <p>Switch this flag to "false" to throw an exception back to the
	 * listener container. This will typically lead to redelivery of
	 * the message, which is usually undesirable - since the message
	 * content will be the same (that is, still invalid).
	 */
	public void setIgnoreInvalidRequests(boolean ignoreInvalidRequests) {
		this.ignoreInvalidRequests = ignoreInvalidRequests;
	}

	@Override
	public void afterPropertiesSet() {
		this.proxy = getProxyForService();
	}


	@Override
	public void onMessage(Message requestMessage, Session session) throws JMSException {
		RemoteInvocation invocation = readRemoteInvocation(requestMessage);
		if (invocation != null) {
			RemoteInvocationResult result = invokeAndCreateResult(invocation, this.proxy);
			writeRemoteInvocationResult(requestMessage, session, result);
		}
	}

	/**
	 * Read a RemoteInvocation from the given JMS message.
	 * @param requestMessage current request message
	 * @return the RemoteInvocation object (or {@code null}
	 * in case of an invalid message that will simply be ignored)
	 * @throws javax.jms.JMSException in case of message access failure
	 */
	@Nullable
	protected RemoteInvocation readRemoteInvocation(Message requestMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(requestMessage);
		if (content instanceof RemoteInvocation) {
			return (RemoteInvocation) content;
		}
		return onInvalidRequest(requestMessage);
	}


	/**
	 * Send the given RemoteInvocationResult as a JMS message to the originator.
	 * @param requestMessage current request message
	 * @param session the JMS Session to use
	 * @param result the RemoteInvocationResult object
	 * @throws javax.jms.JMSException if thrown by trying to send the message
	 */
	protected void writeRemoteInvocationResult(
			Message requestMessage, Session session, RemoteInvocationResult result) throws JMSException {

		Message response = createResponseMessage(requestMessage, session, result);
		MessageProducer producer = session.createProducer(requestMessage.getJMSReplyTo());
		try {
			producer.send(response);
		}
		finally {
			JmsUtils.closeMessageProducer(producer);
		}
	}

	/**
	 * Create the invocation result response message.
	 * <p>The default implementation creates a JMS ObjectMessage for the given
	 * RemoteInvocationResult object. It sets the response's correlation id
	 * to the request message's correlation id, if any; otherwise to the
	 * request message id.
	 * @param request the original request message
	 * @param session the JMS session to use
	 * @param result the invocation result
	 * @return the message response to send
	 * @throws javax.jms.JMSException if creating the message failed
	 */
	protected Message createResponseMessage(Message request, Session session, RemoteInvocationResult result)
			throws JMSException {

		Message response = this.messageConverter.toMessage(result, session);
		String correlation = request.getJMSCorrelationID();
		if (correlation == null) {
			correlation = request.getJMSMessageID();
		}
		response.setJMSCorrelationID(correlation);
		return response;
	}

	/**
	 * Callback that is invoked by {@link #readRemoteInvocation}
	 * when it encounters an invalid request message.
	 * <p>The default implementation either discards the invalid message or
	 * throws a MessageFormatException - according to the "ignoreInvalidRequests"
	 * flag, which is set to "true" (that is, discard invalid messages) by default.
	 * @param requestMessage the invalid request message
	 * @return the RemoteInvocation to expose for the invalid request (typically
	 * {@code null} in case of an invalid message that will simply be ignored)
	 * @throws javax.jms.JMSException in case of the invalid request supposed
	 * to lead to an exception (instead of ignoring it)
	 * @see #readRemoteInvocation
	 * @see #setIgnoreInvalidRequests
	 */
	@Nullable
	protected RemoteInvocation onInvalidRequest(Message requestMessage) throws JMSException {
		if (this.ignoreInvalidRequests) {
			if (logger.isDebugEnabled()) {
				logger.debug("Invalid request message will be discarded: " + requestMessage);
			}
			return null;
		}
		else {
			throw new MessageFormatException("Invalid request message: " + requestMessage);
		}
	}

}
