/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.messaging;

import java.util.Map;


/**
 * A factory for creating messages, allowing for control of the concrete type of the message.
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public interface MessageFactory<M extends Message<?>> {

	/**
	 * Creates a new message with the given payload and headers
	 *
	 * @param payload The message payload
	 * @param headers The message headers
	 * @param <P> The payload's type
	 *
	 * @return the message
	 */
	<P> M createMessage(P payload, Map<String, Object> headers);
}
