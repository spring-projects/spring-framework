/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.messaging;


/**
 * Base interface for any component that handles Messages.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @since 4.0
 */
public interface MessageHandler<M extends Message> {

	/**
	 * TODO: support exceptions?
	 *
	 * Handles the message if possible. If the handler cannot deal with the
	 * message this will result in a <code>MessageRejectedException</code> e.g.
	 * in case of a Selective Consumer. When a consumer tries to handle a
	 * message, but fails to do so, a <code>MessageHandlingException</code> is
	 * thrown. In the last case it is recommended to treat the message as tainted
	 * and go into an error scenario.
	 * <p>
	 * When the handling results in a failure of another message being sent
	 * (e.g. a "reply" message), that failure  will trigger a
	 * <code>MessageDeliveryException</code>.
	 *
	 * @param message the message to be handled
	 * @throws org.springframework.integration.MessageRejectedException if the handler doesn't accept the message
	 * @throws org.springframework.integration.MessageHandlingException when something fails during the handling
	 * @throws org.springframework.integration.MessageDeliveryException when this handler failed to deliver the
	 * reply related to the handling of the message
	 */
	void handleMessage(M message) throws MessagingException;

}
