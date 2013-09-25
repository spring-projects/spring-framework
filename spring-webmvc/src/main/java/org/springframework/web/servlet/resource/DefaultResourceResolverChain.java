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

import org.springframework.core.io.Resource;


/**
 *
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @since 4.0
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	private final List<ResourceResolver> resolvers;

	private List<ResourceTransformer> transformers = new ArrayList<ResourceTransformer>();


	public DefaultResourceResolverChain(List<ResourceResolver> resolvers, List<ResourceTransformer> transformers) {
		this.resolvers = (resolvers != null) ? resolvers : new ArrayList<ResourceResolver>();
		this.transformers = (transformers != null) ? transformers : new ArrayList<ResourceTransformer>();
	}


	@Override
	public ResourceResolver next(ResourceResolver current) {
		return this.resolvers.get(this.resolvers.indexOf(current) + 1);
	}

	@Override
	public Resource resolveAndTransform(HttpServletRequest request, String path, List<Resource> locations)
			throws IOException {

		Resource resource = this.resolvers.get(0).resolve(request, path, locations, this);
		return resource != null ? applyTransformers(request, resource) : resource;
	}

	@Override
	public String resolveUrl(String resourcePath, List<Resource> locations) {
		return this.resolvers.get(0).resolveUrl(resourcePath, locations, this);
	}

	private Resource applyTransformers(HttpServletRequest request, Resource resource) throws IOException {
		for (ResourceTransformer transformer : this.transformers) {
			if (transformer.handles(request, resource)) {
				return applyTransformers(request, transformer.transform(resource));
			}
		}
		return resource;
	}

}
