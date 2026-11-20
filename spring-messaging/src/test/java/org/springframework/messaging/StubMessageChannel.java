/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging;

import java.util.ArrayList;
import java.util.List;

/**
 * A stub MessageChannel that saves all sent messages.
 *
 * @author Rossen Stoyanchev
 */
public class StubMessageChannel implements SubscribableChannel {

	private final List<Message<byte[]>> messages = new ArrayList<>();

	private final List<MessageHandler> handlers = new ArrayList<>();


	public List<Message<byte[]>> getMessages() {
		return this.messages;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean send(Message<?> message) {
		this.messages.add((Message<byte[]>) message);
		return true;
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean send(Message<?> message, long timeout) {
		this.messages.add((Message<byte[]>) message);
		return true;
	}

	@Override
	public boolean subscribe(MessageHandler handler) {
		this.handlers.add(handler);
		return true;
	}

	@Override
	public boolean unsubscribe(MessageHandler handler) {
		this.handlers.remove(handler);
		return true;
	}
}
