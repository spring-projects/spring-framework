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

package org.springframework.test.web.support;

import org.junit.jupiter.api.Test;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;

/**
 * Tests for {@link AbstractStatusAssertions}.
 *
 * @author Rossen Stoyanchev
 * @author Rob Worsnop
 */
class StatusAssertionTests {

	@Test
	void isEqualTo() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.isEqualTo(HttpStatus.CONFLICT);
		assertions.isEqualTo(409);

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.isEqualTo(HttpStatus.REQUEST_TIMEOUT));

		// Wrong status value
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> assertions.isEqualTo(408));
	}

	@Test
	void isEqualToWithCustomStatus() {
		TestStatusAssertions assertions = new TestStatusAssertions(600);

		// Success
		assertions.isEqualTo(600);

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(601).isEqualTo(600));

	}

	@Test
	void reasonEquals() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.reasonEquals("Conflict");

		// Wrong reason
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).reasonEquals("Conflict"));
	}

	@Test
	void statusSeries1xx() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.CONTINUE);

		// Success
		assertions.is1xxInformational();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.OK).is1xxInformational());
	}

	@Test
	void statusSeries2xx() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.OK);

		// Success
		assertions.is2xxSuccessful();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is2xxSuccessful());
	}

	@Test
	void statusSeries3xx() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.PERMANENT_REDIRECT);

		// Success
		assertions.is3xxRedirection();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is3xxRedirection());
	}

	@Test
	void statusSeries4xx() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.BAD_REQUEST);

		// Success
		assertions.is4xxClientError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).is4xxClientError());
	}

	@Test
	void statusSeries5xx() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR);

		// Success
		assertions.is5xxServerError();

		// Wrong series
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.OK).is5xxServerError());
	}

	@Test
	void matchesStatusValue() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.value(equalTo(409));
		assertions.value(greaterThan(400));

		// Wrong status
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() -> assertions.value(equalTo(200)));
	}

	@Test
	void matchesCustomStatusValue() {
		new TestStatusAssertions(600).value(equalTo(600));
	}

	@Test
	void consumesStatusValue() {
		TestStatusAssertions assertions = new TestStatusAssertions(HttpStatus.CONFLICT);

		// Success
		assertions.value((Integer value) -> assertThat(value).isEqualTo(409));
	}

	@Test
	void statusIsAccepted() {
		// Success
		new TestStatusAssertions(HttpStatus.ACCEPTED).isAccepted();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isAccepted());
	}

	@Test
	void statusIsNoContent() {
		// Success
		new TestStatusAssertions(HttpStatus.NO_CONTENT).isNoContent();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isNoContent());
	}

	@Test
	void statusIsFound() {
		// Success
		new TestStatusAssertions(HttpStatus.FOUND).isFound();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isFound());
	}

	@Test
	void statusIsSeeOther() {
		// Success
		new TestStatusAssertions(HttpStatus.SEE_OTHER).isSeeOther();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isSeeOther());
	}

	@Test
	void statusIsNotModified() {
		// Success
		new TestStatusAssertions(HttpStatus.NOT_MODIFIED).isNotModified();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isNotModified());
	}

	@Test
	void statusIsTemporaryRedirect() {
		// Success
		new TestStatusAssertions(HttpStatus.TEMPORARY_REDIRECT).isTemporaryRedirect();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isTemporaryRedirect());
	}

	@Test
	void statusIsPermanentRedirect() {
		// Success
		new TestStatusAssertions(HttpStatus.PERMANENT_REDIRECT).isPermanentRedirect();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isPermanentRedirect());
	}

	@Test
	void statusIsUnauthorized() {
		// Success
		new TestStatusAssertions(HttpStatus.UNAUTHORIZED).isUnauthorized();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isUnauthorized());
	}

	@Test
	void statusIsForbidden() {
		// Success
		new TestStatusAssertions(HttpStatus.FORBIDDEN).isForbidden();

		// Wrong status
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> new TestStatusAssertions(HttpStatus.INTERNAL_SERVER_ERROR).isForbidden());
	}


	private static class TestStatusAssertions extends AbstractStatusAssertions<TestExchangeResult, Object> {

		TestStatusAssertions(HttpStatus status) {
			this(status.value());
		}

		TestStatusAssertions(int status) {
			super(new TestExchangeResult(HttpStatusCode.valueOf(status)), "");
		}

		@Override
		protected HttpStatusCode getStatus() {
			return getExchangeResult().status();
		}

		@Override
		protected void assertWithDiagnostics(Runnable assertion) {
			assertion.run();
		}
	}


	private record TestExchangeResult(HttpStatusCode status) {
	}

}
