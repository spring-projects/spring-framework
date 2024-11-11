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

package org.springframework.web.reactive.function.client;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebClientResponseException}.
 *
 * @author Simon Baslé
 */
class WebClientResponseExceptionTests {

	@Test
	void constructWithSuccessStatusCodeAndNoCauseAdditionalMessage() {
		assertThat(new WebClientResponseException(200, "OK", null, null, null))
				.hasNoCause()
				.hasMessage("200 OK, but response failed with cause: null");
	}

	@Test
	void constructWith1xxStatusCodeAndCauseAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException(100, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("100 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
	}

	@Test
	void constructWith2xxStatusCodeAndCauseAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException(200, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("200 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
	}

	@Test
	void constructWith3xxStatusCodeAndCauseAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException(300, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("300 reasonPhrase, but response failed with cause: java.lang.RuntimeException: example cause");
	}

	@Test
	void constructWithExplicitMessageAndNotErrorCodeAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException("explicit message", 100, "reasonPhrase", null, null, null);
		assertThat(ex).hasMessage("explicit message, but response failed with cause: null");
	}

	@Test
	void constructWithExplicitMessageAndNotErrorCodeAndCauseAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException("explicit message", 100, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("explicit message, but response failed with cause: java.lang.RuntimeException: example cause")
				.hasRootCauseMessage("example cause");
	}

	@Test
	void constructWithExplicitMessageAndErrorCodeAndCauseNoAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException("explicit message", 404, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("explicit message").hasRootCauseMessage("example cause");
	}

	@Test
	void constructWith4xxStatusCodeAndCauseNoAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException(400, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("400 reasonPhrase").hasRootCauseMessage("example cause");
	}

	@Test
	void constructWith5xxStatusCodeAndCauseNoAdditionalMessage() {
		WebClientResponseException ex = new WebClientResponseException(500, "reasonPhrase", null, null, null);
		ex.initCause(new RuntimeException("example cause"));
		assertThat(ex).hasMessage("500 reasonPhrase").hasRootCauseMessage("example cause");
	}

}
