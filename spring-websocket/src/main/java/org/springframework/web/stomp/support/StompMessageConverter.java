/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.web.stomp.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.util.Assert;
import org.springframework.web.stomp.StompCommand;
import org.springframework.web.stomp.StompException;
import org.springframework.web.stomp.StompHeaders;
import org.springframework.web.stomp.StompMessage;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class StompMessageConverter {

	public static final byte LF = 0x0a;

	public static final byte CR = 0x0d;

	private static final byte COLON = ':';

	/**
	 * @param bytes a complete STOMP message (without the trailing 0x00).
	 */
	public StompMessage toStompMessage(Object stomp) {
		Assert.state(stomp instanceof String || stomp instanceof byte[], "'stomp' must be String or byte[]");
		byte[] stompBytes = null;
		if (stomp instanceof String) {
			stompBytes = ((String) stomp).getBytes(StompMessage.CHARSET);
		}
		else {
			stompBytes = (byte[]) stomp;
		}
		int totalLength = stompBytes.length;
		if (stompBytes[totalLength-1] == 0) {
			totalLength--;
		}
		int payloadIndex = findPayloadStart(stompBytes);
		if (payloadIndex == 0) {
			throw new StompException("No command found");
		}
		String headerString = new String(stompBytes, 0, payloadIndex, StompMessage.CHARSET);
		Parser parser = new Parser(headerString);
		StompHeaders headers = new StompHeaders();
		// TODO: validate command and whether a payload is allowed
		StompCommand command = StompCommand.valueOf(parser.nextToken(LF).trim());
		Assert.notNull(command, "No command found");
		while (parser.hasNext()) {
			String header = parser.nextToken(COLON);
			if (header != null) {
				if (parser.hasNext()) {
					String value = parser.nextToken(LF);
					headers.add(header, value);
				}
				else {
					throw new StompException("Parse exception for " + headerString);
				}
			}
		}
		byte[] payload = new byte[totalLength - payloadIndex];
		System.arraycopy(stompBytes, payloadIndex, payload, 0, totalLength - payloadIndex);
		return new StompMessage(command, headers, payload);
	}

	public byte[] fromStompMessage(StompMessage message) {
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		StompHeaders headers = message.getHeaders();
		StompCommand command = message.getCommand();
		try {
			outputStream.write(command.toString().getBytes("UTF-8"));
			outputStream.write(LF);
			for (Entry<String, List<String>> entry : headers.entrySet()) {
				String key = entry.getKey();
				key = replaceAllOutbound(key);
				for (String value : entry.getValue()) {
					outputStream.write(key.getBytes("UTF-8"));
					outputStream.write(COLON);
					value = replaceAllOutbound(value);
					outputStream.write(value.getBytes("UTF-8"));
					outputStream.write(LF);
				}
			}
			outputStream.write(LF);
			outputStream.write(message.getPayload());
			outputStream.write(0);
			return outputStream.toByteArray();
		}
		catch (IOException e) {
			throw new StompException("Failed to serialize " + message, e);
		}
	}

	private String replaceAllOutbound(String key) {
		return key.replaceAll("\\\\", "\\\\")
				.replaceAll(":", "\\\\c")
				.replaceAll("\n", "\\\\n")
				.replaceAll("\r", "\\\\r");
	}

	private int findPayloadStart(byte[] bytes) {
		int i;
		// ignore any leading EOL from the previous message
		for (i = 0; i < bytes.length; i++) {
			if (bytes[i] != '\n' && bytes[i] != '\r' ) {
				break;
			}
			bytes[i] = ' ';
		}
		int payloadOffset = 0;
		for (; i < bytes.length - 1; i++) {
			if ((bytes[i] == LF && bytes[i+1] == LF)) {
				payloadOffset = i + 2;
				break;
			}
			if (i < bytes.length - 3 &&
				(bytes[i] == CR && bytes[i+1] == LF &&
				 bytes[i+2] == CR && bytes[i+3] == LF)) {
				payloadOffset = i + 4;
				break;
			}
		}
		if (i >= bytes.length) {
			throw new StompException("No end of headers found");
		}
		return payloadOffset;
	}

	private class Parser {

		private final String content;

		private int offset;

		public Parser(String content) {
			this.content = content;
		}

		public boolean hasNext() {
			return this.offset < this.content.length();
		}

		public String nextToken(byte delimiter) {
			if (this.offset >= this.content.length()) {
				return null;
			}
			int delimAt = this.content.indexOf(delimiter, this.offset);
			if (delimAt == -1) {
				if (this.offset == this.content.length() - 1 && delimiter == COLON &&
						this.content.charAt(this.offset) == LF) {
					this.offset++;
					return null;
				}
				else if (this.offset == this.content.length() - 2 && delimiter == COLON &&
						this.content.charAt(this.offset) == CR &&
						this.content.charAt(this.offset + 1) == LF) {
					this.offset += 2;
					return null;
				}
				else {
					throw new StompException("No delimiter found at offset " + offset + " in " + this.content);
				}
			}
			int escapeAt = this.content.indexOf('\\', this.offset);
			String token = this.content.substring(this.offset, delimAt + 1);
			this.offset += token.length();
			if (escapeAt >= 0 && escapeAt < delimAt) {
				char escaped = this.content.charAt(escapeAt + 1);
				if (escaped == 'n' || escaped == 'c' || escaped == '\\') {
					token = token.replaceAll("\\\\n", "\n")
							.replaceAll("\\\\r", "\r")
							.replaceAll("\\\\c", ":")
							.replaceAll("\\\\\\\\", "\\\\");
				}
				else {
					throw new StompException("Invalid escape sequence \\" + escaped);
				}
			}
			int length = token.length();
			if (delimiter == LF && length > 1 && token.charAt(length - 2) == CR) {
				return token.substring(0, length - 2);
			}
			else {
				return token.substring(0, length - 1);
			}
		}
	}
}
