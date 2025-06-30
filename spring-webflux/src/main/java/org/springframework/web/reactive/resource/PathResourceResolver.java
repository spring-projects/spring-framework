/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.resource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.core.log.LogFormatUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;

/**
 * A simple {@code ResourceResolver} that tries to find a resource under the given
 * locations matching to the request path.
 *
 * <p>This resolver does not delegate to the {@code ResourceResolverChain} and is
 * expected to be configured at the end in a chain of resolvers.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 5.0
 */
public class PathResourceResolver extends AbstractResourceResolver {

	private Resource @Nullable [] allowedLocations;


	/**
	 * By default, when a Resource is found, the path of the resolved resource is
	 * compared to ensure it's under the input location where it was found.
	 * However sometimes that may not be the case, for example, when
	 * {@link CssLinkResourceTransformer}
	 * resolves public URLs of links it contains, the CSS file is the location
	 * and the resources being resolved are css files, images, fonts and others
	 * located in adjacent or parent directories.
	 * <p>This property allows configuring a complete list of locations under
	 * which resources must be so that if a resource is not under the location
	 * relative to which it was found, this list may be checked as well.
	 * <p>By default {@link ResourceWebHandler} initializes this property
	 * to match its list of locations.
	 * @param locations the list of allowed locations
	 */
	public void setAllowedLocations(Resource @Nullable ... locations) {
		this.allowedLocations = locations;
	}

	public Resource @Nullable [] getAllowedLocations() {
		return this.allowedLocations;
	}


	@Override
	protected Mono<Resource> resolveResourceInternal(@Nullable ServerWebExchange exchange,
			String requestPath, List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, locations);
	}

	@Override
	protected Mono<String> resolveUrlPathInternal(String path, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		if (StringUtils.hasText(path)) {
			return getResource(path, locations).map(resource -> path);
		}
		else {
			return Mono.empty();
		}
	}

	private Mono<Resource> getResource(String resourcePath, List<? extends Resource> locations) {
		return Flux.fromIterable(locations)
				.concatMap(location -> getResource(resourcePath, location))
				.next();
	}

	/**
	 * Find the resource under the given location.
	 * <p>The default implementation checks if there is a readable
	 * {@code Resource} for the given path relative to the location.
	 * @param resourcePath the path to the resource
	 * @param location the location to check
	 * @return the resource, or empty {@link Mono} if none found
	 */
	protected Mono<Resource> getResource(String resourcePath, Resource location) {
		try {
			Resource resource = ResourceHandlerUtils.createRelativeResource(location, resourcePath);
			if (resource.isReadable()) {
				if (checkResource(resource, location)) {
					return Mono.just(resource);
				}
				else if (logger.isWarnEnabled()) {
					Resource[] allowed = getAllowedLocations();
					logger.warn(LogFormatUtils.formatValue(
							"Resource path \"" + resourcePath + "\" was successfully resolved " +
									"but resource \"" + resource + "\" is neither under the " +
									"current location \"" + location + "\" nor under any of the " +
									"allowed locations " + (allowed != null ? Arrays.asList(allowed) : "[]"), -1, true));
				}
			}
			return Mono.empty();
		}
		catch (IOException ex) {
			if (logger.isDebugEnabled()) {
				String error = "Skip location [" + location + "] due to error";
				if (logger.isTraceEnabled()) {
					logger.trace(error, ex);
				}
				else {
					logger.debug(error + ": " + ex.getMessage());
				}
			}
			return Mono.error(ex);
		}
	}

	/**
	 * Perform additional checks on a resolved resource beyond checking whether the
	 * resource exists and is readable. The default implementation also verifies
	 * the resource is either under the location relative to which it was found or
	 * is under one of the {@link #setAllowedLocations allowed locations}.
	 * @param resource the resource to check
	 * @param location the location relative to which the resource was found
	 * @return "true" if resource is in a valid location, "false" otherwise
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (ResourceHandlerUtils.isResourceUnderLocation(location, resource)) {
			return true;
		}
		if (getAllowedLocations() != null) {
			for (Resource current : getAllowedLocations()) {
				if (ResourceHandlerUtils.isResourceUnderLocation(current, resource)) {
					return true;
				}
			}
		}
		return false;
	}

}
