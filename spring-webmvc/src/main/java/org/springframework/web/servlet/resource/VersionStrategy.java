/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.web.servlet.resource;

import java.util.List;

import org.springframework.core.io.Resource;

/**
 * A strategy for handling version strings in request paths when resolving
 * static resources with a {@link VersionResourceResolver}.
 *
 * @author Brian Clozel
 * @since 4.1
 * @see VersionResourceResolver
*/
public interface VersionStrategy {

	/**
	 * Extracts a version string from the request path.
	 * @param requestPath the request path of the resource being resolved
	 * @return the version string or an empty string if none was found
	 */
	String extractVersionFromPath(String requestPath);

	/**
	 * Deletes the given candidate version string from the given request path.
	 * @param requestPath the request path of the resource being resolved
	 * @param candidateVersion the candidate version string
	 * @return the modified request path, without the version string
	 */
	String deleteVersionFromPath(String requestPath, String candidateVersion);

	/**
	 * Checks whether the given {@code Resource} matches the candidate version string.
	 * Useful when the version string is managed on a per-resource basis.
	 * @param baseResource the resource to check against the given version
	 * @param candidateVersion the candidate version for the given resource
	 * @return true if the resource matches the version string, false otherwise
	 */
	boolean resourceVersionMatches(Resource baseResource, String candidateVersion);

	/**
	 * Adds a version string to the given baseUrl.
	 * @param baseUrl the baseUrl of the requested resource
	 * @param locations the resource locations to resolve resources from
	 * @param chain the chain of resource resolvers
	 * @return the baseUrl updated with a version string
	 */
	String addVersionToUrl(String baseUrl, List<? extends Resource> locations, ResourceResolverChain chain);
}
