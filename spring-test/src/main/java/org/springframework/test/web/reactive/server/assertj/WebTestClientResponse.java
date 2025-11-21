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

package org.springframework.test.web.reactive.server.assertj;

import org.assertj.core.api.AssertProvider;

import org.springframework.test.web.reactive.server.ExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;


/**
 * {@link AssertProvider} for {@link WebTestClientResponseAssert} that holds the
 * result of an exchange performed through {@link WebTestClient}. Intended for
 * further use with AssertJ. For example:
 *
 * <pre class="code">
 * ResponseSpec spec = webTestClient.get().uri("/greeting").exchange();
 *
 * WebTestClientResponse response = WebTestClientResponse.from(spec);
 * assertThat(response).hasStatusOk();
 * assertThat(response).contentType().isCompatibleWith(MediaType.APPLICATION_JSON);
 * assertThat(response).bodyJson().extractingPath("$.message").asString().isEqualTo("Hello World");
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface WebTestClientResponse extends AssertProvider<WebTestClientResponseAssert> {

	/**
	 * Return the underlying {@link ExchangeResult}.
	 */
	ExchangeResult getExchangeResult();


	/**
	 * Create an instance from a {@link WebTestClient.ResponseSpec}.
	 */
	static WebTestClientResponse from(WebTestClient.ResponseSpec spec) {
		return from(spec.returnResult(byte[].class));
	}

	/**
	 * Create an instance from an {@link ExchangeResult}.
	 */
	static WebTestClientResponse from(ExchangeResult result) {
		return new DefaultWebTestClientResponse(result);
	}

}
