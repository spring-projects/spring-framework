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

package org.springframework.test.web.servlet.client.assertj;

import org.assertj.core.api.AssertProvider;

import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;

/**
 * {@link AssertProvider} for {@link RestTestClientResponseAssert} that holds the
 * result of an exchange performed through {@link RestTestClient}. Intended for
 * further use with AssertJ. For example:
 *
 * <pre class="code">
 * ResponseSpec spec = restTestClient.get().uri("/greeting").exchange();
 *
 * RestTestClientResponse response = RestTestClientResponse.from(spec);
 * assertThat(response).hasStatusOk();
 * assertThat(response).contentType().isCompatibleWith(MediaType.APPLICATION_JSON);
 * assertThat(response).bodyJson().extractingPath("$.message").asString().isEqualTo("Hello World");
 * </pre>
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public interface RestTestClientResponse extends AssertProvider<RestTestClientResponseAssert> {

	/**
	 * Return the underlying {@link ExchangeResult}.
	 */
	ExchangeResult getExchangeResult();


	/**
	 * Create an instance from a {@link RestTestClient.ResponseSpec}.
	 */
	static RestTestClientResponse from(RestTestClient.ResponseSpec spec) {
		return from(spec.returnResult(byte[].class));
	}

	/**
	 * Create an instance from an {@link ExchangeResult}.
	 */
	static RestTestClientResponse from(ExchangeResult result) {
		return new DefaultRestTestClientResponse(result);
	}

}
