/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;


/**
 * A strategy for matching a request to a server-side resource. Provides a mechanism
 * for resolving an incoming request to an actual
 * {@link org.springframework.core.io.Resource} and also for obtaining the public
 * URL path clients should use when requesting the resource.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.1
 *
 * @see org.springframework.web.servlet.resource.ResourceResolverChain
 */
public interface ResourceResolver {

	/**
	 * Resolve the input request and request path to a {@link Resource} that
	 * exists under one of the given resource locations.
	 *
	 * @param request the current request
	 * @param requestPath the portion of the request path to use
	 * @param locations locations where to look for resources
	 * @param chain a chain with other resolvers to delegate to
	 *
	 * @return the resolved resource or {@code null} if unresolved
	 */
	Resource resolveResource(HttpServletRequest request, String requestPath,
			List<Resource> locations, ResourceResolverChain chain);

	/**
	 * Get the externally facing public URL path for clients to use to access the
	 * resource located at the resource URL path.
	 *
	 * @param resourceUrlPath the candidate resource URL path
	 * @param locations the configured locations where to look up resources
	 * @param chain the chain with remaining resolvers to delegate to
	 *
	 * @return the resolved URL path or {@code null} if unresolved
	 */
	String getPublicUrlPath(String resourceUrlPath, List<Resource> locations, ResourceResolverChain chain);

}
