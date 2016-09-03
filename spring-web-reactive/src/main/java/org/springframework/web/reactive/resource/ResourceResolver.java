/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.reactive.resource;

import java.util.List;

import org.springframework.core.io.Resource;
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
	 * @param requestPath the portion of the request path to use
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of remaining resolvers to delegate to
	 * @return the resolved resource or {@code null} if unresolved
	 */
	Resource resolveResource(ServerWebExchange exchange, String requestPath, List<? extends Resource> locations,
			ResourceResolverChain chain);

	/**
	 * Resolve the externally facing <em>public</em> URL path for clients to use
	 * to access the resource that is located at the given <em>internal</em>
	 * resource path.
	 * <p>This is useful when rendering URL links to clients.
	 * @param resourcePath the internal resource path
	 * @param locations the locations to search in when looking up resources
	 * @param chain the chain of resolvers to delegate to
	 * @return the resolved public URL path or {@code null} if unresolved
	 */
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations, ResourceResolverChain chain);

}
