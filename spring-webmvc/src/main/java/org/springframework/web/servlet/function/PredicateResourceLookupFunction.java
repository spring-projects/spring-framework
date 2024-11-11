/*
 * Copyright 2002-2024 the original author or authors.
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
