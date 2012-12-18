/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.web.servlet.config.annotation;

import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;

/**
 * Encapsulates information required to create a resource handlers.
 *
 * @author Rossen Stoyanchev
 * @author Keith Donald
 *
 * @since 3.1
 */
public class ResourceHandlerRegistration {

	private final ResourceLoader resourceLoader;

	private final String[] pathPatterns;

	private final List<Resource> locations = new ArrayList<Resource>();

	private Integer cachePeriod;

	/**
	 * Create a {@link ResourceHandlerRegistration} instance.
	 * @param resourceLoader a resource loader for turning a String location into a {@link Resource}
	 * @param pathPatterns one or more resource URL path patterns
	 */
	public ResourceHandlerRegistration(ResourceLoader resourceLoader, String... pathPatterns) {
		Assert.notEmpty(pathPatterns, "At least one path pattern is required for resource handling.");
		this.resourceLoader = resourceLoader;
		this.pathPatterns = pathPatterns;
	}

	/**
	 * Add one or more resource locations from which to serve static content. Each location must point to a valid
	 * directory. Multiple locations may be specified as a comma-separated list, and the locations will be checked
	 * for a given resource in the order specified.
	 * <p>For example, {{@code "/"}, {@code "classpath:/META-INF/public-web-resources/"}} allows resources to
	 * be served both from the web application root and from any JAR on the classpath that contains a
	 * {@code /META-INF/public-web-resources/} directory, with resources in the web application root taking precedence.
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 */
	public ResourceHandlerRegistration addResourceLocations(String...resourceLocations) {
		for (String location : resourceLocations) {
			this.locations.add(resourceLoader.getResource(location));
		}
		return this;
	}

	/**
	 * Specify the cache period for the resources served by the resource handler, in seconds. The default is to not
	 * send any cache headers but to rely on last-modified timestamps only. Set to 0 in order to send cache headers
	 * that prevent caching, or to a positive number of seconds to send cache headers with the given max-age value.
	 * @param cachePeriod the time to cache resources in seconds
	 * @return the same {@link ResourceHandlerRegistration} instance for chained method invocation
	 */
	public ResourceHandlerRegistration setCachePeriod(Integer cachePeriod) {
		this.cachePeriod = cachePeriod;
		return this;
	}

	/**
	 * Returns the URL path patterns for the resource handler.
	 */
	protected String[] getPathPatterns() {
		return pathPatterns;
	}

	/**
	 * Returns a {@link ResourceHttpRequestHandler} instance.
	 */
	protected ResourceHttpRequestHandler getRequestHandler() {
		Assert.isTrue(!CollectionUtils.isEmpty(locations), "At least one location is required for resource handling.");
		ResourceHttpRequestHandler requestHandler = new ResourceHttpRequestHandler();
		requestHandler.setLocations(locations);
		if (cachePeriod != null) {
			requestHandler.setCacheSeconds(cachePeriod);
		}
		return requestHandler;
	}

}
