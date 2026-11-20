/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.docs.integration.restrestclient.create;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.web.client.ApiVersionInserter;
import org.springframework.web.client.RestClient;

public class RestClientCreation {

	void createRestClient() {
		// tag::snippet[]
		RestClient defaultClient = RestClient.create();

		RestClient customClient = RestClient.builder()
				.requestFactory(new HttpComponentsClientHttpRequestFactory())
				.configureMessageConverters(converters -> converters.addCustomConverter(new MyCustomMessageConverter()))
				.baseUrl("https://example.com")
				.defaultUriVariables(Map.of("variable", "foo"))
				.defaultHeader("My-Header", "Foo")
				.defaultCookie("My-Cookie", "Bar")
				.defaultApiVersion("1.2")
				.apiVersionInserter(ApiVersionInserter.useHeader("API-Version"))
				.requestInterceptor(new MyCustomInterceptor())
				.requestInitializer(new MyCustomInitializer())
				.build();
		// end::snippet[]
	}

	private static class MyCustomMessageConverter extends StringHttpMessageConverter {
	}

	private static class MyCustomInterceptor implements ClientHttpRequestInterceptor {

		@Override
		public ClientHttpResponse intercept(HttpRequest request, byte[] body,
				ClientHttpRequestExecution execution) throws IOException {

			return execution.execute(request, body);
		}
	}

	private static class MyCustomInitializer implements ClientHttpRequestInitializer {

		@Override
		public void initialize(ClientHttpRequest request) {
			request.getHeaders().add("My-Header", "My-Value");
		}
	}

}
