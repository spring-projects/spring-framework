/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.web.reactive.accept;

import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;
import org.springframework.web.accept.InvalidApiVersionException;
import org.springframework.web.server.ServerWebExchange;

/**
 * {@link ApiVersionResolver} that extract the version from a path segment.
 *
 * <p>Note that this resolver will either resolve the version from the specified
 * path segment, or raise an {@link InvalidApiVersionException}, e.g. if there
 * are not enough path segments. It never returns {@code null}, and therefore
 * cannot yield to other resolvers.
 *
 * @author Rossen Stoyanchev
 * @author Martin Mois
 * @since 7.0
 */
public class PathApiVersionResolver implements ApiVersionResolver {

	private final int pathSegmentIndex;
	private @Nullable Predicate<RequestPath> excludePath = null;


	/**
	 * Create a resolver instance.
	 * @param pathSegmentIndex the index of the path segment that contains
	 * the API version
	 */
	public PathApiVersionResolver(int pathSegmentIndex) {
		Assert.isTrue(pathSegmentIndex >= 0, "'pathSegmentIndex' must be >= 0");
		this.pathSegmentIndex = pathSegmentIndex;
	}

	/**
	 * Create a resolver instance.
	 * @param pathSegmentIndex the index of the path segment that contains the API version
	 * @param excludePath a {@link Predicate} that tests if the given path should be excluded
	 */
	public PathApiVersionResolver(int pathSegmentIndex, Predicate<RequestPath> excludePath) {
		this(pathSegmentIndex);
		this.excludePath = excludePath;
	}


	@Override
	public @Nullable String resolveVersion(ServerWebExchange exchange) {
		int i = 0;
		RequestPath path = exchange.getRequest().getPath();
		if (this.excludePath != null && this.excludePath.test(path)) {
			return null;
		}
		for (PathContainer.Element e : path.pathWithinApplication().elements()) {
			if (e instanceof PathContainer.PathSegment && i++ == this.pathSegmentIndex) {
				return e.value();
			}
		}
		throw new InvalidApiVersionException("No path segment at index " + this.pathSegmentIndex);
	}

}
