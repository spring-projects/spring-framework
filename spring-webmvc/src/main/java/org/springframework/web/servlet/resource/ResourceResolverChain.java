/*
 * Copyright 2002-2013 the original author or authors.
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
 * A contract for invoking a chain of {@link ResourceResolver}s. Each resolver is passed a
 * reference to the chain allowing it delegate to the remaining resolvers.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public interface ResourceResolverChain {

	/**
	 * Resolve the URL path of an incoming request to an actual {@link Resource}.
	 *
	 * @param request the current request
	 * @param requestPath the portion of the request path to use
	 * @param locations the configured locations where to look up resources
	 *
	 * @return the resolved {@link Resource} or {@code null} if this resolver could not
	 *         resolve the resource
	 */
	Resource resolveResource(HttpServletRequest request, String requestPath, List<Resource> locations);

	/**
	 * Resolve the given resource path to a URL path. This is useful when rendering URL
	 * links to clients to determine the actual URL to use.
	 *
	 * @param resourcePath the resource path
	 * @param locations the configured locations where to look up resources
	 *
	 * @return the resolved URL path or {@code null} if this resolver could not resolve
	 *         the given resource path
	 */
	String resolveUrlPath(String resourcePath, List<Resource> locations);

}
