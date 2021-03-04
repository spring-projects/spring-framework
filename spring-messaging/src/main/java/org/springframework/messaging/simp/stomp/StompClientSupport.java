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

package org.springframework.messaging.simp.stomp;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import org.springframework.lang.Nullable;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.SimpleMessageConverter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Base class for STOMP client implementations.
 *
 * <p>Subclasses can connect over WebSocket or TCP using any library. When creating
 * a new connection, a subclass can create an instance of @link DefaultStompSession}
 * which extends {@link org.springframework.messaging.tcp.TcpConnectionHandler}
 * whose lifecycle methods the subclass must then invoke.
 *
 * <p>In effect, {@code TcpConnectionHandler} and {@code TcpConnection} are the
 * contracts that any subclass must adapt to while using {@link StompEncoder}
 * and {@link StompDecoder} to encode and decode STOMP messages.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public abstract class StompClientSupport {

	private MessageConverter messageConverter = new SimpleMessageConverter();

	@Nullable
	private TaskScheduler taskScheduler;

	private long[] defaultHeartbeat = new long[] {10000, 10000};

	private long receiptTimeLimit = TimeUnit.SECONDS.toMillis(15);


	/**
	 * Set the {@link MessageConverter} to use to convert the payload of incoming
	 * and outgoing messages to and from {@code byte[]} based on object type
	 * and the "content-type" header.
	 * <p>By default, {@link SimpleMessageConverter} is configured.
	 * @param messageConverter the message converter to use
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	/**
	 * Return the configured {@link MessageConverter}.
	 */
	public MessageConverter getMessageConverter() {
		return this.messageConverter;
	}

	/**
	 * Configure a scheduler to use for heartbeats and for receipt tracking.
	 * <p><strong>Note:</strong> Some transports have built-in support to work
	 * with heartbeats and therefore do not require a TaskScheduler.
	 * Receipts however, if needed, do require a TaskScheduler to be configured.
	 * <p>By default, this is not set.
	 */
	public void setTaskScheduler(@Nullable TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * The configured TaskScheduler.
	 */
	@Nullable
	public TaskScheduler getTaskScheduler() {
		return this.taskScheduler;
	}

	/**
	 * Configure the default value for the "heart-beat" header of the STOMP
	 * CONNECT frame. The first number represents how often the client will write
	 * or send a heart-beat. The second is how often the server should write.
	 * A value of 0 means no heart-beats.
	 * <p>By default this is set to "10000,10000" but subclasses may override
	 * that default and for example set it to "0,0" if they require a
	 * TaskScheduler to be configured first.
	 * <p><strong>Note:</strong> that a heartbeat is sent only in case of
	 * inactivity, i.e. when no other messages are sent. This can present a
	 * challenge when using an external broker since messages with a non-broker
	 * destination represent activity but aren't actually forwarded to the broker.
	 * In that case you can configure a `TaskScheduler` through the
	 * {@link org.springframework.messaging.simp.config.StompBrokerRelayRegistration}
	 * which ensures a heartbeat is forwarded to the broker also when only
	 * messages with a non-broker destination are sent.
	 * @param heartbeat the value for the CONNECT "heart-beat" header
	 * @see <a href="https://stomp.github.io/stomp-specification-1.2.html#Heart-beating">
	 * https://stomp.github.io/stomp-specification-1.2.html#Heart-beating</a>
	 */
	public void setDefaultHeartbeat(long[] heartbeat) {
		if (heartbeat.length != 2 || heartbeat[0] < 0 || heartbeat[1] < 0) {
			throw new IllegalArgumentException("Invalid heart-beat: " + Arrays.toString(heartbeat));
		}
		this.defaultHeartbeat = heartbeat;
	}

	/**
	 * Return the configured default heart-beat value (never {@code null}).
	 */
	public long[] getDefaultHeartbeat() {
		return this.defaultHeartbeat;
	}

	/**
	 * Determine whether heartbeats are enabled.
	 * <p>Returns {@code false} if {@link #setDefaultHeartbeat defaultHeartbeat}
	 * is set to "0,0", and {@code true} otherwise.
	 */
	public boolean isDefaultHeartbeatEnabled() {
		long[] heartbeat = getDefaultHeartbeat();
		return (heartbeat[0] != 0 && heartbeat[1] != 0);
	}

	/**
	 * Configure the number of milliseconds before a receipt is considered expired.
	 * <p>By default set to 15,000 (15 seconds).
	 */
	public void setReceiptTimeLimit(long receiptTimeLimit) {
		Assert.isTrue(receiptTimeLimit > 0, "Receipt time limit must be larger than zero");
		this.receiptTimeLimit = receiptTimeLimit;
	}

	/**
	 * Return the configured receipt time limit.
	 */
	public long getReceiptTimeLimit() {
		return this.receiptTimeLimit;
	}


	/**
	 * Factory method for create and configure a new session.
	 * @param connectHeaders headers for the STOMP CONNECT frame
	 * @param handler the handler for the STOMP session
	 * @return the created session
	 */
	protected ConnectionHandlingStompSession createSession(
			@Nullable StompHeaders connectHeaders, StompSessionHandler handler) {

		connectHeaders = processConnectHeaders(connectHeaders);
		DefaultStompSession session = new DefaultStompSession(handler, connectHeaders);
		session.setMessageConverter(getMessageConverter());
		session.setTaskScheduler(getTaskScheduler());
		session.setReceiptTimeLimit(getReceiptTimeLimit());
		return session;
	}

	/**
	 * Further initialize the StompHeaders, for example setting the heart-beat
	 * header if necessary.
	 * @param connectHeaders the headers to modify
	 * @return the modified headers
	 */
	protected StompHeaders processConnectHeaders(@Nullable StompHeaders connectHeaders) {
		connectHeaders = (connectHeaders != null ? connectHeaders : new StompHeaders());
		if (connectHeaders.getHeartbeat() == null) {
			connectHeaders.setHeartbeat(getDefaultHeartbeat());
		}
		return connectHeaders;
	}

}
