/*
 * Copyright 2002-2015 the original author or authors.
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

import java.io.IOException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.util.StringUtils;
import org.springframework.web.context.support.ServletContextResource;

/**
 * A simple {@code ResourceResolver} that tries to find a resource under the given
 * locations matching to the request path.
 *
 * <p>This resolver does not delegate to the {@code ResourceResolverChain} and is
 * expected to be configured at the end in a chain of resolvers.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
public class PathResourceResolver extends AbstractResourceResolver {

	private Resource[] allowedLocations;


	/**
	 * By default when a Resource is found, the path of the resolved resource is
	 * compared to ensure it's under the input location where it was found.
	 * However sometimes that may not be the case, e.g. when
	 * {@link org.springframework.web.servlet.resource.CssLinkResourceTransformer}
	 * resolves public URLs of links it contains, the CSS file is the location
	 * and the resources being resolved are css files, images, fonts and others
	 * located in adjacent or parent directories.
	 * <p>This property allows configuring a complete list of locations under
	 * which resources must be so that if a resource is not under the location
	 * relative to which it was found, this list may be checked as well.
	 * <p>By default {@link ResourceHttpRequestHandler} initializes this property
	 * to match its list of locations.
	 * @param locations the list of allowed locations
	 * @since 4.1.2
	 * @see ResourceHttpRequestHandler#initAllowedLocations()
	 */
	public void setAllowedLocations(Resource... locations) {
		this.allowedLocations = locations;
	}

	public Resource[] getAllowedLocations() {
		return this.allowedLocations;
	}


	@Override
	protected Resource resolveResourceInternal(HttpServletRequest request, String requestPath,
			List<? extends Resource> locations, ResourceResolverChain chain) {

		return getResource(requestPath, locations);
	}

	@Override
	protected String resolveUrlPathInternal(String resourcePath, List<? extends Resource> locations,
			ResourceResolverChain chain) {

		return (StringUtils.hasText(resourcePath) && getResource(resourcePath, locations) != null ? resourcePath : null);
	}

	private Resource getResource(String resourcePath, List<? extends Resource> locations) {
		for (Resource location : locations) {
			try {
				if (logger.isTraceEnabled()) {
					logger.trace("Checking location: " + location);
				}
				Resource resource = getResource(resourcePath, location);
				if (resource != null) {
					if (logger.isTraceEnabled()) {
						logger.trace("Found match: " + resource);
					}
					return resource;
				}
				else if (logger.isTraceEnabled()) {
					logger.trace("No match for location: " + location);
				}
			}
			catch (IOException ex) {
				logger.trace("Failure checking for relative resource - trying next location", ex);
			}
		}
		return null;
	}

	/**
	 * Find the resource under the given location.
	 * <p>The default implementation checks if there is a readable
	 * {@code Resource} for the given path relative to the location.
	 * @param resourcePath the path to the resource
	 * @param location the location to check
	 * @return the resource, or {@code null} if none found
	 */
	protected Resource getResource(String resourcePath, Resource location) throws IOException {
		Resource resource = location.createRelative(resourcePath);
		if (resource.exists() && resource.isReadable()) {
			if (checkResource(resource, location)) {
				return resource;
			}
			else if (logger.isTraceEnabled()) {
				logger.trace("Resource path=\"" + resourcePath + "\" was successfully resolved " +
						"but resource=\"" +	resource.getURL() + "\" is neither under the " +
						"current location=\"" + location.getURL() + "\" nor under any of the " +
						"allowed locations=" + Arrays.asList(getAllowedLocations()));
			}
		}
		return null;
	}

	/**
	 * Perform additional checks on a resolved resource beyond checking whether the
	 * resources exists and is readable. The default implementation also verifies
	 * the resource is either under the location relative to which it was found or
	 * is under one of the {@link #setAllowedLocations allowed locations}.
	 * @param resource the resource to check
	 * @param location the location relative to which the resource was found
	 * @return "true" if resource is in a valid location, "false" otherwise.
	 * @since 4.1.2
	 */
	protected boolean checkResource(Resource resource, Resource location) throws IOException {
		if (isResourceUnderLocation(resource, location)) {
			return true;
		}
		if (getAllowedLocations() != null) {
			for (Resource current : getAllowedLocations()) {
				if (isResourceUnderLocation(resource, current)) {
					return true;
				}
			}
		}
		return false;
	}

	private boolean isResourceUnderLocation(Resource resource, Resource location) throws IOException {
		if (!resource.getClass().equals(location.getClass())) {
			return false;
		}
		String resourcePath;
		String locationPath;
		if (resource instanceof UrlResource) {
			resourcePath = resource.getURL().toExternalForm();
			locationPath = StringUtils.cleanPath(location.getURL().toString());
		}
		else if (resource instanceof ClassPathResource) {
			resourcePath = ((ClassPathResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ClassPathResource) location).getPath());
		}
		else if (resource instanceof ServletContextResource) {
			resourcePath = ((ServletContextResource) resource).getPath();
			locationPath = StringUtils.cleanPath(((ServletContextResource) location).getPath());
		}
		else {
			resourcePath = resource.getURL().getPath();
			locationPath = StringUtils.cleanPath(location.getURL().getPath());
		}
		if(locationPath.equals(resourcePath)) {
			return true;
		}
		locationPath = (locationPath.endsWith("/") || locationPath.isEmpty() ? locationPath : locationPath + "/");
		if (!resourcePath.startsWith(locationPath)) {
			return false;
		}
		if (resourcePath.contains("%")) {
			// Use URLDecoder (vs UriUtils) to preserve potentially decoded UTF-8 chars...
			if (URLDecoder.decode(resourcePath, "UTF-8").contains("../")) {
				if (logger.isTraceEnabled()) {
					logger.trace("Resolved resource path contains \"../\" after decoding: " + resourcePath);
				}
				return false;
			}
		}
		return true;
	}

}
