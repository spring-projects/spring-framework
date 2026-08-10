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

package org.springframework.web.accept;

import java.util.function.Predicate;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.http.server.PathContainer;
import org.springframework.http.server.RequestPath;
import org.springframework.util.Assert;
import org.springframework.web.util.ServletRequestPathUtils;


/**
 * {@link ApiVersionResolver} that extract the version from a path segment.
 *
 * <p>If the resolver is created with a path index only, it will always return
 * a version, or raise an {@link InvalidApiVersionException}, but never
 * return {@code null}.
 *
 * <p>The resolver can also be created with an additional
 * {@code Predicate<RequestPath>} that provides more flexibility in deciding
 * whether a given path is versioned or not, possibly resolving to {@code null}.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public class PathApiVersionResolver implements ApiVersionResolver {

	private final int pathSegmentIndex;

	private @Nullable Predicate<RequestPath> versionPathPredicate;


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
	 * Constructor variant of {@link #PathApiVersionResolver(int)} with an
	 * additional {@code Predicate<RequestPath>} to help determine whether
	 * a given path is versioned (true) or not (false).
	 */
	public PathApiVersionResolver(int pathSegmentIndex, Predicate<RequestPath> versionPathPredicate) {
		this(pathSegmentIndex);
		this.versionPathPredicate = versionPathPredicate;
	}

	@Override
	public @Nullable String resolveVersion(HttpServletRequest request) {
		RequestPath path = ServletRequestPathUtils.getParsedRequestPath(request);
		if (!isVersionedPath(path)) {
			return null;
		}
		int i = 0;
		for (PathContainer.Element element : path.pathWithinApplication().elements()) {
			if (element instanceof PathContainer.PathSegment && i++ == this.pathSegmentIndex) {
				return element.value();
			}
		}
		throw new InvalidApiVersionException("No path segment at index " + this.pathSegmentIndex);
	}

	private boolean isVersionedPath(RequestPath path) {
		return (this.versionPathPredicate == null || this.versionPathPredicate.test(path));
	}

}
