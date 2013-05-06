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

package org.springframework.web.socket.sockjs.support;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.springframework.web.socket.AbstractHttpRequestTests;
import org.springframework.web.socket.adapter.TextWebSocketHandlerAdapter;
import org.springframework.web.socket.sockjs.StubTaskScheduler;
import org.springframework.web.socket.sockjs.TransportHandler;
import org.springframework.web.socket.sockjs.TransportType;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link DefaultSockJsService}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSockJsServiceTests extends AbstractHttpRequestTests {

	private DefaultSockJsService service;


	@Before
	public void setUp() {
		super.setUp();
		this.service = new DefaultSockJsService(new StubTaskScheduler());
		this.service.setValidSockJsPrefixes("/echo");
	}

	@Test
	public void defaultTransportHandlers() {

		Map<TransportType, TransportHandler> handlers = service.getTransportHandlers();

		assertEquals(8, handlers.size());
		assertNotNull(handlers.get(TransportType.WEBSOCKET));
		assertNotNull(handlers.get(TransportType.XHR));
		assertNotNull(handlers.get(TransportType.XHR_SEND));
		assertNotNull(handlers.get(TransportType.XHR_STREAMING));
		assertNotNull(handlers.get(TransportType.JSONP));
		assertNotNull(handlers.get(TransportType.JSONP_SEND));
		assertNotNull(handlers.get(TransportType.HTML_FILE));
		assertNotNull(handlers.get(TransportType.EVENT_SOURCE));
	}

	@Test
	public void xhrSend() throws Exception {

		setRequest("POST", "/echo/000/c5839f69/xhr");
		this.service.handleRequest(this.request, this.response, new TextWebSocketHandlerAdapter());

		resetResponse();
		setRequest("POST", "/echo/000/c5839f69/xhr_send");
		this.servletRequest.setContent("[\"x\"]".getBytes("UTF-8"));

		this.service.handleRequest(this.request, this.response, new TextWebSocketHandlerAdapter());

		assertEquals(204, this.servletResponse.getStatus());
		assertEquals("text/plain;charset=UTF-8", this.servletResponse.getContentType());
	}


}
