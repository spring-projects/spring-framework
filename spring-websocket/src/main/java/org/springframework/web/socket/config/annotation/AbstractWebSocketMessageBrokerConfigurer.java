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

package org.springframework.web.socket.config.annotation;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;

import java.util.List;

/**
 * A convenient abstract base class for {@link WebSocketMessageBrokerConfigurer}
 * implementations providing empty method implementations for optional methods.
 *
 * @author Rossen Stoyanchev
 * @since 4.0.1
 */
public abstract class AbstractWebSocketMessageBrokerConfigurer implements WebSocketMessageBrokerConfigurer {

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
	}

	@Override
	public boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		return true;
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
	}

}
