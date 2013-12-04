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

package org.springframework.messaging.core;

import org.springframework.messaging.Message;

/**
 * A contract for processing a {@link Message} after it has been created, either
 * returning a modified (effectively new) message or returning the same.
 *
 * @author Mark Fisher
 * @author Rossen Stoyanchev
 * @since 4.0
 *
 * @see MessageSendingOperations
 * @see MessageRequestReplyOperations
 */
public interface MessagePostProcessor {

	/**
	 * Process the given message.
	 *
	 * @param message the message to process
	 * @return a new or the same message, never {@code null}
	 */
	Message<?> postProcessMessage(Message<?> message);

}
