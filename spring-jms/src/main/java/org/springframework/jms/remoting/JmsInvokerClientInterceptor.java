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

package org.springframework.jms.remoting;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageFormatException;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.jms.TemporaryQueue;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.jms.connection.ConnectionFactoryUtils;
import org.springframework.jms.support.JmsUtils;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.jms.support.converter.SimpleMessageConverter;
import org.springframework.jms.support.destination.DestinationResolver;
import org.springframework.jms.support.destination.DynamicDestinationResolver;
import org.springframework.lang.Nullable;
import org.springframework.remoting.RemoteAccessException;
import org.springframework.remoting.RemoteInvocationFailureException;
import org.springframework.remoting.RemoteTimeoutException;
import org.springframework.remoting.support.DefaultRemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocation;
import org.springframework.remoting.support.RemoteInvocationFactory;
import org.springframework.remoting.support.RemoteInvocationResult;
import org.springframework.util.Assert;

/**
 * {@link org.aopalliance.intercept.MethodInterceptor} for accessing a
 * JMS-based remote service.
 *
 * <p>Serializes remote invocation objects and deserializes remote invocation
 * result objects. Uses Java serialization just like RMI, but with the JMS
 * provider as communication infrastructure.
 *
 * <p>To be configured with a {@link javax.jms.QueueConnectionFactory} and a
 * target queue (either as {@link javax.jms.Queue} reference or as queue name).
 *
 * <p>Thanks to James Strachan for the original prototype that this
 * JMS invoker mechanism was inspired by!
 *
 * @author Juergen Hoeller
 * @author James Strachan
 * @author Stephane Nicoll
 * @since 2.0
 * @see #setConnectionFactory
 * @see #setQueue
 * @see #setQueueName
 * @see org.springframework.jms.remoting.JmsInvokerServiceExporter
 * @see org.springframework.jms.remoting.JmsInvokerProxyFactoryBean
 */
public class JmsInvokerClientInterceptor implements MethodInterceptor, InitializingBean {

	@Nullable
	private ConnectionFactory connectionFactory;

	@Nullable
	private Object queue;

	private DestinationResolver destinationResolver = new DynamicDestinationResolver();

	private RemoteInvocationFactory remoteInvocationFactory = new DefaultRemoteInvocationFactory();

	private MessageConverter messageConverter = new SimpleMessageConverter();

	private long receiveTimeout = 0;


	/**
	 * Set the QueueConnectionFactory to use for obtaining JMS QueueConnections.
	 */
	public void setConnectionFactory(@Nullable ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	/**
	 * Return the QueueConnectionFactory to use for obtaining JMS QueueConnections.
	 */
	@Nullable
	protected ConnectionFactory getConnectionFactory() {
		return this.connectionFactory;
	}

	/**
	 * Set the target Queue to send invoker requests to.
	 */
	public void setQueue(Queue queue) {
		this.queue = queue;
	}

	/**
	 * Set the name of target queue to send invoker requests to.
	 * <p>The specified name will be dynamically resolved via the
	 * {@link #setDestinationResolver DestinationResolver}.
	 */
	public void setQueueName(String queueName) {
		this.queue = queueName;
	}

	/**
	 * Set the DestinationResolver that is to be used to resolve Queue
	 * references for this accessor.
	 * <p>The default resolver is a {@code DynamicDestinationResolver}. Specify a
	 * {@code JndiDestinationResolver} for resolving destination names as JNDI locations.
	 * @see org.springframework.jms.support.destination.DynamicDestinationResolver
	 * @see org.springframework.jms.support.destination.JndiDestinationResolver
	 */
	public void setDestinationResolver(@Nullable DestinationResolver destinationResolver) {
		this.destinationResolver =
				(destinationResolver != null ? destinationResolver : new DynamicDestinationResolver());
	}

	/**
	 * Set the {@link RemoteInvocationFactory} to use for this accessor.
	 * <p>Default is a {@link DefaultRemoteInvocationFactory}.
	 * <p>A custom invocation factory can add further context information
	 * to the invocation, for example user credentials.
	 */
	public void setRemoteInvocationFactory(@Nullable RemoteInvocationFactory remoteInvocationFactory) {
		this.remoteInvocationFactory =
				(remoteInvocationFactory != null ? remoteInvocationFactory : new DefaultRemoteInvocationFactory());
	}

	/**
	 * Specify the {@link MessageConverter} to use for turning
	 * {@link org.springframework.remoting.support.RemoteInvocation}
	 * objects into request messages, as well as response messages into
	 * {@link org.springframework.remoting.support.RemoteInvocationResult} objects.
	 * <p>Default is a {@link SimpleMessageConverter}, using a standard JMS
	 * {@link javax.jms.ObjectMessage} for each invocation / invocation result
	 * object.
	 * <p>Custom implementations may generally adapt {@link java.io.Serializable}
	 * objects into special kinds of messages, or might be specifically tailored for
	 * translating {@code RemoteInvocation(Result)s} into specific kinds of messages.
	 */
	public void setMessageConverter(@Nullable MessageConverter messageConverter) {
		this.messageConverter = (messageConverter != null ? messageConverter : new SimpleMessageConverter());
	}

	/**
	 * Set the timeout to use for receiving the response message for a request
	 * (in milliseconds).
	 * <p>The default is 0, which indicates a blocking receive without timeout.
	 * @see javax.jms.MessageConsumer#receive(long)
	 * @see javax.jms.MessageConsumer#receive()
	 */
	public void setReceiveTimeout(long receiveTimeout) {
		this.receiveTimeout = receiveTimeout;
	}

	/**
	 * Return the timeout to use for receiving the response message for a request
	 * (in milliseconds).
	 */
	protected long getReceiveTimeout() {
		return this.receiveTimeout;
	}


	@Override
	public void afterPropertiesSet() {
		if (getConnectionFactory() == null) {
			throw new IllegalArgumentException("Property 'connectionFactory' is required");
		}
		if (this.queue == null) {
			throw new IllegalArgumentException("'queue' or 'queueName' is required");
		}
	}


	@Override
	@Nullable
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (AopUtils.isToStringMethod(methodInvocation.getMethod())) {
			return "JMS invoker proxy for queue [" + this.queue + "]";
		}

		RemoteInvocation invocation = createRemoteInvocation(methodInvocation);
		RemoteInvocationResult result;
		try {
			result = executeRequest(invocation);
		}
		catch (JMSException ex) {
			throw convertJmsInvokerAccessException(ex);
		}
		try {
			return recreateRemoteInvocationResult(result);
		}
		catch (Throwable ex) {
			if (result.hasInvocationTargetException()) {
				throw ex;
			}
			else {
				throw new RemoteInvocationFailureException("Invocation of method [" + methodInvocation.getMethod() +
						"] failed in JMS invoker remote service at queue [" + this.queue + "]", ex);
			}
		}
	}

