/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.web.socket.sockjs.transport;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.sockjs.SockJsService;
import org.springframework.web.socket.sockjs.frame.SockJsMessageCodec;

/**
 * Provides transport handling code with access to the {@link SockJsService} configuration
 * options they need to have access to. Mainly for internal use.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface SockJsServiceConfig {

	/**
	 * A scheduler instance to use for scheduling heart-beat messages.
	 */
	TaskScheduler getTaskScheduler();

	/**
	 * Streaming transports save responses on the client side and don't free
	 * memory used by delivered messages. Such transports need to recycle the
	 * connection once in a while. This property sets a minimum number of bytes
	 * that can be send over a single HTTP streaming request before it will be
	 * closed. After that client will open a new request. Setting this value to
	 * one effectively disables streaming and will make streaming transports to
	 * behave like polling transports.
	 * <p>The default value is 128K (i.e. 128 * 1024).
	 */
	int getStreamBytesLimit();

	/**
	 * The amount of time in milliseconds when the server has not sent any
	 * messages and after which the server should send a heartbeat frame to the
	 * client in order to keep the connection from breaking.
	 * <p>The default value is 25,000 (25 seconds).
	 */
	long getHeartbeatTime();

	/**
	 * The number of server-to-client messages that a session can cache while waiting for
	 * the next HTTP polling request from the client. All HTTP transports use this
	 * property since even streaming transports recycle HTTP requests periodically.
	 * <p>The amount of time between HTTP requests should be relatively brief and will not
	 * exceed the allows disconnect delay (see
	 * {@link org.springframework.web.socket.sockjs.support.AbstractSockJsService#setDisconnectDelay(long)}),
	 * 5 seconds by default.
	 * <p>The default size is 100.
	 */
	int getHttpMessageCacheSize();

	/**
	 * The codec to use for encoding and decoding SockJS messages.
	 * @throws IllegalStateException if no {@link SockJsMessageCodec} is available
	 */
	SockJsMessageCodec getMessageCodec();

}
