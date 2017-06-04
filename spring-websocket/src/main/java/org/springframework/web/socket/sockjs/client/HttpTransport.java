/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.socket.sockjs.client;

import java.net.URI;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.TextMessage;

/**
 * A SockJS {@link Transport} that uses HTTP requests to simulate a WebSocket
 * interaction. The {@code connect} method of the base {@code Transport} interface
 * is used to receive messages from the server while the
 * {@link #executeSendRequest} method here is used to send messages.
 *
 * @author Rossen Stoyanchev
 * @author Sebastian Lövdahl
 * @since 5.0
 */
public interface HttpTransport extends Transport, InfoReceiver {

	/**
	 * Execute a request to send the message to the server.
	 * <p>Note that as of 4.2 this method accepts a {@code headers} parameter.
	 * @param transportUrl the URL for sending messages.
	 * @param message the message to send
	 */
	void executeSendRequest(URI transportUrl, HttpHeaders headers, TextMessage message);

}
