/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * A contract for invoking a chain of {@link ResourceTransformer ResourceTransformers} where each resolver
 * is given a reference to the chain allowing it to delegate when necessary.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
public interface ResourceTransformerChain {

	/**
	 * Return the {@code ResourceResolverChain} that was used to resolve the
	 * {@code Resource} being transformed. This may be needed for resolving
	 * related resources, for example, links to other resources.
	 */
	ResourceResolverChain getResolverChain();

	/**
	 * Transform the given resource.
	 * @param exchange the current exchange
	 * @param resource the candidate resource to transform
	 * @return the transformed or the same resource, never empty
	 */
	Mono<Resource> transform(ServerWebExchange exchange, Resource resource);

}
