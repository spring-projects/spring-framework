/*
 * Copyright 2002-2010 the original author or authors.
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

import static org.junit.Assert.*;
import org.junit.Test;

/** @author Arjen Poutsma */
public class HttpStatusTests {

	private int[] registryValues =
			new int[]{100, 101, 102, 200, 201, 202, 203, 204, 205, 206, 207, 208, 226, 300, 301, 302, 303, 304, 305,
					307, 400, 401, 402, 403, 404, 405, 406, 407, 408, 409, 410, 411, 412, 413, 414, 415, 416, 417, 422,
					423, 424, 426, 500, 501, 502, 503, 504, 505, 506, 507, 508, 510,};

	private String[] registryDescriptions =
			new String[]{"CONTINUE", "SWITCHING_PROTOCOLS", "PROCESSING", "OK", "CREATED", "ACCEPTED",
					"NON_AUTHORITATIVE_INFORMATION", "NO_CONTENT", "RESET_CONTENT", "PARTIAL_CONTENT", "MULTI_STATUS",
					"ALREADY_REPORTED", "IM_USED", "MULTIPLE_CHOICES", "MOVED_PERMANENTLY", "FOUND", "SEE_OTHER",
					"NOT_MODIFIED", "USE_PROXY", "TEMPORARY_REDIRECT", "BAD_REQUEST", "UNAUTHORIZED",
					"PAYMENT_REQUIRED", "FORBIDDEN", "NOT_FOUND", "METHOD_NOT_ALLOWED", "NOT_ACCEPTABLE",
					"PROXY_AUTHENTICATION_REQUIRED", "REQUEST_TIMEOUT", "CONFLICT", "GONE", "LENGTH_REQUIRED",
					"PRECONDITION_FAILED", "REQUEST_ENTITY_TOO_LARGE", "REQUEST_URI_TOO_LONG", "UNSUPPORTED_MEDIA_TYPE",
					"REQUESTED_RANGE_NOT_SATISFIABLE", "EXPECTATION_FAILED", "UNPROCESSABLE_ENTITY", "LOCKED",
					"FAILED_DEPENDENCY", "UPGRADE_REQUIRED", "INTERNAL_SERVER_ERROR", "NOT_IMPLEMENTED", "BAD_GATEWAY",
					"SERVICE_UNAVAILABLE", "GATEWAY_TIMEOUT", "HTTP_VERSION_NOT_SUPPORTED", "VARIANT_ALSO_NEGOTIATES",
					"INSUFFICIENT_STORAGE", "LOOP_DETECTED", "NOT_EXTENDED",};

	@Test
	public void registryValues() {
		for (int i = 0; i < registryValues.length; i++) {
			HttpStatus status = HttpStatus.valueOf(registryValues[i]);
			assertEquals("Invalid value", registryValues[i], status.value());
			assertEquals("Invalid descripion", registryDescriptions[i], status.name());
		}

	}

}
