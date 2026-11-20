/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import org.junit.jupiter.api.Test;

/**
 * Tests for {@link UrlRegexRequestMatcher}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
class UrlRegexRequestMatcherTests extends AbstractWebRequestMatcherTests {

	@Test
	void verifyExampleInClassLevelJavadoc() throws Exception {
		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		assertMatches(cdnMatcher, "https://code.jquery.com/jquery-1.11.0.min.js");
		assertDoesNotMatch(cdnMatcher, "http://localhost/jquery-1.11.0.min.js");
	}

}
