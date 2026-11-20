/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;

/**
 * A strategy to determine the version of a static resource and to apply and/or
 * extract it from the URL path.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @since 5.0
 * @see VersionResourceResolver
*/
public interface VersionStrategy {

	/**
	 * Extract the resource version from the request path.
	 * @param requestPath the request path to check
	 * @return the version string or {@code null} if none was found
	 */
	@Nullable String extractVersion(String requestPath);

	/**
	 * Remove the version from the request path. It is assumed that the given
	 * version was extracted via {@link #extractVersion(String)}.
	 * @param requestPath the request path of the resource being resolved
	 * @param version the version obtained from {@link #extractVersion(String)}
	 * @return the request path with the version removed
	 */
	String removeVersion(String requestPath, String version);

	/**
	 * Add a version to the given request path.
	 * @param requestPath the requestPath
	 * @param version the version
	 * @return the requestPath updated with a version string
	 */
	String addVersion(String requestPath, String version);

	/**
	 * Determine the version for the given resource.
	 * @param resource the resource to check
	 * @return the resource version
	 */
	Mono<String> getResourceVersion(Resource resource);

}
