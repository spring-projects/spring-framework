/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import com.jayway.jsonpath.Configuration;
import org.hamcrest.Matcher;
import org.jspecify.annotations.Nullable;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.test.web.support.AbstractJsonPathAssertions;

/**
 * <a href="https://github.com/jayway/JsonPath">JsonPath</a> assertions.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 * @author Rob Worsnop
 * @since 5.0
 * @see <a href="https://github.com/jayway/JsonPath">https://github.com/jayway/JsonPath</a>
 * @see JsonPathExpectationsHelper
 */
public class JsonPathAssertions extends AbstractJsonPathAssertions<WebTestClient.BodyContentSpec> {


	JsonPathAssertions(
			WebTestClient.BodyContentSpec spec, String content, String expression,
			@Nullable Configuration configuration) {

		super(spec, content, expression, configuration);
	}


	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher)}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public <T> WebTestClient.BodyContentSpec value(Matcher<? super T> matcher) {
		getPathHelper().assertValue(getContent(), matcher);
		return getBodySpec();
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, Class)}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public <T> WebTestClient.BodyContentSpec value(Class<T> targetType, Matcher<? super T> matcher) {
		getPathHelper().assertValue(getContent(), matcher, targetType);
		return getBodySpec();
	}

	/**
	 * Delegates to {@link JsonPathExpectationsHelper#assertValue(String, Matcher, ParameterizedTypeReference)}.
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public <T> WebTestClient.BodyContentSpec value(ParameterizedTypeReference<T> targetType, Matcher<? super T> matcher) {
		getPathHelper().assertValue(getContent(), matcher, targetType);
		return getBodySpec();
	}

}
