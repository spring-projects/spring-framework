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

import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

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

	private final RestTemplate restTemplate = new RestTemplate();

	private final MockRestServiceServer mockServer = MockRestServiceServer.createServer(this.restTemplate);


	@BeforeEach
	void setup() {
		this.restTemplate.setMessageConverters(
				List.of(new StringHttpMessageConverter(), new MappingJackson2HttpMessageConverter()));
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
		this.restTemplate.put(URI.create("/foo"), body);
		this.mockServer.verify();
	}

}