	/**
	 * Create a new {@code RemoteInvocation} object for the given AOP method invocation.
	 * <p>The default implementation delegates to the {@link RemoteInvocationFactory}.
	 * <p>Can be overridden in subclasses to provide custom {@code RemoteInvocation}
	 * subclasses, containing additional invocation parameters like user credentials.
	 * Note that it is preferable to use a custom {@code RemoteInvocationFactory} which
	 * is a reusable strategy.
	 * @param methodInvocation the current AOP method invocation
	 * @return the RemoteInvocation object
	 * @see RemoteInvocationFactory#createRemoteInvocation
	 */
	protected RemoteInvocation createRemoteInvocation(MethodInvocation methodInvocation) {
		return this.remoteInvocationFactory.createRemoteInvocation(methodInvocation);
	}

	/**
	 * Execute the given remote invocation, sending an invoker request message
	 * to this accessor's target queue and waiting for a corresponding response.
	 * @param invocation the RemoteInvocation to execute
	 * @return the RemoteInvocationResult object
	 * @throws JMSException in case of JMS failure
	 * @see #doExecuteRequest
	 */
	protected RemoteInvocationResult executeRequest(RemoteInvocation invocation) throws JMSException {
		Connection con = createConnection();
		Session session = null;
		try {
			session = createSession(con);
			Queue queueToUse = resolveQueue(session);
			Message requestMessage = createRequestMessage(session, invocation);
			con.start();
			Message responseMessage = doExecuteRequest(session, queueToUse, requestMessage);
			if (responseMessage != null) {
				return extractInvocationResult(responseMessage);
			}
			else {
				return onReceiveTimeout(invocation);
			}
		}
		finally {
			JmsUtils.closeSession(session);
			ConnectionFactoryUtils.releaseConnection(con, getConnectionFactory(), true);
		}
	}

	/**
	 * Create a new JMS Connection for this JMS invoker.
	 */
	protected Connection createConnection() throws JMSException {
		ConnectionFactory connectionFactory = getConnectionFactory();
		Assert.state(connectionFactory != null, "No ConnectionFactory set");
		return connectionFactory.createConnection();
	}

	/**
	 * Create a new JMS Session for this JMS invoker.
	 */
	protected Session createSession(Connection con) throws JMSException {
		return con.createSession(false, Session.AUTO_ACKNOWLEDGE);
	}

	/**
	 * Resolve this accessor's target queue.
	 * @param session the current JMS Session
	 * @return the resolved target Queue
	 * @throws JMSException if resolution failed
	 */
	protected Queue resolveQueue(Session session) throws JMSException {
		if (this.queue instanceof Queue) {
			return (Queue) this.queue;
		}
		else if (this.queue instanceof String) {
			return resolveQueueName(session, (String) this.queue);
		}
		else {
			throw new javax.jms.IllegalStateException(
					"Queue object [" + this.queue + "] is neither a [javax.jms.Queue] nor a queue name String");
		}
	}

