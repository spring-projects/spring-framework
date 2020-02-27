/*
 * Copyright 2002-2018 the original author or authors.
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
 * A path element representing capturing the rest of a path. In the pattern
 * '/foo/{*foobar}' the /{*foobar} is represented as a {@link CaptureTheRestPathElement}.
 *
 * @author Andy Clement
 * @since 5.0
 */
class CaptureTheRestPathElement extends PathElement {

	private final String variableName;


	/**
	 * Create a new {@link CaptureTheRestPathElement} instance.
	 * @param pos position of the path element within the path pattern text
	 * @param captureDescriptor a character array containing contents like '{' '*' 'a' 'b' '}'
	 * @param separator the separator used in the path pattern
	 */
	CaptureTheRestPathElement(int pos, char[] captureDescriptor, char separator) {
		super(pos, separator);
		this.variableName = new String(captureDescriptor, 2, captureDescriptor.length - 3);
	}


	@Override
	public boolean matches(int pathIndex, MatchingContext matchingContext) {
		// No need to handle 'match start' checking as this captures everything
		// anyway and cannot be followed by anything else
		// assert next == null

		// If there is more data, it must start with the separator
		if (pathIndex < matchingContext.pathLength && !matchingContext.isSeparator(pathIndex)) {
			return false;
		}
		if (matchingContext.determineRemainingPath) {
			matchingContext.remainingPathIndex = matchingContext.pathLength;
		}
		if (matchingContext.extractingVariables) {
			// Collect the parameters from all the remaining segments
			MultiValueMap<String,String> parametersCollector = null;
			for (int i = pathIndex; i < matchingContext.pathLength; i++) {
				Element element = matchingContext.pathElements.get(i);
				if (element instanceof PathSegment) {
					MultiValueMap<String, String> parameters = ((PathSegment) element).parameters();
					if (!parameters.isEmpty()) {
						if (parametersCollector == null) {
							parametersCollector = new LinkedMultiValueMap<>();
						}
						parametersCollector.addAll(parameters);
					}
				}
			}
			matchingContext.set(this.variableName, pathToString(pathIndex, matchingContext.pathElements),
					parametersCollector == null?NO_PARAMETERS:parametersCollector);
		}
		return true;
	}

	private String pathToString(int fromSegment, List<Element> pathElements) {
		StringBuilder buf = new StringBuilder();
		for (int i = fromSegment, max = pathElements.size(); i < max; i++) {
			Element element = pathElements.get(i);
			if (element instanceof PathSegment) {
				buf.append(((PathSegment)element).valueToMatch());
			}
			else {
				buf.append(element.value());
			}
		}
		return buf.toString();
	}

	@Override
	public int getNormalizedLength() {
		return 1;
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
		return "CaptureTheRest(/{*" + this.variableName + "})";
	}

	@Override
	public char[] getChars() {
		return ("/{*"+this.variableName+"}").toCharArray();
	}
}
