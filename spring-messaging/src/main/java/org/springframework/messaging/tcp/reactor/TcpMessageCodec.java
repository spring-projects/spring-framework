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

package org.springframework.messaging.tcp.reactor;

import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.messaging.Message;

/**
 * Contract to encode and decode a {@link Message} to and from a {@link ByteBuffer}
 * allowing a higher-level protocol (e.g. STOMP over TCP) to plug in.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 * @param <P> the message payload type
 */
public interface TcpMessageCodec<P> {

	/**
	 * Decode the input {@link ByteBuffer} into one or more {@link Message Messages}.
	 * @param buffer the input buffer to decode from
	 * @return 0 or more decoded messages
	 */
	List<Message<P>> decode(ByteBuffer buffer);

	/**
	 * Encode the given {@link Message} to the output {@link ByteBuffer}.
	 * @param message the message to encode
	 * @return the encoded buffer
	 */
	ByteBuffer encode(Message<P> message);

}
