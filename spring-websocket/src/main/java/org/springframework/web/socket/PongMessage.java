/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.web.socket;

import java.nio.ByteBuffer;

/**
 * A WebSocket pong message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class PongMessage extends AbstractWebSocketMessage<ByteBuffer> {


	/**
	 * Create a new pong message with an empty payload.
	 */
	public PongMessage() {
		super(ByteBuffer.allocate(0));
	}

	/**
	 * Create a new pong message with the given ByteBuffer payload.
	 *
	 * @param payload the non-null payload
	 */
	public PongMessage(ByteBuffer payload) {
		super(payload);
	}


	@Override
	protected int getPayloadSize() {
		return (getPayload() != null) ? getPayload().remaining() : 0;
	}

	@Override
	protected String toStringPayload() {
		return (getPayload() != null) ? getPayload().toString() : null;
	}

}
