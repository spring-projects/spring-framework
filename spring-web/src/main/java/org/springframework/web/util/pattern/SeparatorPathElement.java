/*
 * Copyright 2002-2021 the original author or authors.
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

import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A separator path element. In the pattern '/foo/bar' the two occurrences
 * of '/' will be represented by a SeparatorPathElement (if the default
 * separator of '/' is being used).
 *
 * @author Andy Clement
 * @since 5.0
 */
class SeparatorPathElement extends PathElement {

	SeparatorPathElement(int pos, char separator) {
		super(pos, separator);
	}


	/**
	 * Matching a separator is easy, basically the character at candidateIndex
	 * must be the separator.
	 */
	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		if (pathIndex < matchingContext.pathLength && matchingContext.isSeparator(pathIndex)) {
			if (isNoMorePattern()) {
				if (matchingContext.determineRemainingPath) {
					matchingContext.remainingPathIndex = pathIndex + 1;
					return true;
				}
				else {
					return (pathIndex + 1 == matchingContext.pathLength);
				}
			}
			else {
				pathIndex++;
				return (this.next != null && this.next.matches(pathIndex, matchingContext));
			}
		}
		return false;
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return new char[] {this.separator};
	}


	@Override
	public String toString() {
		return "Separator(" + this.separator + ")";
	}

}
