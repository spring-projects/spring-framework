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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;


/**
 * 
 * @author Jeremy Grelle
 */
public class DefaultResourceResolverChain implements ResourceResolverChain{
	
	private static final ResourceResolver DEFAULT_RESOLVER = new PathMappingResourceResolver();
	
	private final List<ResourceResolver> resolvers;
	
	private List<ResourceTransformer> transformers = new ArrayList<ResourceTransformer>();
	
	public DefaultResourceResolverChain(List<ResourceResolver> resolvers, List<ResourceTransformer> transformers) {
		this.resolvers = resolvers;
		this.resolvers.add(DEFAULT_RESOLVER);
		this.transformers = transformers;
	}

	@Override
	public ResourceResolver next(ResourceResolver current) {
		return this.resolvers.get(this.resolvers.indexOf(current) + 1);
	}

	@Override
	public Resource resolveAndTransform(HttpServletRequest request, String path,
			List<Resource> locations) throws IOException{
		Resource resolved = this.resolvers.get(0).resolve(request, path, locations, this);
		return resolved != null ? applyTransformers(request, resolved) : resolved;
	}
	
	@Override
	public String resolveUrl(String resourcePath, List<Resource> locations) {
		return this.resolvers.get(0).resolveUrl(resourcePath, locations, this);
	}
	
	protected Resource applyTransformers(HttpServletRequest request, Resource resource) throws IOException{
		for (ResourceTransformer transformer : transformers) {
			if (transformer.handles(request, resource)) {
				return applyTransformers(request, transformer.transform(resource));
			}
		}
		return resource;
	}
	
	private static class PathMappingResourceResolver implements ResourceResolver {
		
		private static final Log logger = LogFactory.getLog(PathMappingResourceResolver.class);
		
		@Override
		public Resource resolve(HttpServletRequest request, String path, List<Resource> locations, ResourceResolverChain chain) {
			for (Resource location : locations) {
				try {
					if (logger.isDebugEnabled()) {
						logger.debug("Trying relative path [" + path + "] against base location: " + location);
					}
					Resource resource = location.createRelative(path);
					if (resource.exists() && resource.isReadable()) {
						if (logger.isDebugEnabled()) {
							logger.debug("Found matching resource: " + resource);
						}
						return resource;
					}
					else if (logger.isTraceEnabled()) {
						logger.trace("Relative resource doesn't exist or isn't readable: " + resource);
					}
				}
				catch (IOException ex) {
					logger.debug("Failed to create relative resource - trying next resource location", ex);
				}
			}
			return null;
		}

		@Override
		public String resolveUrl(String resourcePath, List<Resource> locations, ResourceResolverChain chain) {
			if (resolve(null, resourcePath, locations, chain) != null) {
				return resourcePath;
			}
			return null;
		}
	}
}
