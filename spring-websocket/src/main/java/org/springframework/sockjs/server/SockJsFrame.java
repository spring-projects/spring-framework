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
package org.springframework.sockjs.server;

import java.nio.charset.Charset;

import org.springframework.util.Assert;

import com.fasterxml.jackson.core.io.JsonStringEncoder;


/**
 *
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class SockJsFrame {

	private static final SockJsFrame OPEN_FRAME = new SockJsFrame("o");

	private static final SockJsFrame HEARTBEAT_FRAME = new SockJsFrame("h");

	private static final SockJsFrame CLOSE_GO_AWAY_FRAME = closeFrame(3000, "Go away!");

	private static final SockJsFrame CLOSE_ANOTHER_CONNECTION_OPEN = closeFrame(2010, "Another connection still open");


	private final String content;


	private SockJsFrame(String content) {
		this.content = content;
	}

	public static SockJsFrame openFrame() {
		return OPEN_FRAME;
	}

	public static SockJsFrame heartbeatFrame() {
		return HEARTBEAT_FRAME;
	}

	public static SockJsFrame messageFrame(String... messages) {
		return new MessageFrame(messages);
	}

	public static SockJsFrame closeFrameGoAway() {
		return CLOSE_GO_AWAY_FRAME;
	}

	public static SockJsFrame closeFrameAnotherConnectionOpen() {
		return CLOSE_ANOTHER_CONNECTION_OPEN;
	}

	public static SockJsFrame closeFrame(int code, String reason) {
		return new SockJsFrame("c[" + code + ",\"" + reason + "\"]");
	}


	public String getContent() {
		return this.content;
	}

	public byte[] getContentBytes() {
		return this.content.getBytes(Charset.forName("UTF-8"));
	}

	public String toString() {
		String result = this.content;
		if (result.length() > 80) {
			result = result.substring(0, 80) + "...(truncated)";
		}
		return "SockJsFrame content='" + result.replace("\n", "\\n").replace("\r", "\\r") + "'";
	}


	private static class MessageFrame extends SockJsFrame {

		public MessageFrame(String... messages) {
			super(prepareContent(messages));
		}

		public static String prepareContent(String... messages) {
			Assert.notNull(messages, "messages required");
			StringBuilder sb = new StringBuilder();
			sb.append("a[");
			for (int i=0; i < messages.length; i++) {
				sb.append('"');
				// TODO: dependency on Jackson
				char[] quotedChars = JsonStringEncoder.getInstance().quoteAsString(messages[i]);
				sb.append(escapeSockJsCharacters(quotedChars));
				sb.append('"');
	            if (i < messages.length - 1) {
	                sb.append(',');
	            }
			}
			sb.append(']');
			return sb.toString();
		}

		private static String escapeSockJsCharacters(char[] chars) {
			StringBuilder result = new StringBuilder();
			for (char ch : chars) {
				if (isSockJsEscapeCharacter(ch)) {
					result.append('\\').append('u');
					String hex = Integer.toHexString(ch).toLowerCase();
					for (int i = 0; i < (4 - hex.length()); i++) {
						result.append('0');
					}
					result.append(hex);
				}
				else {
					result.append(ch);
				}
			}
			return result.toString();
		}

		private static boolean isSockJsEscapeCharacter(char ch) {
			return (ch >= '\u0000' && ch <= '\u001F') || (ch >= '\u200C' && ch <= '\u200F')
					|| (ch >= '\u2028' && ch <= '\u202F') || (ch >= '\u2060' && ch <= '\u206F')
					|| (ch >= '\uFFF0' && ch <= '\uFFFF') || (ch >= '\uD800' && ch <= '\uDFFF');
		}
	}

	public interface FrameFormat {

		SockJsFrame format(SockJsFrame frame);
	}

	public static class DefaultFrameFormat implements FrameFormat {

		private final String format;

		public DefaultFrameFormat(String format) {
			Assert.notNull(format, "format is required");
			this.format = format;
		}

		/**
		 *
		 * @param format a String with a single %s formatting character where the
		 * frame content is to be inserted; e.g. "data: %s\r\n\r\n"
		 * @return new SockJsFrame instance with the formatted content
		 */
		public SockJsFrame format(SockJsFrame frame) {
			String content = String.format(this.format, preProcessContent(frame.getContent()));
			return new SockJsFrame(content);
		}

		protected String preProcessContent(String content) {
			return content;
		}
	}

}
