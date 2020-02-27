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

package org.springframework.web.socket.config.annotation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.springframework.lang.Nullable;
import org.springframework.web.socket.handler.WebSocketHandlerDecoratorFactory;

/**
 * Configure the processing of messages received from and sent to WebSocket clients.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.3
 */
public class WebSocketTransportRegistration {

	@Nullable
	private Integer messageSizeLimit;

	@Nullable
	private Integer sendTimeLimit;

	@Nullable
	private Integer sendBufferSizeLimit;

	@Nullable
	private Integer timeToFirstMessage;

	private final List<WebSocketHandlerDecoratorFactory> decoratorFactories = new ArrayList<>(2);


	/**
	 * Configure the maximum size of an inbound sub-protocol message, such as
	 * a STOMP frame which may be aggregated from multiple WebSocket messages.
	 * <p>The default value is 64K (i.e. 64 * 1024).
	 * <p><strong>Note:</strong> This is not the same as the size of an
	 * individual WebSocket message which needs to be configured at the WebSocket
	 * server level instead. See the reference documentation for details.
	 */
	public WebSocketTransportRegistration setMessageSizeLimit(int messageSizeLimit) {
		this.messageSizeLimit = messageSizeLimit;
		return this;
	}

	/**
	 * Protected accessor for internal use.
	 */
	@Nullable
	protected Integer getMessageSizeLimit() {
		return this.messageSizeLimit;
	}

	/**
	 * Configure a time limit (in milliseconds) for the maximum amount of a time
	 * allowed when sending messages to a WebSocket session or writing to an
	 * HTTP response when SockJS fallback option are in use.
	 * <p>In general WebSocket servers expect that messages to a single WebSocket
	 * session are sent from a single thread at a time. This is automatically
	 * guaranteed when using {@code @EnableWebSocketMessageBroker} configuration.
	 * If message sending is slow, or at least slower than rate of messages sending,
	 * subsequent messages are buffered until either the {@code sendTimeLimit}
	 * or the {@code sendBufferSizeLimit} are reached at which point the session
	 * state is cleared and an attempt is made to close the session.
	 * <p><strong>NOTE</strong> that the session time limit is checked only
	 * on attempts to send additional messages. So if only a single message is
	 * sent and it hangs, the session will not time out until another message is
	 * sent or the underlying physical socket times out. So this is not a
	 * replacement for WebSocket server or HTTP connection timeout but is rather
	 * intended to control the extent of buffering of unsent messages.
	 * <p><strong>NOTE</strong> that closing the session may not succeed in
	 * actually closing the physical socket and may also hang. This is true
	 * especially when using blocking IO such as the BIO connector in Tomcat
	 * that is used by default on Tomcat 7. Therefore it is recommended to ensure
	 * the server is using non-blocking IO such as Tomcat's NIO connector that
	 * is used by default on Tomcat 8. If you must use blocking IO consider
	 * customizing OS-level TCP settings, for example
	 * {@code /proc/sys/net/ipv4/tcp_retries2} on Linux.
	 * <p>The default value is 10 seconds (i.e. 10 * 10000).
	 * @param timeLimit the timeout value in milliseconds; the value must be
	 * greater than 0, otherwise it is ignored.
	 */
	public WebSocketTransportRegistration setSendTimeLimit(int timeLimit) {
		this.sendTimeLimit = timeLimit;
		return this;
	}

	/**
	 * Protected accessor for internal use.
	 */
	@Nullable
	protected Integer getSendTimeLimit() {
		return this.sendTimeLimit;
	}

	/**
	 * Configure the maximum amount of data to buffer when sending messages
	 * to a WebSocket session, or an HTTP response when SockJS fallback
	 * option are in use.
	 * <p>In general WebSocket servers expect that messages to a single WebSocket
	 * session are sent from a single thread at a time. This is automatically
	 * guaranteed when using {@code @EnableWebSocketMessageBroker} configuration.
	 * If message sending is slow, or at least slower than rate of messages sending,
	 * subsequent messages are buffered until either the {@code sendTimeLimit}
	 * or the {@code sendBufferSizeLimit} are reached at which point the session
	 * state is cleared and an attempt is made to close the session.
	 * <p><strong>NOTE</strong> that closing the session may not succeed in
	 * actually closing the physical socket and may also hang. This is true
	 * especially when using blocking IO such as the BIO connector in Tomcat
	 * configured by default on Tomcat 7. Therefore it is recommended to ensure
	 * the server is using non-blocking IO such as Tomcat's NIO connector used
	 * by default on Tomcat 8. If you must use blocking IO consider customizing
	 * OS-level TCP settings, for example {@code /proc/sys/net/ipv4/tcp_retries2}
	 * on Linux.
	 * <p>The default value is 512K (i.e. 512 * 1024).
	 * @param sendBufferSizeLimit the maximum number of bytes to buffer when
	 * sending messages; if the value is less than or equal to 0 then buffering
	 * is effectively disabled.
	 */
	public WebSocketTransportRegistration setSendBufferSizeLimit(int sendBufferSizeLimit) {
		this.sendBufferSizeLimit = sendBufferSizeLimit;
		return this;
	}

	/**
	 * Protected accessor for internal use.
	 */
	@Nullable
	protected Integer getSendBufferSizeLimit() {
		return this.sendBufferSizeLimit;
	}

	/**
	 * Set the maximum time allowed in milliseconds after the WebSocket connection
	 * is established and before the first sub-protocol message is received.
	 * <p>This handler is for WebSocket connections that use a sub-protocol.
	 * Therefore, we expect the client to send at least one sub-protocol message
	 * in the beginning, or else we assume the connection isn't doing well, e.g.
	 * proxy issue, slow network, and can be closed.
	 * <p>By default this is set to {@code 60,000} (1 minute).
	 * @param timeToFirstMessage the maximum time allowed in milliseconds
	 * @since 5.1
	 */
	public WebSocketTransportRegistration setTimeToFirstMessage(int timeToFirstMessage) {
		this.timeToFirstMessage = timeToFirstMessage;
		return this;
	}

	/**
	 * Protected accessor for internal use.
	 */
	@Nullable
	protected Integer getTimeToFirstMessage() {
		return this.timeToFirstMessage;
	}

	/**
	 * Configure one or more factories to decorate the handler used to process
	 * WebSocket messages. This may be useful in some advanced use cases, for
	 * example to allow Spring Security to forcibly close the WebSocket session
	 * when the corresponding HTTP session expires.
	 * @since 4.1.2
	 */
	public WebSocketTransportRegistration setDecoratorFactories(WebSocketHandlerDecoratorFactory... factories) {
		this.decoratorFactories.addAll(Arrays.asList(factories));
		return this;
	}

	/**
	 * Add a factory that to decorate the handler used to process WebSocket
	 * messages. This may be useful for some advanced use cases, for example
	 * to allow Spring Security to forcibly close the WebSocket session when
	 * the corresponding HTTP session expires.
	 * @since 4.1.2
	 */
	public WebSocketTransportRegistration addDecoratorFactory(WebSocketHandlerDecoratorFactory factory) {
		this.decoratorFactories.add(factory);
		return this;
	}

	protected List<WebSocketHandlerDecoratorFactory> getDecoratorFactories() {
		return this.decoratorFactories;
	}

}
