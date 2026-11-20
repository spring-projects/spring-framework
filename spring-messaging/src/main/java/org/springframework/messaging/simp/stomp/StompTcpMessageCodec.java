/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
