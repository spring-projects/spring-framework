/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.web.servlet.function;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.servlet.resource.ResourceHandlerUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Lookup function used by {@link RouterFunctions#resources(String, Resource)}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @since 5.2
 */
class PathResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {

	private final PathPattern pattern;

	private final Resource location;


	public PathResourceLookupFunction(String pattern, Resource location) {
		Assert.hasLength(pattern, "'pattern' must not be empty");
		ResourceHandlerUtils.assertResourceLocation(location);
		this.pattern = PathPatternParser.defaultInstance.parse(pattern);
		this.location = location;
	}


	@Override
	public Optional<Resource> apply(ServerRequest request) {
		PathContainer pathContainer = request.requestPath().pathWithinApplication();
		if (!this.pattern.matches(pathContainer)) {
			return Optional.empty();
		}

		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		String path = processPath(pathContainer.value());
		if (ResourceHandlerUtils.shouldIgnoreInputPath(path)) {
			return Optional.empty();
		}

		if (!(this.location instanceof UrlResource)) {
			path = UriUtils.decode(path, StandardCharsets.UTF_8);
		}

		try {
			Resource resource = this.location.createRelative(path);
			if (resource.isReadable() && ResourceHandlerUtils.isResourceUnderLocation(this.location, resource)) {
				return Optional.of(resource);
			}
			else {
				return Optional.empty();
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Process the given resource path.
	 * <p>By default, this method delegates to {@link ResourceHandlerUtils#normalizeInputPath}.
	 */
	protected String processPath(String path) {
		return ResourceHandlerUtils.normalizeInputPath(path);
	}

	@Override
	public String toString() {
		return this.pattern + " -> " + this.location;
	}

}
