/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.messaging.simp.stomp;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@@link StompClientSupport}.
 * @author Rossen Stoyanchev
 */
public class StompClientSupportTests {

	private StompClientSupport stompClient;


	@Before
	public void setUp() throws Exception {
		this.stompClient = new StompClientSupport() {};
	}

	@Test
	public void defaultHearbeatValidation() throws Exception {
		trySetDefaultHeartbeat(null);
		trySetDefaultHeartbeat(new long[] {-1, 0});
		trySetDefaultHeartbeat(new long[] {0, -1});
	}

	private void trySetDefaultHeartbeat(long[] heartbeat) {
		try {
			this.stompClient.setDefaultHeartbeat(heartbeat);
			fail("Expected exception");
		}
		catch (IllegalArgumentException ex) {
			// Ignore
		}
	}

	@Test
	public void defaultHeartbeatValue() throws Exception {
		assertArrayEquals(new long[] {10000, 10000}, this.stompClient.getDefaultHeartbeat());
	}

	@Test
	public void isDefaultHeartbeatEnabled() throws Exception {
		assertArrayEquals(new long[] {10000, 10000}, this.stompClient.getDefaultHeartbeat());
		assertTrue(this.stompClient.isDefaultHeartbeatEnabled());

		this.stompClient.setDefaultHeartbeat(new long[] {0, 0});
		assertFalse(this.stompClient.isDefaultHeartbeatEnabled());
	}

	@Test
	public void processConnectHeadersDefault() throws Exception {
		StompHeaders connectHeaders = this.stompClient.processConnectHeaders(null);

		assertNotNull(connectHeaders);
		assertArrayEquals(new long[] {10000, 10000}, connectHeaders.getHeartbeat());
	}

	@Test
	public void processConnectHeadersWithExplicitHeartbeat() throws Exception {

		StompHeaders connectHeaders = new StompHeaders();
		connectHeaders.setHeartbeat(new long[] {15000, 15000});
		connectHeaders = this.stompClient.processConnectHeaders(connectHeaders);

		assertNotNull(connectHeaders);
		assertArrayEquals(new long[] {15000, 15000}, connectHeaders.getHeartbeat());
	}

}
