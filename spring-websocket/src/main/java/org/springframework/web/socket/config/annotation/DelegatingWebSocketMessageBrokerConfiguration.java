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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.util.CollectionUtils;

/**
 * A {@link WebSocketMessageBrokerConfigurationSupport} extension that detects beans of type
 * {@link WebSocketMessageBrokerConfigurer}
 * and delegates to all of them allowing callback style customization of the
 * configuration provided in {@link WebSocketMessageBrokerConfigurationSupport}.
 *
 * <p>This class is typically imported via {@link EnableWebSocketMessageBroker}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Configuration
public class DelegatingWebSocketMessageBrokerConfiguration extends WebSocketMessageBrokerConfigurationSupport {

	private List<WebSocketMessageBrokerConfigurer> configurers = new ArrayList<WebSocketMessageBrokerConfigurer>();


	@Autowired(required=false)
	public void setConfigurers(List<WebSocketMessageBrokerConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}


	@Override
	protected void registerStompEndpoints(StompEndpointRegistry registry) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.registerStompEndpoints(registry);
		}
	}

	@Override
	protected void configureClientInboundChannel(ChannelRegistration registration) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.configureClientInboundChannel(registration);
		}
	}

	@Override
	protected void configureClientOutboundChannel(ChannelRegistration registration) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.configureClientOutboundChannel(registration);
		}
	}

	@Override
	protected boolean configureMessageConverters(List<MessageConverter> messageConverters) {
		boolean registerDefaults = true;
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			if (!c.configureMessageConverters(messageConverters)) {
				registerDefaults = false;
			}
		}
		return registerDefaults;
	}

	@Override
	protected void configureMessageBroker(MessageBrokerRegistry registry) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.configureMessageBroker(registry);
		}
	}

}
