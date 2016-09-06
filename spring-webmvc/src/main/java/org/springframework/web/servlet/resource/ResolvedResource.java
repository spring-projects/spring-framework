package org.springframework.web.servlet.resource;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;

/**
 * Interface for resources resolved through the
 * {@link org.springframework.web.servlet.resource.ResourceResolverChain}
 * that may contribute HTTP response headers as they're served to HTTP clients.
 *
 * <p>Some resource implementations, while served by the
 * {@link org.springframework.web.servlet.resource.ResourceResolverChain} need
 * to contribute resource metadata as HTTP response headers so that HTTP clients
 * can interpret them properly.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public interface ResolvedResource extends Resource {

	/**
	 * The HTTP headers to be contributed to the HTTP response
	 * that serves the current resource.
	 * @return the HTTP response headers
	 */
	HttpHeaders getResponseHeaders();
}
