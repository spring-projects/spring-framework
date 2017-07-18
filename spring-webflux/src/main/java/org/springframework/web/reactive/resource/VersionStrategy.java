/*
 * Copyright 2002-2017 the original author or authors.
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

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;

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
	@Nullable
	String extractVersion(String requestPath);

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
