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

package org.springframework.web.reactive.handler;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.web.util.pattern.PathPattern;

/**
 * Result of matching an request lookup path against {@link PathPattern} instances.
 *
 * <p>Each result optionally associates the matching {@link PathPattern}
 * with a request handler of type {@code T}.
 *
 * @author Brian Clozel
 * @since 5.0
 * @see PathPatternRegistry
 */
public class PathMatchResult<T> {

	private final PathPattern pattern;

	private final T handler;


	public PathMatchResult(PathPattern pattern, @Nullable T handler) {
		Assert.notNull(pattern, "PathPattern must not be null");
		this.pattern = pattern;
		this.handler = handler;
	}


	/**
	 * Return the {@link PathPattern} that matched the incoming request.
	 */
	public PathPattern getPattern() {
		return this.pattern;
	}

	/**
	 * Return the request handler associated with the {@link PathPattern}.
	 */
	@Nullable
	public T getHandler() {
		return this.handler;
	}

}
