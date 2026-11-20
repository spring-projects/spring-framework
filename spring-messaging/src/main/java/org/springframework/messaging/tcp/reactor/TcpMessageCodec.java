/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp.reactor;

import java.nio.ByteBuffer;
import java.util.List;

import org.springframework.messaging.Message;

/**
 * Contract to encode and decode a {@link Message} to and from a {@link ByteBuffer}
 * allowing a higher-level protocol (for example, STOMP over TCP) to plug in.
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
