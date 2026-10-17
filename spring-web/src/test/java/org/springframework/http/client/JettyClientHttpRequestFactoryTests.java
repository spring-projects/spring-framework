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

package org.springframework.http.client;

import java.net.URI;
import java.time.Duration;

import org.eclipse.jetty.client.HttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Arjen Poutsma
 */
class JettyClientHttpRequestFactoryTests extends AbstractHttpRequestFactoryTests {

	@Override
	protected ClientHttpRequestFactory createRequestFactory() {
		return new JettyClientHttpRequestFactory();
	}

	@Override
	@Test
	void httpMethods() throws Exception {
		super.httpMethods();
		assertHttpMethod("patch", HttpMethod.PATCH);
	}

	@Test  // gh-37425
	void readTimeoutZero() throws Exception {
		JettyClientHttpRequestFactory requestFactory = (JettyClientHttpRequestFactory) this.factory;
		requestFactory.setReadTimeout(0);
		assertRequestSucceeds(requestFactory);

		requestFactory.setReadTimeout(Duration.ZERO);
		assertRequestSucceeds(requestFactory);
	}

	@Test  // gh-37425
	void negativeReadTimeoutIsRejected() {
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestFactory.setReadTimeout(-1))
				.withMessage("Timeout must be a non-negative value");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestFactory.setReadTimeout(Duration.ofMillis(-1)))
				.withMessage("Timeout must be a non-negative value");
	}

	@ParameterizedTest  // gh-37425
	@ValueSource(strings = {"PT0.000000001S", "PT0.000999999S"})
	void subMillisecondReadTimeoutIsRejected(Duration timeout) {
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestFactory.setReadTimeout(timeout))
				.withMessage("Timeout must be zero or at least one millisecond");
	}

	@Test  // gh-37425
	void negativeConnectTimeoutIsRejected() {
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestFactory.setConnectTimeout(Duration.ofMillis(-1)))
				.withMessage("Timeout must be a non-negative value");
	}

	@Test  // gh-37425
	void connectTimeoutZero() {
		HttpClient httpClient = new HttpClient();
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory(httpClient);
		requestFactory.setConnectTimeout(5_000);
		requestFactory.setConnectTimeout(0);
		assertThat(httpClient.getConnectTimeout()).isZero();

		requestFactory.setConnectTimeout(5_000);
		requestFactory.setConnectTimeout(Duration.ZERO);
		assertThat(httpClient.getConnectTimeout()).isZero();
	}

	@ParameterizedTest  // gh-37425
	@ValueSource(strings = {"PT0.000000001S", "PT0.000999999S"})
	void subMillisecondConnectTimeoutIsRejected(Duration timeout) {
		JettyClientHttpRequestFactory requestFactory = new JettyClientHttpRequestFactory();
		assertThatIllegalArgumentException()
				.isThrownBy(() -> requestFactory.setConnectTimeout(timeout))
				.withMessage("Timeout must be zero or at least one millisecond");
	}

	private void assertRequestSucceeds(JettyClientHttpRequestFactory requestFactory) throws Exception {
		ClientHttpRequest request = requestFactory.createRequest(
				URI.create(this.baseUrl + "/methods/get"), HttpMethod.GET);
		try (ClientHttpResponse response = request.execute()) {
			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		}
	}

}
