/*
 * Copyright 2002-2022 the original author or authors.
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

import org.springframework.lang.Nullable;

/**
 * A hint that describes resources that should be made available at runtime.
 * <p>The patterns may be a simple path which has a one-to-one mapping to a
 * resource on the classpath, or alternatively may contain the special
 * {@code *} character to indicate a wildcard search.
 *
 * @author Stephane Nicoll
 * @author Brian Clozel
 * @since 6.0
 */
public final class ResourcePatternHint implements ConditionalHint {

	private final String pattern;

	@Nullable
	private final TypeReference reachableType;

	ResourcePatternHint(String pattern, @Nullable TypeReference reachableType) {
		this.pattern = pattern;
		this.reachableType = reachableType;
	}

	/**
	 * Return the pattern to use for identifying the resources to match.
	 * @return the patterns
	 */
	public String getPattern() {
		return this.pattern;
	}

	@Nullable
	@Override
	public TypeReference getReachableType() {
		return this.reachableType;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		ResourcePatternHint that = (ResourcePatternHint) o;
		return this.pattern.equals(that.pattern)
				&& Objects.equals(this.reachableType, that.reachableType);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.pattern, this.reachableType);
	}
}
