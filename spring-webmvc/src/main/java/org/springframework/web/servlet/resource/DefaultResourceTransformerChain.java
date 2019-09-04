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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import javax.servlet.http.HttpServletRequest;

import org.springframework.core.io.Resource;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Default immutable implementation of {@link ResourceTransformerChain}.
 *
 * @author Rossen Stoyanchev
 * @since 4.1
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

	private final ResourceResolverChain resolverChain;

	@Nullable
	private final ResourceTransformer transformer;

	@Nullable
	private final ResourceTransformerChain nextChain;


	public DefaultResourceTransformerChain(
			ResourceResolverChain resolverChain, @Nullable List<ResourceTransformer> transformers) {

		Assert.notNull(resolverChain, "ResourceResolverChain is required");
		this.resolverChain = resolverChain;
		transformers = (transformers != null ? transformers : Collections.emptyList());
		DefaultResourceTransformerChain chain = initTransformerChain(resolverChain, new ArrayList<>(transformers));
		this.transformer = chain.transformer;
		this.nextChain = chain.nextChain;
	}

	private DefaultResourceTransformerChain initTransformerChain(ResourceResolverChain resolverChain,
			ArrayList<ResourceTransformer> transformers) {

		DefaultResourceTransformerChain chain = new DefaultResourceTransformerChain(resolverChain, null, null);
		ListIterator<? extends ResourceTransformer> it = transformers.listIterator(transformers.size());
		while (it.hasPrevious()) {
			chain = new DefaultResourceTransformerChain(resolverChain, it.previous(), chain);
		}
		return chain;
	}

	public DefaultResourceTransformerChain(ResourceResolverChain resolverChain,
			@Nullable ResourceTransformer transformer, @Nullable ResourceTransformerChain chain) {

		Assert.isTrue((transformer == null && chain == null) || (transformer != null && chain != null),
				"Both transformer and transformer chain must be null, or neither is");

		this.resolverChain = resolverChain;
		this.transformer = transformer;
		this.nextChain = chain;
	}


	public ResourceResolverChain getResolverChain() {
		return this.resolverChain;
	}

	@Override
	public Resource transform(HttpServletRequest request, Resource resource) throws IOException {
		return (this.transformer != null && this.nextChain != null ?
				this.transformer.transform(request, resource, this.nextChain) : resource);
	}

}
