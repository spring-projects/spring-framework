/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.function.server;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.function.Function;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.http.server.PathContainer;
import org.springframework.util.Assert;
import org.springframework.web.reactive.resource.ResourceHandlerUtils;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Lookup function used by {@link RouterFunctions#resources(String, Resource)}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 */
class PathResourceLookupFunction implements Function<ServerRequest, Mono<Resource>> {

	private final PathPattern pattern;

	private final Resource location;


	public PathResourceLookupFunction(String pattern, Resource location) {
		Assert.hasLength(pattern, "'pattern' must not be empty");
		ResourceHandlerUtils.assertResourceLocation(location);
		this.pattern = PathPatternParser.defaultInstance.parse(pattern);
		this.location = location;
	}

	@Override
	public Mono<Resource> apply(ServerRequest request) {
		PathContainer pathContainer = request.requestPath().pathWithinApplication();
		if (!this.pattern.matches(pathContainer)) {
			return Mono.empty();
		}

		pathContainer = this.pattern.extractPathWithinPattern(pathContainer);
		String path = ResourceHandlerUtils.normalizeInputPath(pathContainer.value());
		if (ResourceHandlerUtils.shouldIgnoreInputPath(path)) {
			return Mono.empty();
		}

		try {
			Resource resource = ResourceHandlerUtils.createRelativeResource(this.location, path);
			if (resource.isReadable() && ResourceHandlerUtils.isResourceUnderLocation(this.location, resource)) {
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

	@Override
	public String toString() {
		return this.pattern + " -> " + this.location;
	}

}
