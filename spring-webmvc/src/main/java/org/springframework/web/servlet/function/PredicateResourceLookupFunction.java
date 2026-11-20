/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.function;

import java.util.Optional;
import java.util.function.Function;

import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Lookup function used by {@link RouterFunctions#resource(RequestPredicate, Resource)} and
 * {@link RouterFunctions#resource(RequestPredicate, Resource, java.util.function.BiConsumer)}.
 *
 * @author Sebastien Deleuze
 * @since 6.1.4
 */
class PredicateResourceLookupFunction implements Function<ServerRequest, Optional<Resource>> {

	private final RequestPredicate predicate;

	private final Resource resource;

	public PredicateResourceLookupFunction(RequestPredicate predicate, Resource resource) {
		Assert.notNull(predicate, "'predicate' must not be null");
		Assert.notNull(resource, "'resource' must not be null");
		this.predicate = predicate;
		this.resource = resource;
	}

	@Override
	public Optional<Resource> apply(ServerRequest serverRequest) {
		return this.predicate.test(serverRequest) ? Optional.of(this.resource) : Optional.empty();
	}

}
