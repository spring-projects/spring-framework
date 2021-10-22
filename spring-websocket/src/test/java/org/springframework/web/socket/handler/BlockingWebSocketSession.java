/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.socket.handler;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.web.socket.WebSocketMessage;

/**
 * Blocks indefinitely on sending a message but provides a latch to notify when
 * the message has been "sent" (i.e. session is blocked).
 *
 * @author Rossen Stoyanchev
 */
public class BlockingWebSocketSession extends TestWebSocketSession {

	private final AtomicReference<CountDownLatch> sendLatch = new AtomicReference<>();

	private final AtomicReference<CountDownLatch> releaseLatch = new AtomicReference<>();


	public CountDownLatch initSendLatch() {
		this.sendLatch.set(new CountDownLatch(1));
		return this.sendLatch.get();
	}

	@Override
	public void sendMessage(WebSocketMessage<?> message) throws IOException {
		super.sendMessage(message);
		if (this.sendLatch.get() != null) {
			this.sendLatch.get().countDown();
		}
		block();
	}

	private void block() {
		try {
			this.releaseLatch.set(new CountDownLatch(1));
			this.releaseLatch.get().await();
		}
		catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

}
