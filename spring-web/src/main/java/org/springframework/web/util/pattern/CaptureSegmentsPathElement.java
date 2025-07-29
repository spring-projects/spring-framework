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

package org.springframework.web.util.pattern;

import java.util.List;

import org.springframework.http.server.PathContainer.Element;
import org.springframework.http.server.PathContainer.PathSegment;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.pattern.PathPattern.MatchingContext;

/**
 * A path element that captures multiple path segments.
 * This element is only allowed in two situations:
 * <ol>
 * <li>At the start of a path, immediately followed by a {@link LiteralPathElement} like '/{*foobar}/foo/{bar}'
 * <li>At the end of a path, like '/foo/{*foobar}'
 * </ol>
 * <p>Only a single {@link WildcardSegmentsPathElement} or {@link CaptureSegmentsPathElement} element is allowed
 *  * in a pattern. In the pattern '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureSegmentsPathElement}.
 *
 * @author Andy Clement
 * @author Brian Clozel
 * @since 5.0
 */
class CaptureSegmentsPathElement extends PathElement {

	private final String variableName;


	/**
	 * Create a new {@link CaptureSegmentsPathElement} instance.
	 * @param pos position of the path element within the path pattern text
	 * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
	 * @param separator the separator used in the path pattern
	 */
	CaptureSegmentsPathElement(int pos, char[] captureDescriptor, char separator) {
		super(pos, separator);
		this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// wildcard segments at the start of the pattern
		if (pathIndex == 0 && this.next != null) {
			int endPathIndex = pathIndex;
			while (endPathIndex < matchingContext.pathLength) {
				if (this.next.matches(endPathIndex, matchingContext)) {
					collectParameters(matchingContext, pathIndex, endPathIndex);
					return true;
				}
				endPathIndex++;
			}
			return false;
		}
		// match until the end of the path
		else if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		collectParameters(matchingContext, pathIndex, matchingContext.pathLength);
		return true;
	}

	private void collectParameters(MatchingContext matchingContext, int pathIndex, int endPathIndex) {
		if (matchingContext.extractingVariables) {
			// Collect the parameters from all the remaining segments
			MultiValueMap<String, String> parametersCollector = NO_PARAMETERS;
			for (int i = pathIndex; i < endPathIndex; i++) {
				Element element = matchingContext.pathElements.get(i);
				if (element instanceof PathSegment pathSegment) {
					MultiValueMap<String, String> parameters = pathSegment.parameters();
					if (!parameters.isEmpty()) {
						if (parametersCollector == NO_PARAMETERS) {
							parametersCollector = new LinkedMultiValueMap<>();
						}
						parametersCollector.addAll(parameters);
					}
				}
			}
			matchingContext.set(this.variableName, pathToString(pathIndex, endPathIndex, matchingContext.pathElements),
					parametersCollector);
		}
	}

	private String pathToString(int fromSegment, int toSegment, List<Element> pathElements) {
		StringBuilder sb = new StringBuilder();
		for (int i = fromSegment, max = toSegment; i < max; i++) {
			Element element = pathElements.get(i);
			if (element instanceof PathSegment pathSegment) {
				sb.append(pathSegment.valueToMatch());
			}
			else {
				sb.append(element.value());
			}
		}
		return sb.toString();
	}

	@Override
	public int getNormalizedLength() {
		return 1;
	}

	@Override
	public char[] getChars() {
		return ("/{*" + this.variableName + "}").toCharArray();
	}

	@Override
	public int getWildcardCount() {
		return 0;
	}

	@Override
	public int getCaptureCount() {
		return 1;
	}


	@Override
	public String toString() {
		return "CaptureSegments(/{*" + this.variableName + "})";
	}

}
