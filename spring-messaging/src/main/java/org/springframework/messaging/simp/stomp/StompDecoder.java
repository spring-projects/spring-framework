/*
 * Copyright 2002-2020 the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.messaging.support.MessageHeaderInitializer;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MultiValueMap;

/**
 * Decodes one or more STOMP frames contained in a {@link ByteBuffer}.
 *
 * <p>An attempt is made to read all complete STOMP frames from the buffer, which
 * could be zero, one, or more. If there is any left-over content, i.e. an incomplete
 * STOMP frame, at the end the buffer is reset to point to the beginning of the
 * partial content. The caller is then responsible for dealing with that
 * incomplete content by buffering until there is more input available.
 *
 * @author Andy Wilkinson
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompDecoder {

	static final byte[] HEARTBEAT_PAYLOAD = new byte[] {'\n'};

	private static final Log logger = SimpLogging.forLogName(StompDecoder.class);

	@Nullable
	private MessageHeaderInitializer headerInitializer;


	/**
	 * Configure a {@link MessageHeaderInitializer} to apply to the headers of
	 * {@link Message Messages} from decoded STOMP frames.
	 */
	public void setHeaderInitializer(@Nullable MessageHeaderInitializer headerInitializer) {
		this.headerInitializer = headerInitializer;
	}

	/**
	 * Return the configured {@code MessageHeaderInitializer}, if any.
	 */
	@Nullable
	public MessageHeaderInitializer getHeaderInitializer() {
		return this.headerInitializer;
	}


	/**
	 * Decodes one or more STOMP frames from the given {@code ByteBuffer} into a
	 * list of {@link Message Messages}. If the input buffer contains partial STOMP frame
	 * content, or additional content with a partial STOMP frame, the buffer is
	 * reset and {@code null} is returned.
	 * @param byteBuffer the buffer to decode the STOMP frame from
	 * @return the decoded messages, or an empty list if none
	 * @throws StompConversionException raised in case of decoding issues
	 */
	public List<Message<byte[]>> decode(ByteBuffer byteBuffer) {
		return decode(byteBuffer, null);
	}

	/**
	 * Decodes one or more STOMP frames from the given {@code buffer} and returns
	 * a list of {@link Message Messages}.
	 * <p>If the given ByteBuffer contains only partial STOMP frame content and no
	 * complete STOMP frames, an empty list is returned, and the buffer is reset to
	 * to where it was.
	 * <p>If the buffer contains one ore more STOMP frames, those are returned and
	 * the buffer reset to point to the beginning of the unused partial content.
	 * <p>The output partialMessageHeaders map is used to store successfully parsed
	 * headers in case of partial content. The caller can then check if a
	 * "content-length" header was read, which helps to determine how much more
	 * content is needed before the next attempt to decode.
	 * @param byteBuffer the buffer to decode the STOMP frame from
	 * @param partialMessageHeaders an empty output map that will store the last
	 * successfully parsed partialMessageHeaders in case of partial message content
	 * in cases where the partial buffer ended with a partial STOMP frame
	 * @return the decoded messages, or an empty list if none
	 * @throws StompConversionException raised in case of decoding issues
	 */
	public List<Message<byte[]>> decode(ByteBuffer byteBuffer,
			@Nullable MultiValueMap<String, String> partialMessageHeaders) {

		List<Message<byte[]>> messages = new ArrayList<>();
		while (byteBuffer.hasRemaining()) {
			Message<byte[]> message = decodeMessage(byteBuffer, partialMessageHeaders);
			if (message != null) {
				messages.add(message);
				skipEol(byteBuffer);
				if (!byteBuffer.hasRemaining()) {
					break;
				}
			}
			else {
				break;
			}
		}
		return messages;
	}

	/**
	 * Decode a single STOMP frame from the given {@code buffer} into a {@link Message}.
	 */
	@Nullable
	private Message<byte[]> decodeMessage(ByteBuffer byteBuffer, @Nullable MultiValueMap<String, String> headers) {
		Message<byte[]> decodedMessage = null;
		skipEol(byteBuffer);

		// Explicit mark/reset access via Buffer base type for compatibility
		// with covariant return type on JDK 9's ByteBuffer...
		Buffer buffer = byteBuffer;
		buffer.mark();

		String command = readCommand(byteBuffer);
		if (command.length() > 0) {
			StompHeaderAccessor headerAccessor = null;
			byte[] payload = null;
			if (byteBuffer.remaining() > 0) {
				StompCommand stompCommand = StompCommand.valueOf(command);
				headerAccessor = StompHeaderAccessor.create(stompCommand);
				initHeaders(headerAccessor);
				readHeaders(byteBuffer, headerAccessor);
				payload = readPayload(byteBuffer, headerAccessor);
			}
			if (payload != null) {
				if (payload.length > 0) {
					StompCommand stompCommand = headerAccessor.getCommand();
					if (stompCommand != null && !stompCommand.isBodyAllowed()) {
						throw new StompConversionException(stompCommand +
								" shouldn't have a payload: length=" + payload.length + ", headers=" + headers);
					}
				}
				headerAccessor.updateSimpMessageHeadersFromStompHeaders();
				headerAccessor.setLeaveMutable(true);
				decodedMessage = MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());
				if (logger.isTraceEnabled()) {
					logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(payload));
				}
			}
			else {
				logger.trace("Incomplete frame, resetting input buffer...");
				if (headers != null && headerAccessor != null) {
					String name = NativeMessageHeaderAccessor.NATIVE_HEADERS;
					@SuppressWarnings("unchecked")
					MultiValueMap<String, String> map = (MultiValueMap<String, String>) headerAccessor.getHeader(name);
					if (map != null) {
						headers.putAll(map);
					}
				}
				buffer.reset();
			}
		}
		else {
			StompHeaderAccessor headerAccessor = StompHeaderAccessor.createForHeartbeat();
			initHeaders(headerAccessor);
			headerAccessor.setLeaveMutable(true);
			decodedMessage = MessageBuilder.createMessage(HEARTBEAT_PAYLOAD, headerAccessor.getMessageHeaders());
			if (logger.isTraceEnabled()) {
				logger.trace("Decoded " + headerAccessor.getDetailedLogMessage(null));
			}
		}

		return decodedMessage;
	}

	private void initHeaders(StompHeaderAccessor headerAccessor) {
		MessageHeaderInitializer initializer = getHeaderInitializer();
		if (initializer != null) {
			initializer.initHeaders(headerAccessor);
		}
	}

	/**
	 * Skip one ore more EOL characters at the start of the given ByteBuffer.
	 * STOMP, section 2.1 says: "The NULL octet can be optionally followed by
	 * multiple EOLs."
	 */
	protected void skipEol(ByteBuffer byteBuffer) {
		while (true) {
			if (!tryConsumeEndOfLine(byteBuffer)) {
				break;
			}
		}
	}

	private String readCommand(ByteBuffer byteBuffer) {
		ByteArrayOutputStream command = new ByteArrayOutputStream(256);
		while (byteBuffer.remaining() > 0 && !tryConsumeEndOfLine(byteBuffer)) {
			command.write(byteBuffer.get());
		}
		return new String(command.toByteArray(), StandardCharsets.UTF_8);
	}

	private void readHeaders(ByteBuffer byteBuffer, StompHeaderAccessor headerAccessor) {
		while (true) {
			ByteArrayOutputStream headerStream = new ByteArrayOutputStream(256);
			boolean headerComplete = false;
			while (byteBuffer.hasRemaining()) {
				if (tryConsumeEndOfLine(byteBuffer)) {
					headerComplete = true;
					break;
				}
				headerStream.write(byteBuffer.get());
			}
			if (headerStream.size() > 0 && headerComplete) {
				String header = new String(headerStream.toByteArray(), StandardCharsets.UTF_8);
				int colonIndex = header.indexOf(':');
				if (colonIndex <= 0) {
					if (byteBuffer.remaining() > 0) {
						throw new StompConversionException("Illegal header: '" + header +
								"'. A header must be of the form <name>:[<value>].");
					}
				}
				else {
					String headerName = unescape(header.substring(0, colonIndex));
					String headerValue = unescape(header.substring(colonIndex + 1));
					try {
						headerAccessor.addNativeHeader(headerName, headerValue);
					}
					catch (InvalidMimeTypeException ex) {
						if (byteBuffer.remaining() > 0) {
							throw ex;
						}
					}
				}
			}
			else {
				break;
			}
		}
	}

	/**
	 * See STOMP Spec 1.2:
	 * <a href="https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
	 */
	private String unescape(String inString) {
		StringBuilder sb = new StringBuilder(inString.length());
		int pos = 0;  // position in the old string
		int index = inString.indexOf('\\');

		while (index >= 0) {
			sb.append(inString.substring(pos, index));
			if (index + 1 >= inString.length()) {
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			char c = inString.charAt(index + 1);
			if (c == 'r') {
				sb.append('\r');
			}
			else if (c == 'n') {
				sb.append('\n');
			}
			else if (c == 'c') {
				sb.append(':');
			}
			else if (c == '\\') {
				sb.append('\\');
			}
			else {
				// should never happen
				throw new StompConversionException("Illegal escape sequence at index " + index + ": " + inString);
			}
			pos = index + 2;
			index = inString.indexOf('\\', pos);
		}

		sb.append(inString.substring(pos));
		return sb.toString();
	}

	@Nullable
	private byte[] readPayload(ByteBuffer byteBuffer, StompHeaderAccessor headerAccessor) {
		Integer contentLength;
		try {
			contentLength = headerAccessor.getContentLength();
		}
		catch (NumberFormatException ex) {
			if (logger.isDebugEnabled()) {
				logger.debug("Ignoring invalid content-length: '" + headerAccessor);
			}
			contentLength = null;
		}

		if (contentLength != null && contentLength >= 0) {
			if (byteBuffer.remaining() > contentLength) {
				byte[] payload = new byte[contentLength];
				byteBuffer.get(payload);
				if (byteBuffer.get() != 0) {
					throw new StompConversionException("Frame must be terminated with a null octet");
				}
				return payload;
			}
			else {
				return null;
			}
		}
		else {
			ByteArrayOutputStream payload = new ByteArrayOutputStream(256);
			while (byteBuffer.remaining() > 0) {
				byte b = byteBuffer.get();
				if (b == 0) {
					return payload.toByteArray();
				}
				else {
					payload.write(b);
				}
			}
		}
		return null;
	}

	/**
	 * Try to read an EOL incrementing the buffer position if successful.
	 * @return whether an EOL was consumed
	 */
	private boolean tryConsumeEndOfLine(ByteBuffer byteBuffer) {
		if (byteBuffer.remaining() > 0) {
			byte b = byteBuffer.get();
			if (b == '\n') {
				return true;
			}
			else if (b == '\r') {
				if (byteBuffer.remaining() > 0 && byteBuffer.get() == '\n') {
					return true;
				}
				else {
					throw new StompConversionException("'\\r' must be followed by '\\n'");
				}
			}
			// Explicit cast for compatibility with covariant return type on JDK 9's ByteBuffer
			((Buffer) byteBuffer).position(byteBuffer.position() - 1);
		}
		return false;
	}

}
