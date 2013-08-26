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

package org.springframework.web.socket.server.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;


/**
 * A variation of {@link WebSocketConfigurationSupport} that detects implementations of
 * {@link WebSocketConfigurer} in Spring configuration and invokes them in order to
 * configure WebSocket request handling.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@Configuration
public class DelegatingWebSocketConfiguration extends WebSocketConfigurationSupport {

	private final List<WebSocketConfigurer> configurers = new ArrayList<WebSocketConfigurer>();


	@Autowired(required = false)
	public void setConfigurers(List<WebSocketConfigurer> configurers) {
		if (CollectionUtils.isEmpty(configurers)) {
			return;
		}
		this.configurers.addAll(configurers);
	}


	@Override
	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		for (WebSocketConfigurer configurer : this.configurers) {
			configurer.registerWebSocketHandlers(registry);
		}
	}

}
