/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.core.annotation;

import java.util.Arrays;

import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link AnnotationFilter} implementation used for
 * {@link AnnotationFilter#packages(String...)}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class PackagesAnnotationFilter implements AnnotationFilter {

	private final String[] prefixes;

	private final int hashCode;


	PackagesAnnotationFilter(String... packages) {
		Assert.notNull(packages, "Packages array must not be null");
		this.prefixes = new String[packages.length];
		for (int i = 0; i < packages.length; i++) {
			String pkg = packages[i];
			Assert.hasText(pkg, "Packages array must not have empty elements");
			this.prefixes[i] = pkg + ".";
		}
		Arrays.sort(this.prefixes);
		this.hashCode = Arrays.hashCode(this.prefixes);
	}


	@Override
	public boolean matches(String annotationType) {
		for (String prefix : this.prefixes) {
			if (annotationType.startsWith(prefix)) {
				return true;
			}
		}
		return false;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		if (this == other) {
			return true;
		}
		if (other == null || getClass() != other.getClass()) {
			return false;
		}
		return Arrays.equals(this.prefixes, ((PackagesAnnotationFilter) other).prefixes);
	}

	@Override
	public int hashCode() {
		return this.hashCode;
	}

	@Override
	public String toString() {
		return "Packages annotation filter: " +
				StringUtils.arrayToCommaDelimitedString(this.prefixes);
	}

}
