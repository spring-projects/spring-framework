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

import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;


/**
 * 
 * @author Jeremy Grelle
 */
public abstract class AbstractResourceResolver implements ResourceResolver {

	@Override
	public final Resource resolve(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain) {
		
		Resource candidate = chain.next(this).resolve(request, path, locations, chain);
		
		return resolveInternal(request, path, locations, chain, candidate);
	}
	
	protected abstract Resource resolveInternal(HttpServletRequest request, String path,
			List<Resource> locations, ResourceResolverChain chain, Resource resolved);

	@Override
	public String resolveUrl(String resourcePath, List<Resource> locations,
			ResourceResolverChain chain) {
		return chain.next(this).resolveUrl(resourcePath, locations, chain);
	}

}
