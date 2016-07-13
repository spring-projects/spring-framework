/*
 * Copyright 2002-2016 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * A default implementation of {@link ResourceResolverChain} for invoking a list
 * of {@link ResourceResolver}s.
 *
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @since 4.1
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	private final List<ResourceResolver> resolvers = new ArrayList<>();

	private int index = -1;


	public DefaultResourceResolverChain(List<? extends ResourceResolver> resolvers) {
		if (resolvers != null) {
			this.resolvers.addAll(resolvers);
		}
	}


	@Override
	public Resource resolveResource(HttpServletRequest request, String requestPath, List<? extends Resource> locations) {
		ResourceResolver resolver = getNext();
		if (resolver == null) {
			return null;
		}

		try {
			return resolver.resolveResource(request, requestPath, locations, this);
		}
		finally {
			this.index--;
		}
	}

	@Override
	public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		ResourceResolver resolver = getNext();
		if (resolver == null) {
			return null;
		}

		try {
			return resolver.resolveUrlPath(resourcePath, locations, this);
		}
		finally {
			this.index--;
		}
	}

	private ResourceResolver getNext() {
		Assert.state(this.index <= this.resolvers.size(),
				"Current index exceeds the number of configured ResourceResolvers");

		if (this.index == (this.resolvers.size() - 1)) {
			return null;
		}
		this.index++;
		return this.resolvers.get(this.index);
	}

}
