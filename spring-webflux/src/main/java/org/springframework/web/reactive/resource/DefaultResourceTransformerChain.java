/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.web.server.ServerWebExchange;

/**
 * Default immutable implementation of {@link ResourceTransformerChain}.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
class DefaultResourceTransformerChain implements ResourceTransformerChain {

	private final ResourceResolverChain resolverChain;

	private final @Nullable ResourceTransformer transformer;

	private final @Nullable ResourceTransformerChain nextChain;


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


	@Override
	public ResourceResolverChain getResolverChain() {
		return this.resolverChain;
	}

	@Override
	public Mono<Resource> transform(ServerWebExchange exchange, Resource resource) {
		return (this.transformer != null && this.nextChain != null ?
				this.transformer.transform(exchange, resource, this.nextChain) :
				Mono.just(resource));
	}

}
