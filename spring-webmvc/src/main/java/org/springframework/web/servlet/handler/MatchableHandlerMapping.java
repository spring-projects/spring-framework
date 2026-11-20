/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.web.servlet.handler;

import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;

import org.springframework.util.PathMatcher;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.util.UrlPathHelper;
import org.springframework.web.util.pattern.PathPatternParser;

/**
 * Additional interface that a {@link HandlerMapping} can implement to expose
 * a request matching API aligned with its internal request matching
 * configuration and implementation.
 *
 * @author Rossen Stoyanchev
 * @since 4.3.1
 * @deprecated together with {@link HandlerMappingIntrospector} without a replacement.
 */
@Deprecated(since = "7.0", forRemoval = true)
public interface MatchableHandlerMapping extends HandlerMapping {

	/**
	 * Return the parser of this {@code HandlerMapping}, if configured in which
	 * case pre-parsed patterns are used.
	 * @since 5.3
	 */
	default @Nullable PathPatternParser getPatternParser() {
		return null;
	}

	/**
	 * Determine whether the request matches the given pattern. Use this method
	 * when {@link #getPatternParser()} returns {@code null} which means that the
	 * {@code HandlerMapping} is using String pattern matching.
	 * @param request the current request
	 * @param pattern the pattern to match
	 * @return the result from request matching, or {@code null} if none
	 * @deprecated use of {@link PathMatcher} and {@link UrlPathHelper} is deprecated
	 * for use at runtime in web modules in favor of parsed patterns with
	 * {@link PathPatternParser}.
	 */
	@SuppressWarnings("removal")
	@Deprecated(since = "7.0", forRemoval = true)
	@Nullable RequestMatchResult match(HttpServletRequest request, String pattern);

}
