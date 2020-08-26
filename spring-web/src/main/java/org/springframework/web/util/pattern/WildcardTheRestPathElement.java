/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.web.util.pattern;

/**
 * A path element representing wildcarding the rest of a path. In the pattern
 * '/foo/**' the /** is represented as a {@link WildcardTheRestPathElement}.
 *
 * @author Andy Clement
 * @since 5.0
 */
class WildcardTheRestPathElement extends PathElement {

	WildcardTheRestPathElement(int pos, char separator) {
		super(pos, separator);
	}


	@Override
	public boolean matches(int pathIndex, PathPattern.MatchingContext matchingContext) {
		// If there is more data, it must start with the separator
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		return true;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public int getWildcardCount() {
		return 1;
	}


	@Override
	public String toString() {
		return "WildcardTheRest(" + this.separator + "**)";
	}

	@Override
	public char[] getChars() {
		return (this.separator+"**").toCharArray();
	}
}
