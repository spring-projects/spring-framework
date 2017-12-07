/*
 * Copyright 2002-2017 the original author or authors.
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
 * A WebSocket ping message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class PingMessage extends AbstractWebSocketMessage<ByteBuffer> {

	/**
	 * Create a new ping message with an empty payload.
	 */
	public PingMessage() {
		super(ByteBuffer.allocate(0));
	}

	/**
	 * Create a new ping message with the given ByteBuffer payload.
	 * @param payload the non-null payload
	 */
	public PingMessage(ByteBuffer payload) {
		super(payload);
	}


	@Override
	public int getPayloadLength() {
		return getPayload().remaining();
	}

	@Override
	protected String toStringPayload() {
		return getPayload().toString();
	}

}
