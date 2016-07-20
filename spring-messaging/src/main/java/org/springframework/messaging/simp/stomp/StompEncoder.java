/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
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
 */
public final class StompEncoder  {

	private static final byte LF = '\n';

	private static final byte COLON = ':';

	private final Log logger = LogFactory.getLog(StompEncoder.class);


	/**
	 * Encodes the given STOMP {@code message} into a {@code byte[]}
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
				if (logger.isTraceEnabled()) {
					logger.trace("Encoding heartbeat");
				}
				output.write(StompDecoder.HEARTBEAT_PAYLOAD);
			}
			else {
				StompCommand command = StompHeaderAccessor.getCommand(headers);
				Assert.notNull(command, "Missing STOMP command: " + headers);
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

	private void writeHeaders(StompCommand command, Map<String, Object> headers, byte[] payload, DataOutputStream output)
			throws IOException {

		@SuppressWarnings("unchecked")
		Map<String,List<String>> nativeHeaders =
				(Map<String, List<String>>) headers.get(NativeMessageHeaderAccessor.NATIVE_HEADERS);

		if (logger.isTraceEnabled()) {
			logger.trace("Encoding STOMP " + command + ", headers=" + nativeHeaders);
		}

		if (nativeHeaders == null) {
			return;
		}

		boolean shouldEscape = (command != StompCommand.CONNECT && command != StompCommand.CONNECTED);

		for (Entry<String, List<String>> entry : nativeHeaders.entrySet()) {
			byte[] key = encodeHeaderString(entry.getKey(), shouldEscape);
			if (command.requiresContentLength() && "content-length".equals(entry.getKey())) {
				continue;
			}
			List<String> values = entry.getValue();
			if (StompCommand.CONNECT.equals(command) &&
					StompHeaderAccessor.STOMP_PASSCODE_HEADER.equals(entry.getKey())) {
				values = Arrays.asList(StompHeaderAccessor.getPasscode(headers));
			}
			for (String value : values) {
				output.write(key);
				output.write(COLON);
				output.write(encodeHeaderString(value, shouldEscape));
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

	private byte[] encodeHeaderString(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		return inputToUse.getBytes(StandardCharsets.UTF_8);
	}

	/**
	 * See STOMP Spec 1.2:
	 * <a href="http://stomp.github.io/stomp-specification-1.2.html#Value_Encoding">"Value Encoding"</a>.
	 */
	private String escape(String inString) {
		StringBuilder sb = new StringBuilder(inString.length());
		for (int i = 0; i < inString.length(); i++) {
			char c = inString.charAt(i);
			if (c == '\\') {
				sb.append("\\\\");
			}
			else if (c == ':') {
				sb.append("\\c");
			}
			else if (c == '\n') {
				 sb.append("\\n");
			}
			else if (c == '\r') {
				sb.append("\\r");
			}
			else {
				sb.append(c);
			}
		}
		return sb.toString();
	}

	private void writeBody(byte[] payload, DataOutputStream output) throws IOException {
		output.write(payload);
	}

}
