package org.springframework.web.reactive.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

/**
 * Extended interface for a {@link Resource} to be written to an
 * HTTP response.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface HttpResource extends Resource {

	/**
	 * The HTTP headers to be contributed to the HTTP response
	 * that serves the current resource.
	 * @return the HTTP response headers
	 */
	HttpHeaders getResponseHeaders();
}
