/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.socket;

import java.nio.charset.StandardCharsets;

import org.jspecify.annotations.Nullable;

/**
 * A text WebSocket message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public final class TextMessage extends AbstractWebSocketMessage<String> {

	private final byte @Nullable [] bytes;


	/**
	 * Create a new text WebSocket message from the given CharSequence payload.
	 * @param payload the non-null payload
	 */
	public TextMessage(CharSequence payload) {
		super(payload.toString(), true);
		this.bytes = null;
	}

	/**
	 * Create a new text WebSocket message from the given byte[]. It is assumed
	 * the byte array can be encoded into an UTF-8 String.
	 * @param payload the non-null payload
	 */
	public TextMessage(byte[] payload) {
		super(new String(payload, StandardCharsets.UTF_8));
		this.bytes = payload;
	}

	/**
	 * Create a new text WebSocket message with the given payload representing the
	 * full or partial message content. When the {@code isLast} boolean flag is set
	 * to {@code false} the message is sent as partial content and more partial
	 * messages will be expected until the boolean flag is set to {@code true}.
	 * @param payload the non-null payload
	 * @param isLast whether this the last part of a series of partial messages
	 */
	public TextMessage(CharSequence payload, boolean isLast) {
		super(payload.toString(), isLast);
		this.bytes = null;
	}


	@Override
	public int getPayloadLength() {
		return asBytes().length;
	}

	public byte[] asBytes() {
		return (this.bytes != null ? this.bytes : getPayload().getBytes(StandardCharsets.UTF_8));
	}

	@Override
	protected String toStringPayload() {
		String payload = getPayload();
		if (payload.length() <= 10) {
			return payload;
		}
		int end = 10;
		if (Character.isSurrogatePair(payload.charAt(end - 1), payload.charAt(end))) {
			end--;
		}
		return payload.substring(0, end) + "..";
	}

}
