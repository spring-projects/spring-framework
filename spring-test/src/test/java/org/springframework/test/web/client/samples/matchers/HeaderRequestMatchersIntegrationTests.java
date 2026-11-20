/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
