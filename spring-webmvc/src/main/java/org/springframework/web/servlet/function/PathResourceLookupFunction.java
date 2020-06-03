/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Lookup function used by {@link RouterFunctions#resources(String, Resource)}.
 *
 * @author Arjen Poutsma
 * @since 5.2
 */
class PathResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {

	private final PathPattern pattern;

	private final Resource location;


	public PathResourceLookupFunction(String pattern, Resource location) {
		Assert.hasLength(pattern, "'pattern' must not be empty");
		Assert.notNull(location, "'location' must not be null");
		this.pattern = PathPatternParser.defaultInstance.parse(pattern);
		this.location = location;
	}


	@Override
	public Optional<Resource> apply(ServerRequest request) {
		PathContainer pathContainer = request.pathContainer();
		if (!this.pattern.matches(pathContainer)) {
			return Optional.empty();
		}

		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		String path = processPath(pathContainer.value());
		if (path.contains("%")) {
			path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
		}
		if (!StringUtils.hasLength(path) || isInvalidPath(path)) {
			return Optional.empty();
		}

		try {
			Resource resource = this.location.createRelative(path);
			if (resource.exists() && resource.isReadable() && isResourceUnderLocation(resource)) {
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

	private String processPath(String path) {
		boolean slash = false;
		for (int i = 0; i < path.length(); i++) {
			if (path.charAt(i) == '/') {
				slash = true;
			}
			else if (path.charAt(i) > ' ' && path.charAt(i) != 127) {
				if (i == 0 || (i == 1 && slash)) {
					return path;
				}
				path = slash ? "/" + path.substring(i) : path.substring(i);
				return path;
			}
		}
		return (slash ? "/" : "");
	}

	private boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				return true;
			}
		}
		if (path.contains("..") && StringUtils.cleanPath(path).contains("../")) {
				return true;
			}
		return false;
	}

	private boolean isResourceUnderLocation(Resource resource) throws IOException {
		if (resource.getClass() != this.location.getClass()) {
			return false;
		}

		String resourcePath;
		String locationPath;

		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(this.location.getURL().toString());
		}
		else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) this.location).getPath());
		}
		else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(this.location.getURL().getPath());
		}

		if (locationPath.equals(resourcePath)) {
			return true;
		}
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		if (!resourcePath.startsWith(locationPath)) {
			return false;
		}
		if (resourcePath.contains("%") && StringUtils.uriDecode(resourcePath, StandardCharsets.UTF_8).contains("../")) {
			return false;
		}
		return true;
	}


	@Override
	public String toString() {
		return this.pattern + " -> " + this.location;
	}

}
