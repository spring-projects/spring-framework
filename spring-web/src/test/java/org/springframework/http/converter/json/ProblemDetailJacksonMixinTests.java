/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.http.converter.json;

import java.net.URI;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for serializing a {@link org.springframework.http.ProblemDetail} through
 * the Jackson library.
 *
 * @author Rossen Stoyanchev
 * @since 6.0
 */
class ProblemDetailJacksonMixinTests {

	private final ObjectMapper mapper = new Jackson2ObjectMapperBuilder().build();


	@Test
	void writeStatusAndHeaders() throws Exception {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header");
		testWrite(detail,
				"""
				{
					"type": "about:blank",
					"title": "Bad Request",
					"status": 400,
					"detail": "Missing header"
				}""");
	}

	@Test
	void writeCustomProperty() throws Exception {
		ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Missing header");
		detail.setProperty("host", "abc.org");
		detail.setProperty("user", null);

		testWrite(detail, """
				{
					"type": "about:blank",
					"title": "Bad Request",
					"status": 400,
					"detail": "Missing header",
					"host": "abc.org",
					"user": null
				}""");
	}

	@Test
	void readCustomProperty() throws Exception {
		ProblemDetail detail = this.mapper.readValue("""
				{
					"type": "about:blank",
					"title": "Bad Request",
					"status": 400,
					"detail": "Missing header",
					"host": "abc.org",
					"user": null
				}""", ProblemDetail.class);

		assertThat(detail.getType()).isEqualTo(URI.create("about:blank"));
		assertThat(detail.getTitle()).isEqualTo("Bad Request");
		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getDetail()).isEqualTo("Missing header");
		assertThat(detail.getProperties())
				.containsEntry("host", "abc.org")
				.containsEntry("user", null);
	}

	@Test
	void readCustomPropertyFromXml() throws Exception {
		ObjectMapper xmlMapper = new Jackson2ObjectMapperBuilder().createXmlMapper(true).build();
		ProblemDetail detail = xmlMapper.readValue("""
				<problem xmlns="urn:ietf:rfc:7807">
					<type>about:blank</type>
					<title>Bad Request</title>
					<status>400</status>
					<detail>Missing header</detail>
					<host>abc.org</host>
				</problem>""", ProblemDetail.class);

		assertThat(detail.getType()).isEqualTo(URI.create("about:blank"));
		assertThat(detail.getTitle()).isEqualTo("Bad Request");
		assertThat(detail.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(detail.getDetail()).isEqualTo("Missing header");
		assertThat(detail.getProperties()).containsEntry("host", "abc.org");
	}

	private void testWrite(ProblemDetail problemDetail, String expected) throws Exception {
		String output = this.mapper.writeValueAsString(problemDetail);
		JSONAssert.assertEquals(expected, output, false);
	}

}
