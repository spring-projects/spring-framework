/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.server.RequestPath;
import org.springframework.web.server.ServerWebExchange;

/**
 * A strategy for resolving a request to a server-side resource.
 *
 * <p>Provides mechanisms for resolving an incoming request to an actual
 * {@link Resource} and for obtaining the
 * public URL path that clients should use when requesting the resource.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceResolver {

	/**
	 * Resolve the supplied request and request path to a {@link Resource} that
	 * exists under one of the given resource locations.
	 * @param exchange the current exchange
	 * @param requestPath the portion of the request path to use. This is
	 * expected to be the encoded path, i.e. {@link RequestPath#value()}.
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of remaining resolvers to delegate to
	 * @return the resolved resource or an empty {@code Mono} if unresolved
	 */
	Mono<Resource> resolveResource(@Nullable ServerWebExchange exchange, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain);

	/**
	 * Resolve the externally facing <em>public</em> URL path for clients to use
	 * to access the resource that is located at the given <em>internal</em>
	 * resource path.
	 * <p>This is useful when rendering URL links to clients.
	 * @param resourcePath the "internal" resource path to resolve a path for
	 * public use. This is expected to be the encoded path.
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of resolvers to delegate to
	 * @return the resolved public URL path or an empty {@code Mono} if unresolved
	 */
	Mono<String> resolveUrlPath(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain);

}
