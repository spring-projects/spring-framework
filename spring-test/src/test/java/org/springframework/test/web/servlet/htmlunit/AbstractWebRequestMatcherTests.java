/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.net.MalformedURLException;
import java.net.URI;

import org.htmlunit.WebRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Abstract base class for testing {@link WebRequestMatcher} implementations.
 *
 * @author Sam Brannen
 * @since 4.2
 */
abstract class AbstractWebRequestMatcherTests {

	protected void assertMatches(WebRequestMatcher matcher, String url) throws MalformedURLException {
		assertThat(matcher.matches(new WebRequest(URI.create(url).toURL()))).isTrue();
	}

	protected void assertDoesNotMatch(WebRequestMatcher matcher, String url) throws MalformedURLException {
		assertThat(matcher.matches(new WebRequest(URI.create(url).toURL()))).isFalse();
	}

}
