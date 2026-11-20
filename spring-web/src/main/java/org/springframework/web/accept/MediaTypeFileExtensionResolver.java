/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.accept;

import java.util.List;

import org.springframework.http.MediaType;

/**
 * Strategy to resolve a {@link MediaType} to a list of file extensions &mdash;
 * for example, to resolve "application/json" to "json".
 *
 * @author Rossen Stoyanchev
 * @since 3.2
 */
public interface MediaTypeFileExtensionResolver {

	/**
	 * Resolve the given media type to a list of file extensions.
	 * @param mediaType the media type to resolve
	 * @return a list of extensions or an empty list (never {@code null})
	 */
	List<String> resolveFileExtensions(MediaType mediaType);

	/**
	 * Get all registered file extensions.
	 * @return a list of extensions or an empty list (never {@code null})
	 */
	List<String> getAllFileExtensions();

}
