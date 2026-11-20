/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.util;

import java.net.URI;
import java.util.Map;

import org.jspecify.annotations.Nullable;

/**
 * Defines methods for expanding a URI template with variables.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public interface UriTemplateHandler {

	/**
	 * Expand the given URI template with a map of URI variables.
	 * @param uriTemplate the URI template
	 * @param uriVariables variable values
	 * @return the created URI instance
	 */
	URI expand(String uriTemplate, Map<String, ? extends @Nullable Object> uriVariables);

	/**
	 * Expand the given URI template with an array of URI variables.
	 * @param uriTemplate the URI template
	 * @param uriVariables variable values
	 * @return the created URI instance
	 */
	URI expand(String uriTemplate, @Nullable Object... uriVariables);

}
