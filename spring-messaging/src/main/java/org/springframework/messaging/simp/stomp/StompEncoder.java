/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
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
import java.util.Map.Entry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.messaging.Message;

/**
 * An encoder for STOMP frames
 *
 * @author Andy Wilkinson
 * @since 4.0
 */
public final class StompEncoder  {

	private static final byte LF = '\n';

	private static final byte COLON = ':';

	private static final Charset UTF8_CHARSET = Charset.forName("UTF-8");

	private final Log logger = LogFactory.getLog(StompEncoder.class);

	/**
	 * Encodes the given STOMP {@code message} into a {@code byte[]}
	 *
	 * @param message The message to encode
	 *
	 * @return The encoded message
	 */
	public byte[] encode(Message<byte[]> message) {
		try {
			if (logger.isTraceEnabled()) {
				logger.trace("Encoding " + message);
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DataOutputStream output = new DataOutputStream(baos);

			StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);

			if (isHeartbeat(headers)) {
				output.write(message.getPayload());
			} else {
				writeCommand(headers, output);
				writeHeaders(headers, message, output);
				output.write(LF);
				writeBody(message, output);
				output.write((byte)0);
			}

			return baos.toByteArray();
		}
		catch (IOException e) {
			throw new StompConversionException("Failed to encode STOMP frame",  e);
		}
	}

	private boolean isHeartbeat(StompHeaderAccessor headers) {
		return headers.getCommand() == null;
	}

	private void writeCommand(StompHeaderAccessor headers, DataOutputStream output) throws IOException {
		output.write(headers.getCommand().toString().getBytes(UTF8_CHARSET));
		output.write(LF);
	}

	private void writeHeaders(StompHeaderAccessor headers, Message<byte[]> message, DataOutputStream output)
			throws IOException {

		for (Entry<String, List<String>> entry : headers.toStompHeaderMap().entrySet()) {
			byte[] key = getUtf8BytesEscapingIfNecessary(entry.getKey(), headers);
			for (String value : entry.getValue()) {
				output.write(key);
				output.write(COLON);
				output.write(getUtf8BytesEscapingIfNecessary(value, headers));
				output.write(LF);
			}
		}
		if (headers.getCommand() == StompCommand.SEND ||
				headers.getCommand() == StompCommand.MESSAGE ||
				headers.getCommand() == StompCommand.ERROR) {
			output.write("content-length:".getBytes(UTF8_CHARSET));
			output.write(Integer.toString(message.getPayload().length).getBytes(UTF8_CHARSET));
			output.write(LF);
		}
	}

	private void writeBody(Message<byte[]> message, DataOutputStream output) throws IOException {
		output.write(message.getPayload());
	}

	private byte[] getUtf8BytesEscapingIfNecessary(String input, StompHeaderAccessor headers) {
		if (headers.getCommand() != StompCommand.CONNECT && headers.getCommand() != StompCommand.CONNECTED) {
			return escape(input).getBytes(UTF8_CHARSET);
		}
		else {
			return input.getBytes(UTF8_CHARSET);
		}
	}

	private String escape(String input) {
		return input.replaceAll("\\\\", "\\\\\\\\")
				.replaceAll(":", "\\\\c")
				.replaceAll("\n", "\\\\n")
				.replaceAll("\r", "\\\\r");
	}
}