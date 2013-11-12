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

package org.springframework.messaging;

import java.util.ArrayList;
import java.util.List;

/**
 * A stub MessageChannel that saves all sent messages.
 *
 * @author Rossen Stoyanchev
 */
public class StubMessageChannel implements MessageChannel {

	private final List<Message<byte[]>> messages = new ArrayList<>();


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

}
