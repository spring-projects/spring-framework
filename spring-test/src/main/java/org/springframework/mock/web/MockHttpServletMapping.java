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

package org.springframework.mock.web;

import javax.servlet.http.HttpServletMapping;
import javax.servlet.http.MappingMatch;

import org.springframework.lang.Nullable;

/**
 * Mock implementation of {@link HttpServletMapping}.
 *
 * <p>Currently not exposed in {@link MockHttpServletRequest} as a setter to
 * avoid issues for Maven builds in applications with a Servlet 3.1 runtime
 * requirement.
 *
 * @author Rossen Stoyanchev
 * @since 5.3.4
 */
public class MockHttpServletMapping implements HttpServletMapping {

	private final String matchValue;

	private final String pattern;

	private final String servletName;

	@Nullable
	private final MappingMatch mappingMatch;


	public MockHttpServletMapping(
			String matchValue, String pattern, String servletName, @Nullable MappingMatch match) {

		this.matchValue = matchValue;
		this.pattern = pattern;
		this.servletName = servletName;
		this.mappingMatch = match;
	}


	@Override
	public String getMatchValue() {
		return this.matchValue;
	}

	@Override
	public String getPattern() {
		return this.pattern;
	}

	@Override
	public String getServletName() {
		return this.servletName;
	}

	@Override
	@Nullable
	public MappingMatch getMappingMatch() {
		return this.mappingMatch;
	}


	@Override
	public String toString() {
		return "MockHttpServletMapping [matchValue=\"" + this.matchValue + "\", " +
				"pattern=\"" + this.pattern + "\", servletName=\"" + this.servletName + "\", " +
				"mappingMatch=" + this.mappingMatch + "]";
	}

}
