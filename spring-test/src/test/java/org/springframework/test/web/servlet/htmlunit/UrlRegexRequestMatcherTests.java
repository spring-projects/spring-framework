/*
 * Copyright 2002-2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.springframework.test.web.servlet.htmlunit;

import org.junit.Test;

/**
 * Unit tests for {@link UrlRegexRequestMatcher}.
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 */
public class UrlRegexRequestMatcherTests extends AbstractWebRequestMatcherTests {

	@Test
	public void verifyExampleInClassLevelJavadoc() throws Exception {
		WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
		assertMatches(cdnMatcher, "http://code.jquery.com/jquery-1.11.0.min.js");
		assertDoesNotMatch(cdnMatcher, "http://localhost/jquery-1.11.0.min.js");
	}

}