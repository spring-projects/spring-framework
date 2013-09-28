/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.springframework.core.io.Resource;
import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.handler.SimpleUrlHandlerMapping;


/**
 * A helper class for generating the URL for a resource. Given knowledge of all configured
 * resource handler mappings (see {@link #setResourceHandlerMappings(List)}), it can
 * determine whether a given path is a path to a resource, as well as what URL should be
 * sent to the client to access that resource. This is essentially the reverse of
 * resolving an incoming request URL to a resource.
 *
 * @author Rossen Stoyanchev
 * @since 4.0
 */
public class ResourceUrlGenerator {

	private final List<ResourceMapping> resourceMappings = new ArrayList<ResourceMapping>();


	/**
	 * Configure this instance with the handler mappings used to serve resources. It is
	 * expected that the handler mapping URL map contains handlers of type
	 * {@link ResourceHttpRequestHandler}.
	 *
	 * @param handlerMappings resource handler mappings
	 */
	public void setResourceHandlerMappings(List<SimpleUrlHandlerMapping> handlerMappings) {
		this.resourceMappings.clear();
		if (handlerMappings == null) {
			return;
		}
		for (SimpleUrlHandlerMapping handlerMapping : handlerMappings) {
			PathMatcher pathMatcher = handlerMapping.getPathMatcher();

			for(Entry<String, ?> entry : handlerMapping.getUrlMap().entrySet()) {
				Object value = entry.getValue();
				if (value instanceof ResourceHttpRequestHandler) {
					ResourceHttpRequestHandler handler = (ResourceHttpRequestHandler) value;

					String pattern = entry.getKey();
					List<ResourceResolver> resolvers = handler.getResourceResolvers();
					List<Resource> locations = handler.getLocations();
					this.resourceMappings.add(new ResourceMapping(pattern, pathMatcher, resolvers, locations));
				}
			}
		}
	}

	/**
	 * Resolve the given resource path to a URL path. This is useful when rendering URL
	 * links to clients to determine the actual URL to use.
	 *
	 * @param candidatePath the resource path to resolve
	 *
	 * @return the resolved URL path or {@code null} if the given path does not match to
	 *         any resource or otherwise could not be resolved to a resource URL path
	 */
	public String getResourceUrl(String candidatePath) {
		for (ResourceMapping mapping : this.resourceMappings) {
			String url = mapping.getUrlForResource(candidatePath);
			if (url != null) {
				return url;
			}
		}
		return null;
	}


	private static class ResourceMapping {

		private final String pattern;

		private final PathMatcher pathMatcher;

		private final List<ResourceResolver> resolvers;

		private final List<Resource> locations;


		public ResourceMapping(String pattern, PathMatcher pathMatcher,
				List<ResourceResolver> resolvers, List<Resource> locations) {

			this.pattern = pattern;
			this.pathMatcher = pathMatcher;
			this.resolvers = resolvers;
			this.locations = locations;
		}

		public String getUrlForResource(String candidatePath) {

			if (this.pathMatcher.match(this.pattern, candidatePath)) {

				String pathWithinMapping = this.pathMatcher.extractPathWithinPattern(this.pattern, candidatePath);
				String pathMapping = candidatePath.replace(pathWithinMapping, "");

				DefaultResourceResolverChain chain = new DefaultResourceResolverChain(this.resolvers);
				String url = chain.resolveUrlPath(pathWithinMapping, this.locations);
				if (url != null) {
					return pathMapping + url;
				}
			}
			return null;
		}
	}

}
