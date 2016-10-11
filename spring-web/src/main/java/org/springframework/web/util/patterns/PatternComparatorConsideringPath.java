/*
 * Copyright 2016 the original author or authors.
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
package org.springframework.web.util.patterns;

import java.util.Comparator;

/**
 * Similar to {@link PathPatternComparator} but this takes account of a specified path and
 * sorts anything that exactly matches it to be first.
 * 
 * @author Andy Clement
 */
public class PatternComparatorConsideringPath implements Comparator<PathPattern> {

	private String path;
	
	public PatternComparatorConsideringPath(String path) {
		this.path = path;
	}

	@Override
	public int compare(PathPattern o1, PathPattern o2) {
		// Nulls get sorted to the end
		if (o1 == null) {
			return (o2==null?0:+1);
		} else if (o2 == null) {
			return -1;
		}
		if (o1.getPatternString().equals(path)) {
			return (o2.getPatternString().equals(path))?0:-1;
		} else if (o2.getPatternString().equals(path)) {
			return +1;
		}
		return o1.compareTo(o2);
	}

}
