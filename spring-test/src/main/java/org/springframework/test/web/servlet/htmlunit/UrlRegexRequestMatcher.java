/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.servlet.htmlunit;

import java.util.regex.Pattern;

import org.htmlunit.WebRequest;

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
