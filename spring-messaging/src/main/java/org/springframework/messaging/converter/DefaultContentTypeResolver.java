/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.messaging.converter;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.MimeType;

/**
 * A default {@link ContentTypeResolver} that checks the
 * {@link MessageHeaders#CONTENT_TYPE} header or falls back to a default value.
 *
 * <p>The header value is expected to be a {@link org.springframework.util.MimeType}
 * or a {@code String} that can be parsed into a {@code MimeType}.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class DefaultContentTypeResolver implements ContentTypeResolver {

	private MimeType defaultMimeType;


	/**
	 * Set the default MIME type to use when there is no
	 * {@link MessageHeaders#CONTENT_TYPE} header present.
	 * <p>This property does not have a default value.
	 */
	public void setDefaultMimeType(MimeType defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	/**
	 * Return the default MIME type to use if no
	 * {@link MessageHeaders#CONTENT_TYPE} header is present.
	 */
	public MimeType getDefaultMimeType() {
		return this.defaultMimeType;
	}


	@Override
	public MimeType resolve(MessageHeaders headers) {
		if (headers == null || headers.get(MessageHeaders.CONTENT_TYPE) == null) {
			return this.defaultMimeType;
		}
		Object value = headers.get(MessageHeaders.CONTENT_TYPE);
		if (value instanceof MimeType) {
			return (MimeType) value;
		}
		else if (value instanceof String) {
			return MimeType.valueOf((String) value);
		}
		else {
			throw new IllegalArgumentException(
					"Unknown type for contentType header value: " + value.getClass());
		}
	}

	@Override
	public String toString() {
		return "DefaultContentTypeResolver[" + "defaultMimeType=" + this.defaultMimeType + "]";
	}

}
