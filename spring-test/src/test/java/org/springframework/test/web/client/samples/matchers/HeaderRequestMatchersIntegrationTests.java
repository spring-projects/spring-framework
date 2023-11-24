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

package org.springframework.test.web.client.samples.matchers;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on request headers.
 *
 * @author Rossen Stoyanchev
 */
class HeaderRequestMatchersIntegrationTests {

	private static final String RESPONSE_BODY = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

	private final RestTemplate restTemplate = new RestTemplate();

	private final MockRestServiceServer mockServer = MockRestServiceServer.createServer(this.restTemplate);


	@BeforeEach
	void setup() {
		this.restTemplate.setMessageConverters(
				List.of(new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter()));
	}


	@Test
	void testString() {
		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", "application/json, application/*+json"))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		executeAndVerify();
	}

	@Test
	void testStringContains() {
		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", containsString("json")))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		executeAndVerify();
	}

	private void executeAndVerify() {
		this.restTemplate.getForObject(URI.create("/person/1"), Person.class);
		this.mockServer.verify();
	}

}
