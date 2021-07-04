/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.test.web.servlet.htmlunit;

import java.util.regex.Pattern;

import com.gargoylesoftware.htmlunit.WebRequest;

/**
 * A {@link WebRequestMatcher} that allows matching on
 * {@code WebRequest#getUrl().toExternalForm()} using a regular expression.
 *
 * <p>For example, if you would like to match on the domain {@code code.jquery.com},
 * you might want to use the following.
 *
 * <pre class="code">
 * WebRequestMatcher cdnMatcher = new UrlRegexRequestMatcher(".*?//code.jquery.com/.*");
 * </pre>
 *
 * @author Rob Winch
 * @author Sam Brannen
 * @since 4.2
 * @see org.springframework.test.web.servlet.htmlunit.DelegatingWebConnection
 */
public final class UrlRegexRequestMatcher implements WebRequestMatcher {

	private final Pattern pattern;


	public UrlRegexRequestMatcher(String regex) {
		this.pattern = Pattern.compile(regex);
	}

	public UrlRegexRequestMatcher(Pattern pattern) {
		this.pattern = pattern;
	}


	@Override
	public boolean matches(WebRequest request) {
		String url = request.getUrl().toExternalForm();
		return this.pattern.matcher(url).matches();
	}

}
