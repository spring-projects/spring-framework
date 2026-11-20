/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

/**
 * Factory to create {@link UriBuilder} instances with shared configuration
 * such as a base URI, an encoding mode strategy, and others across all URI
 * builder instances created through a factory.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 * @see DefaultUriBuilderFactory
 */
public interface UriBuilderFactory extends UriTemplateHandler {

	/**
	 * Initialize a builder with the given URI template.
	 * @param uriTemplate the URI template to use
	 * @return the builder instance
	 */
	UriBuilder uriString(String uriTemplate);

	/**
	 * Create a URI builder with default settings.
	 * @return the builder instance
	 */
	UriBuilder builder();

}
