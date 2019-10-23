/*
 * Copyright 2002-2019 the original author or authors.
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
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;

import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpLogging;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.support.NativeMessageHeaderAccessor;
import org.springframework.util.Assert;

/**
 * An encoder for STOMP frames.
 *
 * @author Andy Wilkinson
 * @author Rossen Stoyanchev
 * @since 4.0
 * @see StompDecoder
 */
public class StompEncoder  {

	private static final byte LF = '\n';

	private static final byte COLON = ':';

	private static final Log logger = SimpLogging.forLogName(StompEncoder.class);

	private static final int HEADER_KEY_CACHE_LIMIT = 32;


	private final Map<String, byte[]> headerKeyAccessCache = new ConcurrentHashMap<>(HEADER_KEY_CACHE_LIMIT);

	@SuppressWarnings("serial")
	private final Map<String, byte[]> headerKeyUpdateCache =
			new LinkedHashMap<String, byte[]>(HEADER_KEY_CACHE_LIMIT, 0.75f, true) {
				@Override
				protected boolean removeEldestEntry(Map.Entry<String, byte[]> eldest) {
					if (size() > HEADER_KEY_CACHE_LIMIT) {
						headerKeyAccessCache.remove(eldest.getKey());
						return true;
					}
					else {
						return false;
					}
				}
			};


	/**
	 * Encodes the given STOMP {@code message} into a {@code byte[]}.
	 * @param message the message to encode
	 * @return the encoded message
	 */
	public byte[] encode(Message<byte[]> message) {
		return encode(message.getHeaders(), message.getPayload());
	}

	/**
	 * Encodes the given payload and headers into a {@code byte[]}.
	 * @param headers the headers
	 * @param payload the payload
	 * @return the encoded message
	 */
	public byte[] encode(Map<String, Object> headers, byte[] payload) {
		Assert.notNull(headers, "'headers' is required");
		Assert.notNull(payload, "'payload' is required");

		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(128 + payload.length);
			DataOutputStream output = new DataOutputStream(baos);

			if (SimpMessageType.HEARTBEAT.equals(SimpMessageHeaderAccessor.getMessageType(headers))) {
				logger.trace("Encoding heartbeat");
				output.write(StompDecoder.HEARTBEAT_PAYLOAD);
			}

			else {
				StompCommand command = StompHeaderAccessor.getCommand(headers);
				if (command == null) {
					throw new IllegalStateException("Missing STOMP command: " + headers);
				}

				output.write(command.toString().getBytes(StandardCharsets.UTF_8));
				output.write(LF);
				writeHeaders(command, headers, payload, output);
				output.write(LF);
				writeBody(payload, output);
				output.write((byte) 0);
			}

			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new StompConversionException("Failed to encode STOMP frame, headers=" + headers,  ex);
		}
	}

	private void writeHeaders(StompCommand command, Map<String, Object> headers, byte[] payload,
			DataOutputStream output) throws IOException {

		@SuppressWarnings("unchecked")
		Map<String,List<String>> nativeHeaders =
				(Map<String, List<String>>) headers.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		if (logger.isTraceEnabled()) {
			logger.trace("Encoding STOMP " + command + ", headers=" + nativeHeaders);
		}

		if (nativeHeaders == null) {
			return;
		}

		boolean shouldEscape = (command != StompCommand.CONNECT && command != StompCommand.STOMP
				&& command != StompCommand.CONNECTED);

		for (Entry<String, List<String>> entry : nativeHeaders.entrySet()) {
			if (command.requiresContentLength() && "content-length".equals(entry.getKey())) {
				continue;
			}

			List<String> values = entry.getValue();
			if ((StompCommand.CONNECT.equals(command) || StompCommand.STOMP.equals(command)) &&
					StompHeaderAccessor.STOMP_PASSCODE_HEADER.equals(entry.getKey())) {
				values = Collections.singletonList(StompHeaderAccessor.getPasscode(headers));
			}

			byte[] encodedKey = encodeHeaderKey(entry.getKey(), shouldEscape);
			for (String value : values) {
				output.write(encodedKey);
				output.write(COLON);
				output.write(encodeHeaderValue(value, shouldEscape));
				output.write(LF);
			}
		}

		if (command.requiresContentLength()) {
			int contentLength = payload.length;
			output.write("content-length:".getBytes(StandardCharsets.UTF_8));
			output.write(Integer.toString(contentLength).getBytes(StandardCharsets.UTF_8));
			output.write(LF);
		}
	}

	private byte[] encodeHeaderKey(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		if (this.headerKeyAccessCache.containsKey(inputToUse)) {
			return this.headerKeyAccessCache.get(inputToUse);
		}
		synchronized (this.headerKeyUpdateCache) {
			byte[] bytes = this.headerKeyUpdateCache.get(inputToUse);
			if (bytes == null) {
				bytes = inputToUse.getBytes(StandardCharsets.UTF_8);
				this.headerKeyAccessCache.put(inputToUse, bytes);
				this.headerKeyUpdateCache.put(inputToUse, bytes);
			}
			return bytes;
		}
	}

	private byte[] encodeHeaderValue(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		return inputToUse.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * See STOMP Spec 1.2:
	 * <a href="https://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
	 */
	private String escape(String inString) {
		StringBuilder sb = null;
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (c == '\\') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\\\");
			}
			else if (c == ':') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\c");
			}
			else if (c == '\n') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\n");
			}
			else if (c == '\r') {
				sb = getStringBuilder(sb, inString, i);
				sb.append("\\r");
			}
			else if (sb != null){
				sb.append(c);
			}
		}
		return (sb != null ? sb.toString() : inString);
	}

	private StringBuilder getStringBuilder(@Nullable StringBuilder sb, String inString, int i) {
		if (sb == null) {
			sb = new StringBuilder(inString.length());
			sb.append(inString.substring(0, i));
		}
		return sb;
	}

	private void writeBody(byte[] payload, DataOutputStream output) throws IOException {
		output.write(payload);
	}

}
