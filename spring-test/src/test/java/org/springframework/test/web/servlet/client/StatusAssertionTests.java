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

package org.springframework.test.web.servlet.client;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.mockito.BDDMockito.mock;
import static org.mockito.BDDMockito.when;

/**
 * Tests for {@link StatusAssertions}.
 *
 * @author Rob Worsnop
 */
class StatusAssertionTests {

	@Test
	void isEqualTo() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.isEqualTo(HttpStatus.CONFLICT);
		assertions.isEqualTo(409);

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.isEqualTo(HttpStatus.REQUEST_TIMEOUT));

		// Wrong status value
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.isEqualTo(408));
	}

	@Test
	void isEqualToWithCustomStatus() {
		StatusAssertions assertions = statusAssertions(600);

		// Success
		assertions.isEqualTo(600);

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(601).isEqualTo(600));

	}

	@Test
	void reasonEquals() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.reasonEquals("Conflict");

		// Wrong reason
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).reasonEquals("Conflict"));
	}

	@Test
	void statusSeries1xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONTINUE);

		// Success
		assertions.is1xxInformational();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.OK).is1xxInformational());
	}

	@Test
	void statusSeries2xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.OK);

		// Success
		assertions.is2xxSuccessful();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is2xxSuccessful());
	}

	@Test
	void statusSeries3xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.PERMANENT_REDIRECT);

		// Success
		assertions.is3xxRedirection();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is3xxRedirection());
	}

	@Test
	void statusSeries4xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.BAD_REQUEST);

		// Success
		assertions.is4xxClientError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is4xxClientError());
	}

	@Test
	void statusSeries5xx() {
		StatusAssertions assertions = statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR);

		// Success
		assertions.is5xxServerError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				statusAssertions(HttpStatus.OK).is5xxServerError());
	}

	@Test
	void matchesStatusValue() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.value(equalTo(409));
		assertions.value(greaterThan(400));

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				assertions.value(equalTo(200)));
	}

	@Test
	void matchesCustomStatusValue() {
		statusAssertions(600).value(equalTo(600));
	}

	@Test
	void consumesStatusValue() {
		StatusAssertions assertions = statusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.value((Integer value) -> assertThat(value).isEqualTo(409));
	}

	@Test
	void statusIsAccepted() {
		// Success
		statusAssertions(HttpStatus.ACCEPTED).isAccepted();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isAccepted());
	}

	@Test
	void statusIsNoContent() {
		// Success
		statusAssertions(HttpStatus.NO_CONTENT).isNoContent();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isNoContent());
	}

	@Test
	void statusIsFound() {
		// Success
		statusAssertions(HttpStatus.FOUND).isFound();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isFound());
	}

	@Test
	void statusIsSeeOther() {
		// Success
		statusAssertions(HttpStatus.SEE_OTHER).isSeeOther();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isSeeOther());
	}

	@Test
	void statusIsNotModified() {
		// Success
		statusAssertions(HttpStatus.NOT_MODIFIED).isNotModified();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isNotModified());
	}

	@Test
	void statusIsTemporaryRedirect() {
		// Success
		statusAssertions(HttpStatus.TEMPORARY_REDIRECT).isTemporaryRedirect();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isTemporaryRedirect());
	}

	@Test
	void statusIsPermanentRedirect() {
		// Success
		statusAssertions(HttpStatus.PERMANENT_REDIRECT).isPermanentRedirect();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isPermanentRedirect());
	}

	@Test
	void statusIsUnauthorized() {
		// Success
		statusAssertions(HttpStatus.UNAUTHORIZED).isUnauthorized();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isUnauthorized());
	}

	@Test
	void statusIsForbidden() {
		// Success
		statusAssertions(HttpStatus.FORBIDDEN).isForbidden();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> statusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isForbidden());
	}

	private StatusAssertions statusAssertions(HttpStatus status) {
		return statusAssertions(status.value());
	}

	private StatusAssertions statusAssertions(int status) {
		try {
			RestClient.RequestHeadersSpec.ConvertibleClientHttpResponse response = mock();
			when(response.getStatusCode()).thenReturn(HttpStatusCode.valueOf(status));
			ExchangeResult result = new ExchangeResult(response);
			return new StatusAssertions(result, mock());
		}
		catch (IOException ex) {
			throw new AssertionError(ex);
		}
	}

}
