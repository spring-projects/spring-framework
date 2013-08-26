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

import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.server.support.WebSocketHttpRequestHandler;
import org.springframework.web.socket.sockjs.SockJsHttpRequestHandler;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link WebSocketConfigurationSupport}.
 *
 * @author Rossen Stoyanchev
 */
public class WebSocketConfigurationTests {

	private DelegatingWebSocketConfiguration config;

	private GenericWebApplicationContext context;


	@Before
	public void setup() {
		this.config = new DelegatingWebSocketConfiguration();
		this.context = new GenericWebApplicationContext();
		this.context.refresh();
	}

	@Test
	public void webSocket() throws Exception {

		final WebSocketHandler handler = new TextWebSocketHandlerAdapter();

		WebSocketConfigurer configurer = new WebSocketConfigurer() {
			@Override
			public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
				registry.addHandler(handler, "/h1");
			}
		};

		this.config.setConfigurers(Arrays.asList(configurer));
		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.config.webSocketHandlerMapping();
		hm.setApplicationContext(this.context);

		Object actual = hm.getUrlMap().get("/h1");

		assertNotNull(actual);
		assertEquals(WebSocketHttpRequestHandler.class, actual.getClass());
		assertEquals(1, hm.getUrlMap().size());
	}

	@Test
	public void webSocketWithSockJS() throws Exception {

		final WebSocketHandler handler = new TextWebSocketHandlerAdapter();

		WebSocketConfigurer configurer = new WebSocketConfigurer() {
			@Override
			public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
				registry.addHandler(handler, "/h1").withSockJS();
			}
		};

		this.config.setConfigurers(Arrays.asList(configurer));
		SimpleUrlHandlerMapping hm = (SimpleUrlHandlerMapping) this.config.webSocketHandlerMapping();
		hm.setApplicationContext(this.context);

		Object actual = hm.getUrlMap().get("/h1/**");

		assertNotNull(actual);
		assertEquals(SockJsHttpRequestHandler.class, actual.getClass());
		assertEquals(1, hm.getUrlMap().size());
	}

}
