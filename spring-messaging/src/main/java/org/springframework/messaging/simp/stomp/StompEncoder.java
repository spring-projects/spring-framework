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

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedList;
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

	private static final Byte LINE_FEED_BYTE = '\n';

	private static final Byte COLON_BYTE = ':';

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

		if (SimpMessageType.HEARTBEAT.equals(SimpMessageHeaderAccessor.getMessageType(headers))) {
			logger.trace("Encoding heartbeat");
			return StompDecoder.HEARTBEAT_PAYLOAD;
		}

		StompCommand command = StompHeaderAccessor.getCommand(headers);
		if (command == null) {
			throw new IllegalStateException("Missing STOMP command: " + headers);
		}

		Result result = new DefaultResult();
		result.add(command.toString().getBytes(StandardCharsets.UTF_8));
		result.add(LINE_FEED_BYTE);
		writeHeaders(command, headers, payload, result);
		result.add(LINE_FEED_BYTE);
		result.add(payload);
		result.add((byte) 0);
		return result.toByteArray();
	}

	private void writeHeaders(
			StompCommand command, Map<String, Object> headers, byte[] payload, Result result) {

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
				result.add(encodedKey);
				result.add(COLON_BYTE);
				result.add(encodeHeaderValue(value, shouldEscape));
				result.add(LINE_FEED_BYTE);
			}
		}

		if (command.requiresContentLength()) {
			int contentLength = payload.length;
			result.add("content-length:".getBytes(StandardCharsets.UTF_8));
			result.add(Integer.toString(contentLength).getBytes(StandardCharsets.UTF_8));
			result.add(LINE_FEED_BYTE);
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
			sb.append(inString, 0, i);
		}
		return sb;
	}


	/**
	 * Accumulates byte content and returns an aggregated byte[] at the end.
	 */
	private interface Result {

		void add(byte[] bytes);

		void add(byte b);

		byte[] toByteArray();

	}

	@SuppressWarnings("serial")
	private static class DefaultResult extends LinkedList<Object> implements Result {

		private int size;


		public void add(byte[] bytes) {
			this.size += bytes.length;
			super.add(bytes);
		}

		public void add(byte b) {
			this.size++;
			super.add(b);
		}

		public byte[] toByteArray() {
			byte[] result = new byte[this.size];
			int position = 0;
			for (Object o : this) {
				if (o instanceof byte[]) {
					byte[] src = (byte[]) o;
					System.arraycopy(src, 0, result, position, src.length);
					position += src.length;
				}
				else {
					result[position++] = (Byte) o;
				}
			}
			return result;
		}
	}

}
