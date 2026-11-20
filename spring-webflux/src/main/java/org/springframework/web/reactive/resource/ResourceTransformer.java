/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.reactive.resource;

import reactor.core.publisher.Mono;

import org.springframework.core.io.Resource;
import org.springframework.web.server.ServerWebExchange;

/**
 * An abstraction for transforming the content of a resource.
 *
 * @author Rossen Stoyanchev
 * @since 5.0
 */
@FunctionalInterface
public interface ResourceTransformer {

	/**
	 * Transform the given resource.
	 * @param exchange the current exchange
	 * @param resource the resource to transform
	 * @param transformerChain the chain of remaining transformers to delegate to
	 * @return the transformed resource (never empty)
	 */
	Mono<Resource> transform(ServerWebExchange exchange, Resource resource,
			ResourceTransformerChain transformerChain);

}
