/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
			new CountDownLatch(1).await();
		}
		catch (InterruptedException ex) {
			ex.printStackTrace();
		}
	}

}
