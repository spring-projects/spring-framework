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

package org.springframework.sockjs.server.support;

import java.util.Map;

import org.junit.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.sockjs.server.TransportHandler;
import org.springframework.sockjs.server.TransportType;

import static org.junit.Assert.*;


/**
 * Test fixture for {@link DefaultSockJsService}.
 *
 * @author Rossen Stoyanchev
 */
public class DefaultSockJsServiceTests {


	@Test
	public void testDefaultTransportHandlers() {

		DefaultSockJsService sockJsService = new DefaultSockJsService(new ThreadPoolTaskScheduler());
		Map<TransportType, TransportHandler> handlers = sockJsService.getTransportHandlers();

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


}
