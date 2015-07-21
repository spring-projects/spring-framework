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

package org.springframework.web.socket.messaging;

import org.springframework.messaging.Message;

/**
 * A contract for handling sub-protocol errors sent to clients.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface SubProtocolErrorHandler<P> {

	/**
	 * Handle errors thrown while processing client messages providing an
	 * opportunity to prepare the error message or to prevent one from being sent.
	 * <p>Note that the STOMP protocol requires a server to close the connection
	 * after sending an ERROR frame. To prevent an ERROR frame from being sent,
	 * a handler could return {@code null} and send a notification message
	 * through the broker instead, e.g. via a user destination.
	 * @param clientMessage the client message related to the error, possibly
	 * {@code null} if error occurred while parsing a WebSocket message
	 * @param ex the cause for the error, never {@code null}
	 * @return the error message to send to the client, or {@code null} in which
	 * case no message will be sent.
	 */
	Message<P> handleClientMessageProcessingError(Message<P> clientMessage, Throwable ex);

	/**
	 * Handle errors sent from the server side to clients, e.g. errors from the
	 * {@link org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler
	 * "broke relay"} because connectivity failed or the external broker sent an
	 * error message, etc.
	 * @param errorMessage the error message, never {@code null}
	 * @return the error message to send to the client, or {@code null} in which
	 * case no message will be sent.
	 */
	Message<P> handleErrorMessageToClient(Message<P> errorMessage);

}
