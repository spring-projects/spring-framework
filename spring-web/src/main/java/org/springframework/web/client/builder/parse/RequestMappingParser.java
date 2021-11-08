/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client.builder.parse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.builder.parse.model.RequestDetails;

public final class RequestMappingParser {

	private RequestMappingParser() {
	}

	public static RequestDetails getRequestDetails(final RequestMapping requestMapping) {
		if (requestMapping.method().length != 1) {
			throw new RuntimeException(
					"Only one request method, currently, supported. PR:s are welcome.");
		}
		final HttpMethod requestMethod = HttpMethod.valueOf(requestMapping.method()[0].name());

		if (requestMapping.value().length != 1) {
			throw new RuntimeException("Only one request path, currently, supported. PR:s are welcome.");
		}
		final String requestPath = requestMapping.value()[0];

		if (requestMapping.consumes().length > 1) {
			throw new RuntimeException(
					"Only one, or zero, consumes, currently, supported. PR:s are welcome.");
		}
		MediaType consumes = null;
		if (requestMapping.consumes().length == 1) {
			final String consumesString = requestMapping.consumes()[0];
			consumes = MediaType.parseMediaType(consumesString);
		}

		if (requestMapping.produces().length > 1) {
			throw new RuntimeException(
					"Only one, or zero, produces, currently, supported. PR:s are welcome.");
		}
		MediaType produces = null;
		if (requestMapping.produces().length == 1) {
			final String producesString = requestMapping.produces()[0];
			produces = MediaType.parseMediaType(producesString);
		}

		final HttpHeaders httpHeaders = new HttpHeaders();
		for (final String header : requestMapping.headers()) {
			final int equalityIndex = header.indexOf("=");
			if (equalityIndex == -1) {
				throw new RuntimeException("Cannot parse header '" + header + "'");
			}
			final String[] spitted = header.split("=");
			httpHeaders.add(spitted[0], spitted[1]);
		}

		return new RequestDetails(requestMethod, requestPath, consumes, produces, httpHeaders);
	}
}
