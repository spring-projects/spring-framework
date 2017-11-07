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

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.ResourceUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Lookup function used by {@link RouterFunctions#resources(String, Resource)}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class PathResourceLookupFunction implements Function<ServerRequest, Mono<Resource>> {

	private static final PathPatternParser PATTERN_PARSER = new PathPatternParser();

	private final PathPattern pattern;

	private final Resource location;


	public PathResourceLookupFunction(String pattern, Resource location) {
		this.pattern = PATTERN_PARSER.parse(pattern);
		this.location = location;
	}


	@Override
	public Mono<Resource> apply(ServerRequest request) {
		PathContainer pathContainer = request.pathContainer();
		if (!this.pattern.matches(pathContainer)) {
			return Mono.empty();
		}
		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		String path = processPath(pathContainer.value());
		if (path.contains("%")) {
			path = StringUtils.uriDecode(path, StandardCharsets.UTF_8);
		}
		if (!StringUtils.hasLength(path) || isInvalidPath(path)) {
			return Mono.empty();
		}
		try {
			Resource resource = this.location.createRelative(path);
			if (resource.exists() && resource.isReadable() && isResourceUnderLocation(resource)) {
				return Mono.just(resource);
			}
			else {
				return Mono.empty();
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	private static String processPath(String path) {
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

	private static boolean isInvalidPath(String path) {
		if (path.contains("WEB-INF") || path.contains("META-INF")) {
			return true;
		}
		if (path.contains(":/")) {
			String relativePath = (path.charAt(0) == '/' ? path.substring(1) : path);
			if (ResourceUtils.isUrl(relativePath) || relativePath.startsWith("url:")) {
				return true;
			}
		}
		if (path.contains("")) {
			path = StringUtils.cleanPath(path);
			if (path.contains("../")) {
				return true;
			}
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
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath :
				locationPath + "/");
		if (!resourcePath.startsWith(locationPath)) {
			return false;
		}

		if (resourcePath.contains("%")) {
			if (StringUtils.uriDecode(resourcePath, StandardCharsets.UTF_8).contains("../")) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return String.format("%s -> %s", this.pattern, this.location);
	}
}
