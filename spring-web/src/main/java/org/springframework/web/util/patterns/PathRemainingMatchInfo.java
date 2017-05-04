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

package org.springframework.web.util.patterns;

import java.util.Map;

/**
 * A holder for the result of a {@link PathPattern#getPathRemaining(String)} call. Holds
 * information on the path left after the first part has successfully matched a pattern
 * and any variables bound in that first part that matched.
 * 
 * @author Andy Clement
 * @since 5.0
 */
public class PathRemainingMatchInfo {

	private String pathRemaining;
	
	private Map<String, String> matchingVariables;
	
	PathRemainingMatchInfo(String pathRemaining) {
		this.pathRemaining = pathRemaining;
	}
	
	PathRemainingMatchInfo(String pathRemaining, Map<String, String> matchingVariables) {
		this.pathRemaining = pathRemaining;
		this.matchingVariables = matchingVariables;
	}

	/**
	 * @return the part of a path that was not matched by a pattern
	 */
	public String getPathRemaining() {
		return pathRemaining;
	}
	
	/**
	 * @return variables that were bound in the part of the path that was successfully matched. 
	 * Will be an empty map if no variables were bound
	 */
	public Map<String, String> getMatchingVariables() {
		return matchingVariables;
	}
}
