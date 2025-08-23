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

import org.springframework.http.HttpHeaders;
import org.springframework.test.web.support.AbstractHeaderAssertions;

/**
 * Assertions on headers of the response.
 *
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 * @author Rob Worsnop
 * @since 5.0
 * @see WebTestClient.ResponseSpec#expectHeader()
 */
public class HeaderAssertions extends AbstractHeaderAssertions<ExchangeResult, WebTestClient.ResponseSpec> {


	HeaderAssertions(ExchangeResult result, WebTestClient.ResponseSpec spec) {
		super(result, spec);
	}


	@Override
	protected HttpHeaders getResponseHeaders() {
		return getExchangeResult().getResponseHeaders();
	}

	@Override
	protected void assertWithDiagnostics(Runnable assertion) {
		getExchangeResult().assertWithDiagnostics(assertion);
	}

}
