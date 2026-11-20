/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.tcp.reactor;

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;

import io.netty.buffer.ByteBuf;

import org.springframework.messaging.Message;

/**
 * Convenient base class for {@link ReactorNettyCodec} implementations that need
 * to work with NIO {@link ByteBuffer ByteBuffers}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @param <P> the message payload type
 */
public abstract class AbstractNioBufferReactorNettyCodec<P> implements ReactorNettyCodec<P> {

	@Override
	public Collection<Message<P>> decode(ByteBuf inputBuffer) {
		ByteBuffer nioBuffer = inputBuffer.nioBuffer();
		int start = nioBuffer.position();
		List<Message<P>> messages = decodeInternal(nioBuffer);
		inputBuffer.skipBytes(nioBuffer.position() - start);
		return messages;
	}

	@Override
	public void encode(Message<P> message, ByteBuf outputBuffer) {
		outputBuffer.writeBytes(encodeInternal(message));
	}


	protected abstract List<Message<P>> decodeInternal(ByteBuffer nioBuffer);

	protected abstract ByteBuffer encodeInternal(Message<P> message);

}