	/**
	 * Resolve the given queue name into a JMS {@link javax.jms.Queue},
	 * via this accessor's {@link DestinationResolver}.
	 * @param session the current JMS Session
	 * @param queueName the name of the queue
	 * @return the located Queue
	 * @throws JMSException if resolution failed
	 * @see #setDestinationResolver
	 */
	protected Queue resolveQueueName(Session session, String queueName) throws JMSException {
		return (Queue) this.destinationResolver.resolveDestinationName(session, queueName, false);
	}

	/**
	 * Create the invoker request message.
	 * <p>The default implementation creates a JMS {@link javax.jms.ObjectMessage}
	 * for the given RemoteInvocation object.
	 * @param session the current JMS Session
	 * @param invocation the remote invocation to send
	 * @return the JMS Message to send
	 * @throws JMSException if the message could not be created
	 */
	protected Message createRequestMessage(Session session, RemoteInvocation invocation) throws JMSException {
		return this.messageConverter.toMessage(invocation, session);
	}

	/**
	 * Actually execute the given request, sending the invoker request message
	 * to the specified target queue and waiting for a corresponding response.
	 * <p>The default implementation is based on standard JMS send/receive,
	 * using a {@link javax.jms.TemporaryQueue} for receiving the response.
	 * @param session the JMS Session to use
	 * @param queue the resolved target Queue to send to
	 * @param requestMessage the JMS Message to send
	 * @return the RemoteInvocationResult object
	 * @throws JMSException in case of JMS failure
	 */
	@Nullable
	protected Message doExecuteRequest(Session session, Queue queue, Message requestMessage) throws JMSException {
		TemporaryQueue responseQueue = null;
		MessageProducer producer = null;
		MessageConsumer consumer = null;
		try {
			responseQueue = session.createTemporaryQueue();
			producer = session.createProducer(queue);
			consumer = session.createConsumer(responseQueue);
			requestMessage.setJMSReplyTo(responseQueue);
			producer.send(requestMessage);
			long timeout = getReceiveTimeout();
			return (timeout > 0 ? consumer.receive(timeout) : consumer.receive());
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
	 * Extract the invocation result from the response message.
	 * <p>The default implementation expects a JMS {@link javax.jms.ObjectMessage}
	 * carrying a {@link RemoteInvocationResult} object. If an invalid response
	 * message is encountered, the {@code onInvalidResponse} callback gets invoked.
	 * @param responseMessage the response message
	 * @return the invocation result
	 * @throws JMSException is thrown if a JMS exception occurs
	 * @see #onInvalidResponse
	 */
	protected RemoteInvocationResult extractInvocationResult(Message responseMessage) throws JMSException {
		Object content = this.messageConverter.fromMessage(responseMessage);
		if (content instanceof RemoteInvocationResult) {
			return (RemoteInvocationResult) content;
		}
		return onInvalidResponse(responseMessage);
	}

	/**
	 * Callback that is invoked by {@link #executeRequest} when the receive
	 * timeout has expired for the specified {@link RemoteInvocation}.
	 * <p>By default, an {@link RemoteTimeoutException} is thrown. Sub-classes
	 * can choose to either throw a more dedicated exception or even return
	 * a default {@link RemoteInvocationResult} as a fallback.
	 * @param invocation the invocation
	 * @return a default result when the receive timeout has expired
	 */
	protected RemoteInvocationResult onReceiveTimeout(RemoteInvocation invocation) {
		throw new RemoteTimeoutException("Receive timeout after " + this.receiveTimeout + " ms for " + invocation);
	}

	/**
	 * Callback that is invoked by {@link #extractInvocationResult} when
	 * it encounters an invalid response message.
	 * <p>The default implementation throws a {@link MessageFormatException}.
	 * @param responseMessage the invalid response message
	 * @return an alternative invocation result that should be returned to
	 * the caller (if desired)
	 * @throws JMSException if the invalid response should lead to an
	 * infrastructure exception propagated to the caller
	 * @see #extractInvocationResult
	 */
	protected RemoteInvocationResult onInvalidResponse(Message responseMessage) throws JMSException {
		throw new MessageFormatException("Invalid response message: " + responseMessage);
	}

	/**
	 * Recreate the invocation result contained in the given {@link RemoteInvocationResult}
	 * object.
	 * <p>The default implementation calls the default {@code recreate()} method.
	 * <p>Can be overridden in subclasses to provide custom recreation, potentially
	 * processing the returned result object.
	 * @param result the RemoteInvocationResult to recreate
	 * @return a return value if the invocation result is a successful return
	 * @throws Throwable if the invocation result is an exception
	 * @see org.springframework.remoting.support.RemoteInvocationResult#recreate()
	 */
	@Nullable
	protected Object recreateRemoteInvocationResult(RemoteInvocationResult result) throws Throwable {
		return result.recreate();
	}

	/**
	 * Convert the given JMS invoker access exception to an appropriate
	 * Spring {@link RemoteAccessException}.
	 * @param ex the exception to convert
	 * @return the RemoteAccessException to throw
	 */
	protected RemoteAccessException convertJmsInvokerAccessException(JMSException ex) {
		return new RemoteAccessException("Could not access JMS invoker queue [" + this.queue + "]", ex);
	}

}
