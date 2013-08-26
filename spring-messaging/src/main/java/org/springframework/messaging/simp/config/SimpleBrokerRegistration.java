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

package org.springframework.messaging.simp.config;

import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.handler.SimpleBrokerMessageHandler;


/**
 * A simple message broker alternative providing a simple getting started option.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SimpleBrokerRegistration extends AbstractBrokerRegistration {


	public SimpleBrokerRegistration(MessageChannel webSocketReplyChannel, String[] destinationPrefixes) {
		super(webSocketReplyChannel, destinationPrefixes);
	}

	@Override
	protected SimpleBrokerMessageHandler getMessageHandler() {
		SimpleBrokerMessageHandler handler =
				new SimpleBrokerMessageHandler(getWebSocketReplyChannel(), getDestinationPrefixes());
		return handler;
	}

}
