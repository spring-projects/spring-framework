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

package org.springframework.test.web.client.samples.matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.MediaType;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

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

	private RestClient restClient;

	private MockRestServiceServer mockServer;


	@BeforeEach
	void setup() {
		RestClient.Builder clientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.createServer(clientBuilder);
		this.restClient = clientBuilder.build();
	}


	@Test
	void string() {
		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", "application/json"))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		executeAndVerify();
	}

	@Test
	void stringContains() {
		this.mockServer.expect(requestTo("/person/1"))
			.andExpect(header("Accept", containsString("json")))
			.andRespond(withSuccess(RESPONSE_BODY, MediaType.APPLICATION_JSON));

		executeAndVerify();
	}

	private void executeAndVerify() {
		this.restClient.get().uri("/person/1").accept(MediaType.APPLICATION_JSON).retrieve().body(Person.class);
		this.mockServer.verify();
	}

}
