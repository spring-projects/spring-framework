/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.web.servlet.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default immutable implementation of {@link ResourceResolverChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class DefaultResourceResolverChain implements ResourceResolverChain {

	@Nullable
	private final ResourceResolver resolver;

	@Nullable
	private final ResourceResolverChain nextChain;


	public DefaultResourceResolverChain(@Nullable List<? extends ResourceResolver> resolvers) {
		resolvers = (resolvers != null ? resolvers : Collections.emptyList());
		DefaultResourceResolverChain chain = initChain(new ArrayList<>(resolvers));
		this.resolver = chain.resolver;
		this.nextChain = chain.nextChain;
	}

	private static DefaultResourceResolverChain initChain(ArrayList<? extends ResourceResolver> resolvers) {
		DefaultResourceResolverChain chain = new DefaultResourceResolverChain(null, null);
		ListIterator<? extends ResourceResolver> it = resolvers.listIterator(resolvers.size());
		while (it.hasPrevious()) {
			chain = new DefaultResourceResolverChain(it.previous(), chain);
		}
		return chain;
	}

	private DefaultResourceResolverChain(@Nullable ResourceResolver resolver, @Nullable ResourceResolverChain chain) {
		Assert.isTrue((resolver == null && chain == null) || (resolver != null && chain != null),
				"Both resolver and resolver chain must be null, or neither is");
		this.resolver = resolver;
		this.nextChain = chain;
	}


	@Override
	@Nullable
	public Resource resolveResource(
			@Nullable HttpServletRequest request, String requestPath, List<? extends Resource> locations) {

		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveResource(request, requestPath, locations, this.nextChain) : null);
	}

	@Override
	@Nullable
	public String resolveUrlPath(String resourcePath, List<? extends Resource> locations) {
		return (this.resolver != null && this.nextChain != null ?
				this.resolver.resolveUrlPath(resourcePath, locations, this.nextChain) : null);
	}

}
