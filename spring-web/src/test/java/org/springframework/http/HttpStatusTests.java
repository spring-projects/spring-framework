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

package org.springframework.http;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link HttpStatus}.
 * @author Arjen Poutsma
 * @author Brian Clozel
 */
class HttpStatusTests {

	private final MultiValueMap<Integer, String> statusCodes = new LinkedMultiValueMap<>();


	@BeforeEach
	void createStatusCodes() {
		statusCodes.add(100, "CONTINUE");
		statusCodes.add(101, "SWITCHING_PROTOCOLS");
		statusCodes.add(102, "PROCESSING");
		statusCodes.add(103, "EARLY_HINTS");

		statusCodes.add(200, "OK");
		statusCodes.add(201, "CREATED");
		statusCodes.add(202, "ACCEPTED");
		statusCodes.add(203, "NON_AUTHORITATIVE_INFORMATION");
		statusCodes.add(204, "NO_CONTENT");
		statusCodes.add(205, "RESET_CONTENT");
		statusCodes.add(206, "PARTIAL_CONTENT");
		statusCodes.add(207, "MULTI_STATUS");
		statusCodes.add(208, "ALREADY_REPORTED");
		statusCodes.add(226, "IM_USED");

		statusCodes.add(300, "MULTIPLE_CHOICES");
		statusCodes.add(301, "MOVED_PERMANENTLY");
		statusCodes.add(302, "FOUND");
		statusCodes.add(303, "SEE_OTHER");
		statusCodes.add(304, "NOT_MODIFIED");
		statusCodes.add(307, "TEMPORARY_REDIRECT");
		statusCodes.add(308, "PERMANENT_REDIRECT");

		statusCodes.add(400, "BAD_REQUEST");
		statusCodes.add(401, "UNAUTHORIZED");
		statusCodes.add(402, "PAYMENT_REQUIRED");
		statusCodes.add(403, "FORBIDDEN");
		statusCodes.add(404, "NOT_FOUND");
		statusCodes.add(405, "METHOD_NOT_ALLOWED");
		statusCodes.add(406, "NOT_ACCEPTABLE");
		statusCodes.add(407, "PROXY_AUTHENTICATION_REQUIRED");
		statusCodes.add(408, "REQUEST_TIMEOUT");
		statusCodes.add(409, "CONFLICT");
		statusCodes.add(410, "GONE");
		statusCodes.add(411, "LENGTH_REQUIRED");
		statusCodes.add(412, "PRECONDITION_FAILED");
		statusCodes.add(413, "PAYLOAD_TOO_LARGE");
		statusCodes.add(413, "CONTENT_TOO_LARGE");
		statusCodes.add(414, "URI_TOO_LONG");
		statusCodes.add(415, "UNSUPPORTED_MEDIA_TYPE");
		statusCodes.add(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
		statusCodes.add(417, "EXPECTATION_FAILED");
		statusCodes.add(418, "I_AM_A_TEAPOT");
		statusCodes.add(421, "MISDIRECTED_REQUEST");
		statusCodes.add(422, "UNPROCESSABLE_CONTENT");
		statusCodes.add(422, "UNPROCESSABLE_ENTITY");
		statusCodes.add(423, "LOCKED");
		statusCodes.add(424, "FAILED_DEPENDENCY");
		statusCodes.add(425, "TOO_EARLY");
		statusCodes.add(426, "UPGRADE_REQUIRED");
		statusCodes.add(428, "PRECONDITION_REQUIRED");
		statusCodes.add(429, "TOO_MANY_REQUESTS");
		statusCodes.add(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");
		statusCodes.add(451, "UNAVAILABLE_FOR_LEGAL_REASONS");

		statusCodes.add(500, "INTERNAL_SERVER_ERROR");
		statusCodes.add(501, "NOT_IMPLEMENTED");
		statusCodes.add(502, "BAD_GATEWAY");
		statusCodes.add(503, "SERVICE_UNAVAILABLE");
		statusCodes.add(504, "GATEWAY_TIMEOUT");
		statusCodes.add(505, "HTTP_VERSION_NOT_SUPPORTED");
		statusCodes.add(506, "VARIANT_ALSO_NEGOTIATES");
		statusCodes.add(507, "INSUFFICIENT_STORAGE");
		statusCodes.add(508, "LOOP_DETECTED");
		statusCodes.add(509, "BANDWIDTH_LIMIT_EXCEEDED");
		statusCodes.add(510, "NOT_EXTENDED");
		statusCodes.add(511, "NETWORK_AUTHENTICATION_REQUIRED");
	}


	@Test
	void fromMapToEnum() throws Exception {
		for (Map.Entry<Integer, List<String>> entry : statusCodes.entrySet()) {
			int value = entry.getKey();
			HttpStatus status = HttpStatus.valueOf(value);
			assertThat(status.value()).as("Invalid value").isEqualTo(value);
			assertThat(entry.getValue()).as("Invalid name for [" + value + "]").contains(status.name());
			Deprecated deprecatedAnnotation = HttpStatus.class.getField(status.name()).getAnnotation(Deprecated.class);
			if (deprecatedAnnotation != null) {
				assertThat(statusCodes.get(entry.getKey()))
						.as("Should not resolve deprecated enum value when non-deprecated exists")
						.hasSize(1);
			}
		}
	}

	@Test
	void fromEnumToMap() {
		for (HttpStatus status : HttpStatus.values()) {
			int code = status.value();
			assertThat(statusCodes).as("Map has no value for [" + code + "]").containsKey(code);
			assertThat(statusCodes.get(code)).as("Invalid name for [" + code + "]").contains(status.name());
		}
	}

	@Test
	void allStatusSeriesShouldMatchExpectations() {
		// The Series of an HttpStatus is set manually, so we make sure it is the correct one.
		for (HttpStatus status : HttpStatus.values()) {
			HttpStatus.Series expectedSeries = HttpStatus.Series.valueOf(status.value());
			assertThat(status.series()).isEqualTo(expectedSeries);
		}
	}

}
