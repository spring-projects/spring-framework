/*
 * Copyright 2002-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.lang.Nullable;

/**
 * A contract for invoking a chain of {@link ResourceResolver ResourceResolvers} where each resolver
 * is given a reference to the chain allowing it to delegate when necessary.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public interface ResourceResolverChain {

	/**
	 * Resolve the supplied request and request path to a {@link Resource} that
	 * exists under one of the given resource locations.
	 * @param request the current request
	 * @param requestPath the portion of the request path to use
	 * @param locations the locations to search in when looking up resources
	 * @return the resolved resource, or {@code null} if unresolved
	 */
	@Nullable
	Resource resolveResource(
			@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations);

	/**
	 * Resolve the externally facing <em>public</em> URL path for clients to use
	 * to access the resource that is located at the given <em>internal</em>
	 * resource path.
	 * <p>This is useful when rendering URL links to clients.
	 * @param resourcePath the internal resource path
	 * @param locations the locations to search in when looking up resources
	 * @return the resolved public URL path, or {@code null} if unresolved
	 */
	@Nullable
	String resolveUrlPath(String resourcePath, List<? extends Resource> locations);

}
