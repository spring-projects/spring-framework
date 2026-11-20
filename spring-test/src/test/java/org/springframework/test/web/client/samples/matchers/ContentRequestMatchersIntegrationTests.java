/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client.samples.matchers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples of defining expectations on request content and content type.
 *
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 * @see JsonPathRequestMatchersIntegrationTests
 * @see XmlContentRequestMatchersIntegrationTests
 * @see XpathRequestMatchersIntegrationTests
 */
class ContentRequestMatchersIntegrationTests {

	private RestClient restClient;

	private MockRestServiceServer mockServer;


	@BeforeEach
	void setup() {
		RestClient.Builder clientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.createServer(clientBuilder);
		this.restClient = clientBuilder.build();
	}


	@Test
	void contentType() {
		this.mockServer.expect(content().contentType("application/json")).andRespond(withSuccess());
		executeAndVerify(new Person());
	}

	@Test
	void contentTypeNoMatch() {
		this.mockServer.expect(content().contentType("application/json;charset=UTF-8")).andRespond(withSuccess());
		assertThatExceptionOfType(AssertionError.class)
				.isThrownBy(() -> executeAndVerify("foo"))
				.withMessageStartingWith("Content type expected:<application/json;charset=UTF-8>");
	}

	@Test
	void contentAsString() {
		this.mockServer.expect(content().string("foo")).andRespond(withSuccess());
		executeAndVerify("foo");
	}

	@Test
	void contentStringStartsWith() {
		this.mockServer.expect(content().string(startsWith("foo"))).andRespond(withSuccess());
		executeAndVerify("foo123");
	}

	@Test
	void contentAsBytes() {
		this.mockServer.expect(content().bytes("foo".getBytes())).andRespond(withSuccess());
		executeAndVerify("foo");
	}

	private void executeAndVerify(Object body) {
		this.restClient.put().uri("/foo").body(body).retrieve().toBodilessEntity();
		this.mockServer.verify();
	}

}
