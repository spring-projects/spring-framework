/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

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

	private @Nullable MimeType defaultMimeType;


	/**
	 * Set the default MIME type to use when there is no
	 * {@link MessageHeaders#CONTENT_TYPE} header present.
	 * <p>This property does not have a default value.
	 */
	public void setDefaultMimeType(@Nullable MimeType defaultMimeType) {
		this.defaultMimeType = defaultMimeType;
	}

	/**
	 * Return the default MIME type to use if no
	 * {@link MessageHeaders#CONTENT_TYPE} header is present.
	 */
	public @Nullable MimeType getDefaultMimeType() {
		return this.defaultMimeType;
	}


	@Override
	public @Nullable MimeType resolve(@Nullable MessageHeaders headers) {
		if (headers == null || headers.get(MessageHeaders.CONTENT_TYPE) == null) {
			return this.defaultMimeType;
		}
		Object value = headers.get(MessageHeaders.CONTENT_TYPE);
		if (value == null) {
			return null;
		}
		else if (value instanceof MimeType mimeType) {
			return mimeType;
		}
		else if (value instanceof String text) {
			return MimeType.valueOf(text);
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
