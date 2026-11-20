/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.http.server;

import java.net.URI;

import org.jspecify.annotations.Nullable;

/**
 * Specialization of {@link PathContainer} that subdivides the path into a
 * {@link #contextPath()} and the remaining {@link #pathWithinApplication()}.
 * The latter is typically used for request mapping within the application
 * while the former is useful when preparing external links that point back to
 * the application.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface RequestPath extends PathContainer {

	/**
	 * Returns the portion of the URL path that represents the application.
	 * The context path is always at the beginning of the path and starts but
	 * does not end with "/". It is shared for URLs of the same application.
	 * <p>The context path may come from the underlying runtime API such as
	 * when deploying as a WAR to a Servlet container or it may be assigned in
	 * a WebFlux application through the use of
	 * {@link org.springframework.http.server.reactive.ContextPathCompositeHandler
	 * ContextPathCompositeHandler}.
	 */
	PathContainer contextPath();

	/**
	 * The portion of the request path after the context path which is typically
	 * used for request mapping within the application.
	 */
	PathContainer pathWithinApplication();

	/**
	 * Return a new {@code RequestPath} instance with a modified context path.
	 * The new context path must match 0 or more path segments at the start.
	 * @param contextPath the new context path
	 * @return a new {@code RequestPath} instance
	 */
	RequestPath modifyContextPath(String contextPath);


	/**
	 * Parse the URI for a request into a {@code RequestPath}.
	 * @param uri the URI of the request
	 * @param contextPath the contextPath portion of the URI path
	 */
	static RequestPath parse(URI uri, @Nullable String contextPath) {
		return parse(uri.getRawPath(), contextPath);
	}

	/**
	 * Variant of {@link #parse(URI, String)} with the encoded
	 * {@link URI#getRawPath() raw path}.
	 * @param rawPath the path
	 * @param contextPath the contextPath portion of the URI path
	 * @since 5.3
	 */
	static RequestPath parse(String rawPath, @Nullable String contextPath) {
		return new DefaultRequestPath(rawPath, contextPath);
	}

}
