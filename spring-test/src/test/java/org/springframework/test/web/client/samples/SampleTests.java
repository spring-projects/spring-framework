/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.test.web.client.samples;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.test.web.Person;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.web.client.ExpectedCount.manyTimes;
import static org.springframework.test.web.client.ExpectedCount.never;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Examples to demonstrate writing client-side REST tests with Spring MVC Test.
 *
 * <p>While the tests in this class invoke the RestClient directly, in actual
 * tests the RestClient may likely be invoked indirectly, i.e. through client
 * code.
 *
 * @author Rossen Stoyanchev
 */
class SampleTests {

	private RestClient restClient;

	private MockRestServiceServer mockServer;

	@BeforeEach
	void setup() {
		RestClient.Builder clientBuilder = RestClient.builder();
		this.mockServer = MockRestServiceServer.bindTo(clientBuilder).ignoreExpectOrder(true).build();
		this.restClient = clientBuilder.build();
	}


	@Test
	void performGet() {
		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		Person ludwig = this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		// We are only validating the request. The response is mocked out.
		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.mockServer.verify();
	}

	@Test
	void performGetManyTimes() {
		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(manyTimes(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		Person ludwig = this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		// We are only validating the request. The response is mocked out.
		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);
		this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);
		this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		this.mockServer.verify();
	}

	@Test
	void expectNever() {
		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(once(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		this.mockServer.expect(never(), requestTo("/composers/43")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		this.mockServer.verify();
	}

	@Test
	void expectNeverViolated() {
		String responseBody = "{\"name\" : \"Ludwig van Beethoven\", \"someDouble\" : \"1.6035\"}";

		this.mockServer.expect(once(), requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));
		this.mockServer.expect(never(), requestTo("/composers/43")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);
		assertThatExceptionOfType(AssertionError.class).isThrownBy(() ->
				this.restClient.get().uri("/composers/{id}", 43).retrieve().body(Person.class));
	}

	@Test
	void performGetWithResponseBodyFromFile() {
		Resource responseBody = new ClassPathResource("ludwig.json", getClass());

		this.mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess(responseBody, MediaType.APPLICATION_JSON));

		@SuppressWarnings("unused")
		Person ludwig = this.restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		// hotel.getId() == 42
		// hotel.getName().equals("Holiday Inn")

		this.mockServer.verify();
	}

	@Test
	void verify() {
		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("1", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("2", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("4", MediaType.TEXT_PLAIN));

		this.mockServer.expect(requestTo("/number")).andExpect(method(HttpMethod.GET))
			.andRespond(withSuccess("8", MediaType.TEXT_PLAIN));

		@SuppressWarnings("unused")
		String result1 = this.restClient.get().uri("/number").retrieve().body(String.class);
		// result1 == "1"

		@SuppressWarnings("unused")
		String result2 = this.restClient.get().uri("/number").retrieve().body(String.class);
		// result == "2"

		try {
			this.mockServer.verify();
		}
		catch (AssertionError error) {
			assertThat(error.getMessage()).as(error.getMessage()).contains("2 unsatisfied expectation(s)");
		}
	}

	@Test // SPR-14694
	void repeatedAccessToResponseViaResource() {
		Resource resource = new ClassPathResource("ludwig.json", getClass());

		RestClient.Builder clientBuilder = RestClient.builder().requestInterceptor(new ContentInterceptor(resource));
		MockRestServiceServer mockServer = MockRestServiceServer.bindTo(clientBuilder)
				.ignoreExpectOrder(true)
				.bufferContent()  // enable repeated reads of response body
				.build();
		RestClient restClient = clientBuilder.build();

		mockServer.expect(requestTo("/composers/42")).andExpect(method(HttpMethod.GET))
				.andRespond(withSuccess(resource, MediaType.APPLICATION_JSON));

		restClient.get().uri("/composers/{id}", 42).retrieve().body(Person.class);

		mockServer.verify();
	}


	private static class ContentInterceptor implements ClientHttpRequestInterceptor {

		private final Resource resource;


		private ContentInterceptor(Resource resource) {
			this.resource = resource;
		}

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {

			ClientHttpResponse response = execution.execute(request, body);
			byte[] expected = FileCopyUtils.copyToByteArray(this.resource.getInputStream());
			byte[] actual = FileCopyUtils.copyToByteArray(response.getBody());
			assertThat(new String(actual)).isEqualTo(new String(expected));
			return response;
		}
	}

}
