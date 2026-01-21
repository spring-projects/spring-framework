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

import org.springframework.http.server.PathContainer;
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
 * @since 7.0
 */
public class PathApiVersionResolver implements ApiVersionResolver {

	private final int pathSegmentIndex;


	/**
	 * Create a resolver instance.
	 * @param pathSegmentIndex the index of the path segment that contains
	 * the API version
	 */
	public PathApiVersionResolver(int pathSegmentIndex) {
		Assert.isTrue(pathSegmentIndex >= 0, "'pathSegmentIndex' must be >= 0");
		this.pathSegmentIndex = pathSegmentIndex;
	}


	@Override
	public String resolveVersion(ServerWebExchange exchange) {
		int i = 0;
		for (PathContainer.Element e : exchange.getRequest().getPath().pathWithinApplication().elements()) {
			if (e instanceof PathContainer.PathSegment && i++ == this.pathSegmentIndex) {
				return e.value();
			}
		}
		throw new InvalidApiVersionException("No path segment at index " + this.pathSegmentIndex);
	}

}
