/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket.config.annotation;

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
 * @author Sebastien Deleuze
 * @since 4.0
 */
@Configuration(proxyBeanMethods = false)
public class DelegatingWebSocketConfiguration extends WebSocketConfigurationSupport {

	private final List<WebSocketConfigurer> configurers = new ArrayList<>();


	@Autowired(required = false)
	public void setConfigurers(List<WebSocketConfigurer> configurers) {
		if (!CollectionUtils.isEmpty(configurers)) {
			this.configurers.addAll(configurers);
		}
	}


	@Override
	protected void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
		for (WebSocketConfigurer configurer : this.configurers) {
			configurer.registerWebSocketHandlers(registry);
		}
	}

}
