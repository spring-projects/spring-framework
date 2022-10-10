/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.http.observation;


import io.micrometer.common.KeyValue;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpOutcome}.
 *
 * @author Brian Clozel
 */
class HttpOutcomeTests {

	@ParameterizedTest
	@ValueSource(ints = {100, 101, 102})
	void shouldResolveInformational(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.INFORMATIONAL);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "INFORMATIONAL"));
	}

	@ParameterizedTest
	@ValueSource(ints = {200, 202, 226})
	void shouldResolveSuccess(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.SUCCESS);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "SUCCESS"));
	}

	@ParameterizedTest
	@ValueSource(ints = {300, 302, 303})
	void shouldResolveRedirection(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.REDIRECTION);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "REDIRECTION"));
	}

	@ParameterizedTest
	@ValueSource(ints = {400, 404, 405})
	void shouldResolveClientError(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.CLIENT_ERROR);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "CLIENT_ERROR"));
	}

	@ParameterizedTest
	@ValueSource(ints = {500, 502, 503})
	void shouldResolveServerError(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.SERVER_ERROR);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "SERVER_ERROR"));
	}

	@ParameterizedTest
	@ValueSource(ints = {600, 799, 855})
	void shouldResolveUnknown(int code) {
		HttpOutcome httpOutcome = HttpOutcome.forStatus(HttpStatusCode.valueOf(code));
		assertThat(httpOutcome).isEqualTo(HttpOutcome.UNKNOWN);
		assertThat(httpOutcome.asKeyValue()).isEqualTo(KeyValue.of("outcome", "UNKNOWN"));
	}

}
