/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.docs.testing.resttestclient.assertj;

import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.client.ExchangeResult;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.test.web.servlet.client.assertj.RestTestClientResponse;

import static org.assertj.core.api.Assertions.assertThat;

public class AssertJTests {

	RestTestClient client;

	@Test
	void withSpec() {
		// tag::withSpec[]
		RestTestClient.ResponseSpec spec = client.get().uri("/persons").exchange();

		RestTestClientResponse response = RestTestClientResponse.from(spec);
		assertThat(response).hasStatusOk();
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
		// end::withSpec[]
	}

	@Test
	void withResult() {
		// tag::withResult[]
		ExchangeResult result = client.get().uri("/persons").exchange().returnResult();

		RestTestClientResponse response = RestTestClientResponse.from(result);
		assertThat(response).hasStatusOk();
		assertThat(response).hasContentTypeCompatibleWith(MediaType.TEXT_PLAIN);
		// end::withResult[]
	}

}
