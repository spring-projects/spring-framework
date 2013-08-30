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

package org.springframework.messaging.simp.stomp;

import org.springframework.messaging.Message;

import reactor.function.Consumer;
import reactor.function.Function;
import reactor.io.Buffer;
import reactor.tcp.encoding.Codec;

/**
 * A Reactor TCP {@link Codec} for sending and receiving STOMP messages
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public class StompCodec implements Codec<Buffer, Message<byte[]>, Message<byte[]>> {

	private static final StompDecoder DECODER = new StompDecoder();

	private static final Function<Message<byte[]>, Buffer> ENCODER_FUNCTION = new Function<Message<byte[]>, Buffer>() {

		private final StompEncoder encoder = new StompEncoder();

		@Override
		public Buffer apply(Message<byte[]> message) {
			return Buffer.wrap(this.encoder.encode(message));
		}
	};

	@Override
	public Function<Buffer, Message<byte[]>> decoder(final Consumer<Message<byte[]>> next) {
		return new Function<Buffer, Message<byte[]>>() {

			@Override
			public Message<byte[]> apply(Buffer buffer) {
				while (buffer.remaining() > 0) {
					Message<byte[]> message = DECODER.decode(buffer.byteBuffer());
					if (message != null) {
						next.accept(message);
					}
				}
				return null;
			}
		};
	}

	@Override
	public Function<Message<byte[]>, Buffer> encoder() {
		return ENCODER_FUNCTION;
	}

}
