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

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;


/**
 * A {@link WebSocketMessageBrokerConfiguration} extension that detects beans of type
 * {@link WebSocketMessageBrokerConfigurer} and delegates to all of them allowing callback
 * style customization of the configuration provided in
 * {@link WebSocketMessageBrokerConfigurationSupport}.
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
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		this.configurers.addAll(configurers);
	}

	@Override
	protected void registerStompEndpoints(StompEndpointRegistry registry) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.registerStompEndpoints(registry);
		}
	}

	@Override
	protected void configureMessageBroker(MessageBrokerConfigurer configurer) {
		for (WebSocketMessageBrokerConfigurer c : this.configurers) {
			c.configureMessageBroker(configurer);
		}
	}

}
