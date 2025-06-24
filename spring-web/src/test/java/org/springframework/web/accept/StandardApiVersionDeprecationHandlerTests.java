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

package org.springframework.web.accept;

import java.net.URI;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.jupiter.api.Test;

import org.springframework.web.testfixture.servlet.MockHttpServletRequest;
import org.springframework.web.testfixture.servlet.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StandardApiVersionDeprecationHandler}.
 * @author Rossen Stoyanchev
 */
public class StandardApiVersionDeprecationHandlerTests {

	private final MockHttpServletRequest request = new MockHttpServletRequest();

	private final MockHttpServletResponse response = new MockHttpServletResponse();


	@Test
	void basic() {
		String deprecationUrl = "https://example.org/deprecation";
		String sunsetDate = "Wed, 11 Nov 2026 11:11:11 GMT";
		String sunsetUrl = "https://example.org/sunset";

		ApiVersionParser<String> parser = version -> version;
		StandardApiVersionDeprecationHandler handler = new StandardApiVersionDeprecationHandler(parser);

		handler.configureVersion("1.1")
				.setDeprecationDate(getDate("Fri, 30 Jun 2023 23:59:59 GMT"))
				.setDeprecationLink(URI.create(deprecationUrl))
				.setSunsetDate(getDate(sunsetDate))
				.setSunsetLink(URI.create(sunsetUrl));

		handler.handleVersion("1.1", request, response);

		assertThat(response.getHeader("Deprecation")).isEqualTo("@1688169599");
		assertThat(response.getHeader("Sunset")).isEqualTo(sunsetDate);
		assertThat(response.getHeaders("Link")).containsExactlyInAnyOrder(
				"<" + deprecationUrl + ">; rel=\"deprecation\"; type=\"text/html\"",
				"<" + sunsetUrl + ">; rel=\"sunset\"; type=\"text/html\""
		);
	}

	private static ZonedDateTime getDate(String date) {
		return ZonedDateTime.parse(date, DateTimeFormatter.RFC_1123_DATE_TIME);
	}

}
