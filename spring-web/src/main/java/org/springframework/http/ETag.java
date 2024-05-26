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

import org.springframework.util.Assert;

/**
 * ETag header value holder.
 *
 * @author Riley Park
 * @since TODO
 * @param value value that uniquely represents the resource
 * @param weak if weak validation should be used
 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag">ETag Header</a>
 */
public record ETag(
		String value,
		boolean weak
) {

	/**
	 * ETag prefix.
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag#directives">ETag Header Directives</a>
	 */
	public static final String PREFIX = "\"";

	/**
	 * ETag prefix, with a weak validator.
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag#directives">ETag Header Directives</a>
	 * @see <a href=https://developer.mozilla.org/en-US/docs/Web/HTTP/Conditional_requests#weak_validation">Weak Validation</a>
	 */
	public static final String PREFIX_WEAK = "W/\"";

	/**
	 * ETag suffix.
	 *
	 * @see <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/ETag#directives">ETag Header Directives</a>
	 */
	public static final String SUFFIX = "\"";

	/**
	 * Parses an {@code ETag} header value as defined in RFC 7232.
	 * @param etag the {@literal ETag} header value
	 * @return the parsed content disposition
	 * @see #toString()
	 */
	public static ETag parse(String etag) {
		boolean weak = etag.startsWith(PREFIX_WEAK);
		Assert.isTrue(etag.startsWith(PREFIX) || weak,
				"Invalid ETag: does not start with " + PREFIX + " or " + PREFIX_WEAK);
		Assert.isTrue(etag.endsWith(SUFFIX), "Invalid ETag: does not end with " + SUFFIX);
		int start = (weak ? PREFIX_WEAK.length() : PREFIX.length());
		String value = etag.substring(start, etag.length() - SUFFIX.length());
		return new ETag(value, weak);
	}

	public String toHeaderValue() {
		return (weak ? PREFIX_WEAK : PREFIX) + value + SUFFIX;
	}

}
