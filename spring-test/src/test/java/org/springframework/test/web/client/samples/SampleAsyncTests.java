/*
 * Copyright 2002-2016 the original author or authors.
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
package org.springframework.test.web.client.samples;

import org.junit.Before;
import org.junit.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.ExpectedCount;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.client.AsyncRestTemplate;

import static org.junit.Assert.*;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/**
 * Examples to demonstrate writing client-side REST tests with Spring MVC Test.
 * While the tests in this class invoke the RestTemplate directly, in actual
 * tests the RestTemplate may likely be invoked indirectly, i.e. through client
 * code.
 *
 * @author Rossen Stoyanchev
 */
public class SampleAsyncTests {

	private MockRestServiceServer mockServer;

	private AsyncRestTemplate restTemplate;


	@Before
	public void setup() {
		this.restTemplate = new AsyncRestTemplate();
		this.mockServer = MockRestServiceServer.createServer(this.restTemplate);

	}

	@Test
	public void performGet() throws Exception {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		ListenableFuture<ResponseEntity<Person>> ludwig =
				this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);

		// We are only validating the request. The response is mocked out.
		// person.getName().equals("Ludwig van Beethoven")
		// person.getDouble().equals(1.6035)

		this.mockServer.verify();
	}

	@Test
	public void performGetManyTimes() throws Exception {

		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(manyTimes(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		ListenableFuture<ResponseEntity<Person>> ludwig =
				this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);

		// We are only validating the request. The response is mocked out.
		// person.getName().equals("Ludwig van Beethoven")
		// person.getDouble().equals(1.6035)

		this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);
		this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);
		this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);
		this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);

		this.mockServer.verify();
	}

	@Test
	public void performGetWithResponseBodyFromFile() throws Exception {

		Resource responseBody = new ClassPathResource("ludwig.json", this.getClass());

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		ListenableFuture<ResponseEntity<Person>> ludwig =
				this.restTemplate.getForEntity("/composers/{id}", Person.class, 42);

		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.mockServer.verify();
	}

	@Test
	public void verify() {

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("8", MediaType.TEXT_PLAIN));

		@SuppressWarnings("unused")
		ListenableFuture<ResponseEntity<String>> result = this.restTemplate.getForEntity("/number", String.class);
		// result == "1"

		result = this.restTemplate.getForEntity("/number", String.class);
		// result == "2"

		try {
			this.mockServer.verify();
		}
		catch (AssertionError error) {
			assertTrue(error.getMessage(), error.getMessage().contains("2 unsatisfied expectation(s)"));
		}
	}
}
