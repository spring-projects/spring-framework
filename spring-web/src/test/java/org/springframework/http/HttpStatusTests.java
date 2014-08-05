/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.http;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/** @author Arjen Poutsma */
public class HttpStatusTests {

	private Map<Integer, String> statusCodes = new LinkedHashMap<Integer, String>();

	@Before
	public void createStatusCodes() {
		statusCodes.put(100, "CONTINUE");
		statusCodes.put(101, "SWITCHING_PROTOCOLS");
		statusCodes.put(102, "PROCESSING");
		statusCodes.put(103, "CHECKPOINT");

		statusCodes.put(200, "OK");
		statusCodes.put(201, "CREATED");
		statusCodes.put(202, "ACCEPTED");
		statusCodes.put(203, "NON_AUTHORITATIVE_INFORMATION");
		statusCodes.put(204, "NO_CONTENT");
		statusCodes.put(205, "RESET_CONTENT");
		statusCodes.put(206, "PARTIAL_CONTENT");
		statusCodes.put(207, "MULTI_STATUS");
		statusCodes.put(208, "ALREADY_REPORTED");
		statusCodes.put(226, "IM_USED");

		statusCodes.put(300, "MULTIPLE_CHOICES");
		statusCodes.put(301, "MOVED_PERMANENTLY");
		statusCodes.put(302, "FOUND");
		statusCodes.put(303, "SEE_OTHER");
		statusCodes.put(304, "NOT_MODIFIED");
		statusCodes.put(305, "USE_PROXY");
		statusCodes.put(307, "TEMPORARY_REDIRECT");
		statusCodes.put(308, "PERMANENT_REDIRECT");

		statusCodes.put(400, "BAD_REQUEST");
		statusCodes.put(401, "UNAUTHORIZED");
		statusCodes.put(402, "PAYMENT_REQUIRED");
		statusCodes.put(403, "FORBIDDEN");
		statusCodes.put(404, "NOT_FOUND");
		statusCodes.put(405, "METHOD_NOT_ALLOWED");
		statusCodes.put(406, "NOT_ACCEPTABLE");
		statusCodes.put(407, "PROXY_AUTHENTICATION_REQUIRED");
		statusCodes.put(408, "REQUEST_TIMEOUT");
		statusCodes.put(409, "CONFLICT");
		statusCodes.put(410, "GONE");
		statusCodes.put(411, "LENGTH_REQUIRED");
		statusCodes.put(412, "PRECONDITION_FAILED");
		statusCodes.put(413, "PAYLOAD_TOO_LARGE");
		statusCodes.put(414, "URI_TOO_LONG");
		statusCodes.put(415, "UNSUPPORTED_MEDIA_TYPE");
		statusCodes.put(416, "REQUESTED_RANGE_NOT_SATISFIABLE");
		statusCodes.put(417, "EXPECTATION_FAILED");
		statusCodes.put(418, "I_AM_A_TEAPOT");
		statusCodes.put(419, "INSUFFICIENT_SPACE_ON_RESOURCE");
		statusCodes.put(420, "METHOD_FAILURE");
		statusCodes.put(421, "DESTINATION_LOCKED");
		statusCodes.put(422, "UNPROCESSABLE_ENTITY");
		statusCodes.put(423, "LOCKED");
		statusCodes.put(424, "FAILED_DEPENDENCY");
		statusCodes.put(426, "UPGRADE_REQUIRED");
		statusCodes.put(428, "PRECONDITION_REQUIRED");
		statusCodes.put(429, "TOO_MANY_REQUESTS");
		statusCodes.put(431, "REQUEST_HEADER_FIELDS_TOO_LARGE");

		statusCodes.put(500, "INTERNAL_SERVER_ERROR");
		statusCodes.put(501, "NOT_IMPLEMENTED");
		statusCodes.put(502, "BAD_GATEWAY");
		statusCodes.put(503, "SERVICE_UNAVAILABLE");
		statusCodes.put(504, "GATEWAY_TIMEOUT");
		statusCodes.put(505, "HTTP_VERSION_NOT_SUPPORTED");
		statusCodes.put(506, "VARIANT_ALSO_NEGOTIATES");
		statusCodes.put(507, "INSUFFICIENT_STORAGE");
		statusCodes.put(508, "LOOP_DETECTED");
		statusCodes.put(509, "BANDWIDTH_LIMIT_EXCEEDED");
		statusCodes.put(510, "NOT_EXTENDED");
		statusCodes.put(511, "NETWORK_AUTHENTICATION_REQUIRED");
	}

	@Test
	public void fromMapToEnum() {
		for (Map.Entry<Integer, String> entry : statusCodes.entrySet()) {
			int value = entry.getKey();
			HttpStatus status = HttpStatus.valueOf(value);
			assertEquals("Invalid value", value, status.value());
			assertEquals("Invalid name for [" + value + "]", entry.getValue(), status.name());
		}
	}

	@Test
	public void fromEnumToMap() {

		for (HttpStatus status : HttpStatus.values()) {
			int value = status.value();
			if (value == 302 || value == 413 || value == 414) {
				continue;
			}
			assertTrue("Map has no value for [" + value + "]", statusCodes.containsKey(value));
			assertEquals("Invalid name for [" + value + "]", statusCodes.get(value), status.name());
		}
	}
}
