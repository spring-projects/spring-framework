/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.web;

import org.assertj.core.api.AbstractStringAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.error.BasicErrorMessageFactory;
import org.assertj.core.internal.Failures;

import org.springframework.lang.Nullable;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * AssertJ {@link org.assertj.core.api.Assert assertions} that can be applied
 * to a {@link String} representing a URI.
 *
 * @author Stephane Nicoll
 * @since 6.2
 */
public class UriAssert extends AbstractStringAssert<UriAssert> {

	private static final AntPathMatcher pathMatcher = new AntPathMatcher();

	private final String displayName;

	public UriAssert(@Nullable String actual, String displayName) {
		super(actual, UriAssert.class);
		this.displayName = displayName;
		as(displayName);
	}

	/**
	 * Verify that the actual URI is equal to the URI built using the given
	 * {@code uriTemplate} and {@code uriVars}.
	 * Example: <pre><code class='java'>
	 * // Verify that uri is equal to "/orders/1/items/2"
	 * assertThat(uri).isEqualToTemplate("/orders/{orderId}/items/{itemId}", 1, 2));
	 * </code></pre>
	 * @param uriTemplate the expected URI string, with a number of URI
	 * template variables
	 * @param uriVars the values to replace the URI template variables
	 * @see UriComponentsBuilder#buildAndExpand(Object...)
	 */
	public UriAssert isEqualToTemplate(String uriTemplate, Object... uriVars) {
		String uri = buildUri(uriTemplate, uriVars);
		return isEqualTo(uri);
	}

	/**
	 * Verify that the actual URI matches the given {@linkplain AntPathMatcher
	 * Ant-style} {@code uriPattern}.
	 * Example: <pre><code class='java'>
	 * // Verify that pattern matches "/orders/1/items/2"
	 * assertThat(uri).matchPattern("/orders/*"));
	 * </code></pre>
	 * @param uriPattern the pattern that is expected to match
	 * @see AntPathMatcher
	 */
	public UriAssert matchesAntPattern(String uriPattern) {
		Assertions.assertThat(pathMatcher.isPattern(uriPattern))
				.withFailMessage("'%s' is not an Ant-style path pattern", uriPattern).isTrue();
		Assertions.assertThat(pathMatcher.match(uriPattern, this.actual))
				.withFailMessage("%s '%s' does not match the expected URI pattern '%s'",
						this.displayName, this.actual, uriPattern).isTrue();
		return this;
	}

	@SuppressWarnings("NullAway")
	private String buildUri(String uriTemplate, Object... uriVars) {
		try {
			return UriComponentsBuilder.fromUriString(uriTemplate)
					.buildAndExpand(uriVars).encode().toUriString();
		}
		catch (Exception ex) {
			throw Failures.instance().failure(this.info,
					new ShouldBeValidUriTemplate(uriTemplate, ex.getMessage()));
		}
	}


	private static final class ShouldBeValidUriTemplate extends BasicErrorMessageFactory {

		private ShouldBeValidUriTemplate(String uriTemplate, String errorMessage) {
			super("%nExpecting:%n  %s%nTo be a valid URI template but got:%n  %s%n", uriTemplate, errorMessage);
		}
	}

}
