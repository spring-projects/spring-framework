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

package org.springframework.web.util.pattern;

import java.util.Comparator;

import org.springframework.lang.Nullable;

/**
 * {@link PathPattern} comparator that takes account of a specified
 * path and sorts anything that exactly matches it to be first.
 *
 * <p>Patterns that have the same specificity are then compared
 * using their String representation, in order to avoid
 * considering them as "duplicates" when sorting them
 * in {@link java.util.Set} collections.
 *
 * @author Brian Clozel
 * @since 5.0
 */
public class PathPatternComparator implements Comparator<PathPattern> {

	private final String path;


	public PathPatternComparator(String path) {
		this.path = path;
	}


	@Override
	public int compare(@Nullable PathPattern o1, @Nullable PathPattern o2) {
		// Nulls get sorted to the end
		if (o1 == null) {
			return (o2 == null ? 0 : +1);
		}
		else if (o2 == null) {
			return -1;
		}

		// exact matches get sorted first
		if (o1.getPatternString().equals(path)) {
			return (o2.getPatternString().equals(path)) ? 0 : -1;
		}
		else if (o2.getPatternString().equals(path)) {
			return +1;
		}

		// compare pattern specificity
		int result = o1.compareTo(o2);
		// if equal specificity, sort using pattern string
		if (result == 0) {
			return o1.getPatternString().compareTo(o2.getPatternString());
		}
		return result;
	}

}
