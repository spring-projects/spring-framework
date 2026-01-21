/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.web.reactive.server;

import java.util.function.Consumer;

import org.hamcrest.Matcher;
import org.hamcrest.MatcherAssert;

import org.springframework.http.HttpStatusCode;
import org.springframework.test.web.support.AbstractStatusAssertions;

/**
 * Assertions on the response status.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectStatus()
 */
public class StatusAssertions extends AbstractStatusAssertions<ExchangeResult, WebTestClient.ResponseSpec> {


	StatusAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		super(result, spec);
	}


	@Override
	protected HttpStatusCode getStatus() {
		return getExchangeResult().getStatus();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}

	/**
	 * Match the response status value with a Hamcrest matcher.
	 * @param matcher the matcher to use
	 * @deprecated in favor of {@link Consumer}-based variants
	 */
	@Deprecated(since = "7.0", forRemoval = true)
	public WebTestClient.ResponseSpec value(Matcher<? super Integer> matcher) {
		int actual = getStatus().value();
		assertWithDiagnostics(() -> MatcherAssert.assertThat("Response status", actual, matcher));
		return getResponseSpec();
	}

}
