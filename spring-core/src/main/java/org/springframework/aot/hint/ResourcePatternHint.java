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

package org.springframework.aot.hint;

import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.util.AntPathMatcher;
import org.springframework.util.Assert;

/**
 * A hint that describes resources that should be made available at runtime.
 *
 * <p>Each pattern may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special
 * {@code *} character to indicate a wildcard match. For example:
 * <ul>
 *     <li>"file.properties": matches just the {@code file.properties}
 *         file at the root of the classpath.</li>
 *     <li>"com/example/file.properties": matches just the
 *         {@code file.properties} file in {@code com/example/}.</li>
 *     <li>"*.properties": matches all the files with a {@code .properties}
 *         extension at the root of the classpath.</li>
 *     <li>"com/example/*.properties": matches all the files with a {@code .properties}
 *         extension in {@code com/example/}.</li>
 *     <li>"com/example/{@literal **}": matches all the files in {@code com/example/}
 *         and its child directories at any depth.</li>
 *     <li>"com/example/{@literal **}/*.properties": matches all the files with a {@code .properties}
 *         extension in {@code com/example/} and its child directories at any depth.</li>
 * </ul>
 *
 * <p>A resource pattern must not start with a slash ({@code /}) unless it is the
 * root directory.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @author Sebastien Deleuze
 * @author Sam Brannen
 * @since 6.0
 */
public final class ResourcePatternHint implements ConditionalHint {

	private static final AntPathMatcher PATH_MATCHER = new AntPathMatcher();

	private final String pattern;

	private final @Nullable TypeReference reachableType;


	ResourcePatternHint(String pattern, @Nullable TypeReference reachableType) {
		Assert.isTrue(("/".equals(pattern) || !pattern.startsWith("/")),
				() -> "Resource pattern [%s] must not start with a '/' unless it is the root directory"
						.formatted(pattern));
		this.pattern = pattern;
		this.reachableType = reachableType;
	}


	/**
	 * Return the pattern to use for identifying the resources to match.
	 */
	public String getPattern() {
		return this.pattern;
	}

	/**
	 * Whether the given path matches the current glob pattern.
	 * @param path the path to match against
	 */
	public boolean matches(String path) {
		return PATH_MATCHER.match(this.pattern, path);
	}

	@Override
	public @Nullable TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ResourcePatternHint that &&
				this.pattern.equals(that.pattern) && Objects.equals(this.reachableType, that.reachableType)));
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pattern, this.reachableType);
	}

}
