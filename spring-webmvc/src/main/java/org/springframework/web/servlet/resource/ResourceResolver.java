/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.resource;

import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;

/**
 * A strategy for resolving a request to a server-side resource.
 *
 * <p>Provides mechanisms for resolving an incoming request to an actual
 * {@link org.springframework.core.io.Resource} and for obtaining the
 * public URL path that clients should use when requesting the resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 * @see org.springframework.web.servlet.resource.ResourceResolverChain
 */
public interface ResourceResolver {

	/**
	 * Resolve the supplied request and request path to a {@link Resource} that
	 * exists under one of the given resource locations.
	 * @param request the current request (may not be present in some calls)
	 * @param requestPath the portion of the request path to use
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of remaining resolvers to delegate to
	 * @return the resolved resource, or {@code null} if unresolved
	 */
	@Nullable Resource resolveResource(@Nullable HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * Resolve the externally facing <em>public</em> URL path for clients to use
	 * to access the resource that is located at the given <em>internal</em>
	 * resource path.
	 * <p>This is useful when rendering URL links to clients.
	 * @param resourcePath the internal resource path
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of resolvers to delegate to
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	@Nullable String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
