/*
 * Copyright 2002-2025 the original author or authors.
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

package org.springframework.web.accept;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

/**
 * The main component that encapsulates configuration preferences and strategies
 * to manage API versioning for an application.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface ApiVersionStrategy {

	/**
	 * Resolve the version value from a request, e.g. from a request header.
	 * @param request the current request
	 * @return the version, if present or {@code null}
	 */
	@Nullable
	String resolveVersion(HttpServletRequest request);

	/**
	 * Parse the version of a request into an Object.
	 * @param version the value to parse
	 * @return an Object that represents the version
	 */
	Comparable<?> parseVersion(String version);

	/**
	 * Validate a request version, including required and supported version checks.
	 * @param requestVersion the version to validate
	 * @param request the request
	 * @throws MissingApiVersionException if the version is required, but not specified
	 * @throws InvalidApiVersionException if the version is not supported
	 */
	void validateVersion(@Nullable Comparable<?> requestVersion, HttpServletRequest request)
			throws MissingApiVersionException, InvalidApiVersionException;

	/**
	 * Return a default version to use for requests that don't specify one.
	 */
	@Nullable Comparable<?> getDefaultVersion();

}
