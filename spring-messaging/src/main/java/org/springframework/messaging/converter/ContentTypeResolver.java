/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.messaging.converter;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHeaders;
import org.springframework.util.InvalidMimeTypeException;
import org.springframework.util.MimeType;

/**
 * Resolve the content type for a message.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
@FunctionalInterface
public interface ContentTypeResolver {

	/**
	 * Determine the {@link MimeType} of a message from the given MessageHeaders.
	 * @param headers the headers to use for the resolution
	 * @return the resolved {@code MimeType}, or {@code null} if none found
	 * @throws InvalidMimeTypeException if the content type is a String that cannot be parsed
	 * @throws IllegalArgumentException if there is a content type but its type is unknown
	 */
	@Nullable MimeType resolve(@Nullable MessageHeaders headers) throws InvalidMimeTypeException;

}
