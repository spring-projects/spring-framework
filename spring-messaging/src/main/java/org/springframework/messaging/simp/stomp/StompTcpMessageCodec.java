/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.messaging.simp.stomp;

import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.reactor.TcpMessageCodec;

/**
 * {@link TcpMessageCodec} for STOMP, delegating to {@link StompDecoder} and
 * {@link StompEncoder}.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
public class StompTcpMessageCodec implements TcpMessageCodec<byte[]> {

	private final StompDecoder decoder;

	private final StompEncoder encoder;


	public StompTcpMessageCodec() {
		this(new StompDecoder());
	}

	public StompTcpMessageCodec(StompDecoder decoder) {
		this(decoder, new StompEncoder());
	}

	public StompTcpMessageCodec(StompDecoder decoder, StompEncoder encoder) {
		this.decoder = decoder;
		this.encoder = encoder;
	}


	@Override
	public List<Message<byte[]>> decode(ByteBuffer nioBuffer) {
		return this.decoder.decode(nioBuffer);
	}

	@Override
	public ByteBuffer encode(Message<byte[]> message) {
		return ByteBuffer.wrap(this.encoder.encode(message));
	}

}
