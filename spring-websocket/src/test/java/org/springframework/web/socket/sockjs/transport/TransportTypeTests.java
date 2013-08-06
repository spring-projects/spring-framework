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

package org.springframework.web.socket.sockjs.transport;

import org.junit.Test;
import org.springframework.web.socket.sockjs.transport.TransportType;

import static org.junit.Assert.*;

/**
 * @author Rossen Stoyanchev
 */
public class TransportTypeTests {

	@Test
	public void testFromValue() {
		assertEquals(TransportType.WEBSOCKET, TransportType.fromValue("websocket"));
		assertEquals(TransportType.XHR, TransportType.fromValue("xhr"));
		assertEquals(TransportType.XHR_SEND, TransportType.fromValue("xhr_send"));
		assertEquals(TransportType.JSONP, TransportType.fromValue("jsonp"));
		assertEquals(TransportType.JSONP_SEND, TransportType.fromValue("jsonp_send"));
		assertEquals(TransportType.XHR_STREAMING, TransportType.fromValue("xhr_streaming"));
		assertEquals(TransportType.EVENT_SOURCE, TransportType.fromValue("eventsource"));
		assertEquals(TransportType.HTML_FILE, TransportType.fromValue("htmlfile"));
	}

}
