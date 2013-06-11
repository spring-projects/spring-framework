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
package org.springframework.web.messaging.stomp.support;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.messaging.GenericMessage;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;
import org.springframework.web.messaging.stomp.StompCommand;
import org.springframework.web.messaging.stomp.StompConversionException;
import org.springframework.web.messaging.stomp.StompHeaders;


/**
 * @author Gary Russell
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class StompMessageConverter {

	private static final Charset STOMP_CHARSET = Charset.forName("UTF-8");

	public static final byte LF = 0x0a;

	public static final byte CR = 0x0d;

	private static final byte COLON = ':';


	/**
	 * @param stompContent a complete STOMP message (without the trailing 0x00) as byte[] or String.
	 */
	public Message<byte[]> toMessage(Object stompContent, String sessionId) {

		byte[] byteContent = null;
		if (stompContent instanceof String) {
			byteContent = ((String) stompContent).getBytes(STOMP_CHARSET);
		}
		else if (stompContent instanceof byte[]){
			byteContent = (byte[]) stompContent;
		}
		else {
			throw new IllegalArgumentException(
					"stompContent is neither String nor byte[]: " + stompContent.getClass());
		}

		int totalLength = byteContent.length;
		if (byteContent[totalLength-1] == 0) {
			totalLength--;
		}

		int payloadIndex = findIndexOfPayload(byteContent);
		if (payloadIndex == 0) {
			throw new StompConversionException("No command found");
		}

		String headerContent = new String(byteContent, 0, payloadIndex, STOMP_CHARSET);
		Parser parser = new Parser(headerContent);

		// TODO: validate command and whether a payload is allowed
		StompCommand command = StompCommand.valueOf(parser.nextToken(LF).trim());
		Assert.notNull(command, "No command found");

		StompHeaders stompHeaders = new StompHeaders(command);
		stompHeaders.setSessionId(sessionId);

		while (parser.hasNext()) {
			String header = parser.nextToken(COLON);
			if (header != null) {
				if (parser.hasNext()) {
					String value = parser.nextToken(LF);
					stompHeaders.getRawHeaders().put(header, value);
				}
				else {
					throw new StompConversionException("Parse exception for " + headerContent);
				}
			}
		}

		byte[] payload = new byte[totalLength - payloadIndex];
		System.arraycopy(byteContent, payloadIndex, payload, 0, totalLength - payloadIndex);

		stompHeaders.updateMessageHeaders();

		return createMessage(command, stompHeaders.getMessageHeaders(), payload);
	}

	private int findIndexOfPayload(byte[] bytes) {
		int i;
		// ignore any leading EOL from the previous message
		for (i = 0; i < bytes.length; i++) {
			if (bytes[i] != '\n' && bytes[i] != '\r') {
				break;
			}
			bytes[i] = ' ';
		}
		int index = 0;
		for (; i < bytes.length - 1; i++) {
			if (bytes[i] == LF && bytes[i+1] == LF) {
				index = i + 2;
				break;
			}
			if ((i < (bytes.length - 3)) &&
					(bytes[i] == CR && bytes[i+1] == LF && bytes[i+2] == CR && bytes[i+3] == LF)) {
				index = i + 4;
				break;
			}
		}
		if (i >= bytes.length) {
			throw new StompConversionException("No end of headers found");
		}
		return index;
	}

	protected Message<byte[]> createMessage(StompCommand command, Map<String, Object> headers, byte[] payload) {
		return new GenericMessage<byte[]>(payload, headers);
	}

	public byte[] fromMessage(Message<byte[]> message) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		MessageHeaders messageHeaders = message.getHeaders();
		StompHeaders stompHeaders = new StompHeaders(messageHeaders, false);
		stompHeaders.updateRawHeaders();
		try {
			out.write(stompHeaders.getStompCommand().toString().getBytes("UTF-8"));
			out.write(LF);
			for (Entry<String, String> entry : stompHeaders.getRawHeaders().entrySet()) {
				String key = entry.getKey();
				key = replaceAllOutbound(key);
				String value = entry.getValue();
				out.write(key.getBytes("UTF-8"));
				out.write(COLON);
				value = replaceAllOutbound(value);
				out.write(value.getBytes("UTF-8"));
				out.write(LF);
			}
			out.write(LF);
			out.write(message.getPayload());
			out.write(0);
			return out.toByteArray();
		}
		catch (IOException e) {
			throw new StompConversionException("Failed to serialize " + message, e);
		}
	}

	private String replaceAllOutbound(String key) {
		return key.replaceAll("\\\\", "\\\\")
				.replaceAll(":", "\\\\c")
				.replaceAll("\n", "\\\\n")
				.replaceAll("\r", "\\\\r");
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
					throw new StompConversionException("No delimiter found at offset " + offset + " in " + this.content);
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
					throw new StompConversionException("Invalid escape sequence \\" + escaped);
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
