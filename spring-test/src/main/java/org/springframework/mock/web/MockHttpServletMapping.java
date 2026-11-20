/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.mock.web;

import jakarta.servlet.http.HttpServletMapping;
import jakarta.servlet.http.MappingMatch;
import org.jspecify.annotations.Nullable;

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

	private final @Nullable MappingMatch mappingMatch;


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
	public @Nullable MappingMatch getMappingMatch() {
		return this.mappingMatch;
	}


	@Override
	public String toString() {
		return "MockHttpServletMapping [matchValue=\"" + this.matchValue + "\", " +
				"pattern=\"" + this.pattern + "\", servletName=\"" + this.servletName + "\", " +
				"mappingMatch=" + this.mappingMatch + "]";
	}

}
