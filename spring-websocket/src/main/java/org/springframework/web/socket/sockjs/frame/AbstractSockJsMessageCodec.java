/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.socket.sockjs.frame;

import java.util.Locale;

import org.springframework.util.Assert;

/**
 * A base class for SockJS message codec that provides an implementation of
 * {@link #encode(String[])}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public abstract class AbstractSockJsMessageCodec implements SockJsMessageCodec {

	@Override
	public String encode(String... messages) {
		Assert.notNull(messages, "messages must not be null");
		StringBuilder sb = new StringBuilder();
		sb.append("a[");
		for (int i = 0; i < messages.length; i++) {
			sb.append('"');
			char[] quotedChars = applyJsonQuoting(messages[i]);
			sb.append(escapeSockJsSpecialChars(quotedChars));
			sb.append('"');
			if (i < messages.length - 1) {
				sb.append(',');
			}
		}
		sb.append(']');
		return sb.toString();
	}

	/**
	 * Apply standard JSON string quoting (see <a href="https://www.json.org/">json.org</a>).
	 */
	protected abstract char[] applyJsonQuoting(String content);

	/**
	 * See "JSON Unicode Encoding" section of SockJS protocol.
	 */
	private String escapeSockJsSpecialChars(char[] characters) {
		StringBuilder result = new StringBuilder();
		for (char c : characters) {
			if (isSockJsSpecialChar(c)) {
				result.append('\\').append('u');
				String hex = Integer.toHexString(c).toLowerCase(Locale.ROOT);
				result.append("0".repeat(Math.max(0, (4 - hex.length()))));
				result.append(hex);
			}
			else {
				result.append(c);
			}
		}
		return result.toString();
	}

	/**
	 * See `escapable_by_server` variable in the SockJS protocol test suite.
	 */
	private boolean isSockJsSpecialChar(char ch) {
		return (ch <= '\u001F') || (ch >= '\u200C' && ch <= '\u200F') ||
				(ch >= '\u2028' && ch <= '\u202F') || (ch >= '\u2060' && ch <= '\u206F') ||
				(ch >= '\uFFF0') || (ch >= '\uD800' && ch <= '\uDFFF');
	}

}
