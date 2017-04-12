/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.reactive.function.server;

import java.util.Optional;

import org.springframework.util.Assert;

/**
 * Represents a function that evaluates on a given {@link ServerRequest}.
 * Instances of this function that evaluate on common request properties can be found in {@link RequestPredicates}.
 *
 * @author Arjen Poutsma
 * @since 5.0
 * @see RequestPredicates
 * @see RouterFunctions#route(RequestPredicate, HandlerFunction)
 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
 */
@FunctionalInterface
public interface RequestPredicate {

	/**
	 * Evaluate this predicate on the given request.
	 * @param request the request to match against
	 * @return {@code true} if the request matches the predicate; {@code false} otherwise
	 */
	boolean test(ServerRequest request);

	/**
	 * Return a composed request predicate that tests against both this predicate AND
	 * the {@code other} predicate. When evaluating the composed predicate, if this
	 * predicate is {@code false}, then the {@code other} predicate is not evaluated.
	 * @param other a predicate that will be logically-ANDed with this predicate
	 * @return a predicate composed of this predicate AND the {@code other} predicate
	 */
	default RequestPredicate and(RequestPredicate other) {
		Assert.notNull(other, "'other' must not be null");
		return new RequestPredicates.AndRequestPredicate(this, other);
	}

	/**
	 * Return a predicate that represents the logical negation of this predicate.
	 * @return a predicate that represents the logical negation of this predicate
	 */
	default RequestPredicate negate() {
		return (t) -> !test(t);
	}

	/**
	 * Return a composed request predicate that tests against both this predicate OR
	 * the {@code other} predicate. When evaluating the composed predicate, if this
	 * predicate is {@code true}, then the {@code other} predicate is not evaluated.
	 * @param other a predicate that will be logically-ORed with this predicate
	 * @return a predicate composed of this predicate OR the {@code other} predicate
	 */
	default RequestPredicate or(RequestPredicate other) {
		Assert.notNull(other, "'other' must not be null");
		return new RequestPredicates.OrRequestPredicate(this, other);
	}

	/**
	 * Transform the given request into a request used for a nested route. For instance,
	 * a path-based predicate can return a {@code ServerRequest} with a the path remaining after a
	 * match.
	 * <p>The default implementation returns an {@code Optional} wrapping the given path if
	 * {@link #test(ServerRequest)} evaluates to {@code true}; or {@link Optional#empty()} if it
	 * evaluates to {@code false}.
	 * @param request the request to be nested
	 * @return the nested request
	 * @see RouterFunctions#nest(RequestPredicate, RouterFunction)
	 */
	default Optional<ServerRequest> nest(ServerRequest request) {
		return test(request) ? Optional.of(request) : Optional.empty();
	}

}
