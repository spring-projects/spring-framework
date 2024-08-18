/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.http;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.StringUtils;

/**
 * Represents an ETag for HTTP conditional requests.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.38
 * @param tag the unquoted tag value
 * @param weak whether the entity tag is for weak or strong validation
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc7232">RFC 7232</a>
 */
public record ETag(String tag, boolean weak) {

	private static final Log logger = LogFactory.getLog(ETag.class);

	private static final ETag WILDCARD = new ETag("*", false);


	/**
	 * Whether this a wildcard tag matching to any entity tag value.
	 */
	public boolean isWildcard() {
		return (this == WILDCARD);
	}

	/**
	 * Return the fully formatted tag including "W/" prefix and quotes.
	 */
	public String formattedTag() {
		if (isWildcard()) {
			return "*";
		}
		return (this.weak ? "W/" : "") + "\"" + this.tag + "\"";
	}

	@Override
	public String toString() {
		return formattedTag();
	}


	/**
	 * Parse entity tags from an "If-Match" or "If-None-Match" header.
	 * @param source the source string to parse
	 * @return the parsed ETags
	 */
	public static List<ETag> parse(String source) {

		List<ETag> result = new ArrayList<>();
		State state = State.BEFORE_QUOTES;
		int startIndex = -1;
		boolean weak = false;

		for (int i = 0; i < source.length(); i++) {
			char c = source.charAt(i);

			if (state == State.IN_QUOTES) {
				if (c == '"') {
					String tag = source.substring(startIndex, i);
					if (StringUtils.hasText(tag)) {
						result.add(new ETag(tag, weak));
					}
					state = State.AFTER_QUOTES;
					startIndex = -1;
					weak = false;
				}
				continue;
			}

			if (Character.isWhitespace(c)) {
				continue;
			}

			if (c == ',') {
				state = State.BEFORE_QUOTES;
				continue;
			}

			if (state == State.BEFORE_QUOTES) {
				if (c == '*') {
					result.add(WILDCARD);
					state = State.AFTER_QUOTES;
					continue;
				}
				if (c == '"') {
					state = State.IN_QUOTES;
					startIndex = i + 1;
					continue;
				}
				if (c == 'W' && source.length() > i + 2) {
					if (source.charAt(i + 1) == '/' && source.charAt(i + 2) == '"') {
						state = State.IN_QUOTES;
						i = i + 2;
						startIndex = i + 1;
						weak = true;
						continue;
					}
				}
			}

			if (logger.isDebugEnabled()) {
				logger.debug("Unexpected char at index " + i);
			}
		}

		if (state != State.IN_QUOTES && logger.isDebugEnabled()) {
			logger.debug("Expected closing '\"'");
		}

		return result;
	}


	private enum State {

		BEFORE_QUOTES, IN_QUOTES, AFTER_QUOTES

	}

}
