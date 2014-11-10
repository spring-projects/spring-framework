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
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageType;

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

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final Log logger = LogFactory.getLog(StompEncoder.class);


	/**
	 * Encodes the given STOMP {@code message} into a {@code byte[]}
	 * @param message the message to encode
	 * @return the encoded message
	 */
	public byte[] encode(Message<byte[]> message) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream(128 + message.getPayload().length);
			DataOutputStream output = new DataOutputStream(baos);

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
			if (SimpMessageType.HEARTBEAT == headers.getMessageType()) {
				logger.trace("Encoded heartbeat");
				output.write(message.getPayload());
			}
			else {
				output.write(headers.getCommand().toString().getBytes(UTF8_CHARSET));
				output.write(LF);
				writeHeaders(headers, message, output);
				output.write(LF);
				writeBody(message, output);
				output.write((byte) 0);
			}

			return baos.toByteArray();
		}
		catch (IOException ex) {
			throw new StompConversionException("Failed to encode STOMP frame",  ex);
		}
	}

	private void writeHeaders(StompHeaderAccessor headers, Message<byte[]> message, DataOutputStream output)
			throws IOException {

		StompCommand command = headers.getCommand();
		Map<String,List<String>> stompHeaders = headers.toStompHeaderMap();
		boolean shouldEscape = (command != StompCommand.CONNECT && command != StompCommand.CONNECTED);

		if (logger.isDebugEnabled()) {
			logger.debug("Encoded STOMP " + command + ", headers=" + stompHeaders);
		}

		for (Entry<String, List<String>> entry : stompHeaders.entrySet()) {
			byte[] key = encodeHeaderString(entry.getKey(), shouldEscape);
			for (String value : entry.getValue()) {
				output.write(key);
				output.write(COLON);
				output.write(encodeHeaderString(value, shouldEscape));
				output.write(LF);
			}
		}
		if (command.requiresContentLength()) {
			int contentLength = message.getPayload().length;
			output.write("content-length:".getBytes(UTF8_CHARSET));
			output.write(Integer.toString(contentLength).getBytes(UTF8_CHARSET));
			output.write(LF);
		}
	}

	private byte[] encodeHeaderString(String input, boolean escape) {
		String inputToUse = (escape ? escape(input) : input);
		return inputToUse.getBytes(UTF8_CHARSET);
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

	private void writeBody(Message<byte[]> message, DataOutputStream output) throws IOException {
		output.write(message.getPayload());
	}

}
