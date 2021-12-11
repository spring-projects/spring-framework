/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.web.client;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInitializer;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.GenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.util.StreamUtils;
import org.springframework.web.util.DefaultUriBuilderFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.MediaType.parseMediaType;

/**
 * Unit tests for {@link RestTemplate}.
 *
 * @author Arjen Poutsma
 * @author Rossen Stoyanchev
 * @author Brian Clozel
 * @author Sam Brannen
 */
@SuppressWarnings("unchecked")
class RestTemplateTests {

	private final ClientHttpRequestFactory requestFactory = mock(ClientHttpRequestFactory.class);

	private final ClientHttpRequest request = mock(ClientHttpRequest.class);

	private final ClientHttpResponse response = mock(ClientHttpResponse.class);

	private final ResponseErrorHandler errorHandler = mock(ResponseErrorHandler.class);

	@SuppressWarnings("rawtypes")
	private final HttpMessageConverter converter = mock(HttpMessageConverter.class);

	private final RestTemplate template = new RestTemplate(Collections.singletonList(converter));


	@BeforeEach
	void setup() {
		template.setRequestFactory(requestFactory);
		template.setErrorHandler(errorHandler);
	}

	@Test
	void constructorPreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RestTemplate((List<HttpMessageConverter<?>>) null))
				.withMessage("At least one HttpMessageConverter is required");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new RestTemplate(Arrays.asList(null, this.converter)))
				.withMessage("The HttpMessageConverter list must not contain null elements");
	}

	@Test
	void setMessageConvertersPreconditions() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> template.setMessageConverters((List<HttpMessageConverter<?>>) null))
				.withMessage("At least one HttpMessageConverter is required");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> template.setMessageConverters(Arrays.asList(null, this.converter)))
				.withMessage("The HttpMessageConverter list must not contain null elements");
	}

	@Test
	void varArgsTemplateVariables() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/21");
		mockResponseStatus(HttpStatus.OK);

		template.execute("https://example.com/hotels/{hotel}/bookings/{booking}", GET,
				null, null, "42", "21");

		verify(response).close();
	}

	@Test
	void varArgsNullTemplateVariable() throws Exception {
		mockSentRequest(GET, "https://example.com/-foo");
		mockResponseStatus(HttpStatus.OK);

		template.execute("https://example.com/{first}-{last}", GET, null, null, null, "foo");

		verify(response).close();
	}

	@Test
	void mapTemplateVariables() throws Exception {
		mockSentRequest(GET, "https://example.com/hotels/42/bookings/42");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = Collections.singletonMap("hotel", "42");
		template.execute("https://example.com/hotels/{hotel}/bookings/{hotel}", GET, null, null, vars);

		verify(response).close();
	}

	@Test
	void mapNullTemplateVariable() throws Exception {
		mockSentRequest(GET, "https://example.com/-foo");
		mockResponseStatus(HttpStatus.OK);

		Map<String, String> vars = new HashMap<>(2);
		vars.put("first", null);
		vars.put("last", "foo");
		template.execute("https://example.com/{first}-{last}", GET, null, null, vars);

		verify(response).close();
	}

	@Test  // SPR-15201
	void uriTemplateWithTrailingSlash() throws Exception {
		String url = "https://example.com/spring/";
		mockSentRequest(GET, url);
		mockResponseStatus(HttpStatus.OK);

		template.execute(url, GET, null, null);

		verify(response).close();
	}

	@Test
	void errorHandling() throws Exception {
		String url = "https://example.com";
		mockSentRequest(GET, url);
		mockResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR);
		willThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR))
				.given(errorHandler).handleError(new URI(url), GET, response);

		assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
				template.execute(url, GET, null, null));

		verify(response).close();
	}

	@Test
	void getForObject() throws Exception {
		String expected = "Hello World";
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		mockTextResponseBody("Hello World");

		String result = template.getForObject("https://example.com", String.class);
		assertThat(result).as("Invalid GET result").isEqualTo(expected);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);

		verify(response).close();
	}

	@Test
	void getUnsupportedMediaType() throws Exception {
		mockSentRequest(GET, "https://example.com/resource");
		mockResponseStatus(HttpStatus.OK);

		given(converter.canRead(String.class, null)).willReturn(true);
		MediaType supportedMediaType = new MediaType("foo", "bar");
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(supportedMediaType));

		MediaType barBaz = new MediaType("bar", "baz");
		mockResponseBody("Foo", new MediaType("bar", "baz"));
		given(converter.canRead(String.class, barBaz)).willReturn(false);

		assertThatExceptionOfType(RestClientException.class).isThrownBy(() ->
				template.getForObject("https://example.com/{p}", String.class, "resource"));

		verify(response).close();
	}

	@Test
	void requestAvoidsDuplicateAcceptHeaderValues() throws Exception {
		HttpMessageConverter<?> firstConverter = mock(HttpMessageConverter.class);
		given(firstConverter.canRead(any(), any())).willReturn(true);
		given(firstConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		HttpMessageConverter<?> secondConverter = mock(HttpMessageConverter.class);
		given(secondConverter.canRead(any(), any())).willReturn(true);
		given(secondConverter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "https://example.com/", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		mockTextResponseBody("Hello World");

		template.setMessageConverters(Arrays.asList(firstConverter, secondConverter));
		template.getForObject("https://example.com/", String.class);

		assertThat(requestHeaders.getAccept().size()).as("Sent duplicate Accept header values").isEqualTo(1);
	}

	@Test
	void getForEntity() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(GET, "https://example.com", requestHeaders);
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		String expected = "Hello World";
		mockTextResponseBody(expected);

		ResponseEntity<String> result = template.getForEntity("https://example.com", String.class);
		assertThat(result.getBody()).as("Invalid GET result").isEqualTo(expected);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(result.getHeaders().getContentType()).as("Invalid Content-Type header").isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

		verify(response).close();
	}

	@Test
	void getForObjectWithCustomUriTemplateHandler() throws Exception {
		DefaultUriBuilderFactory uriTemplateHandler = new DefaultUriBuilderFactory();
		template.setUriTemplateHandler(uriTemplateHandler);
		mockSentRequest(GET, "https://example.com/hotels/1/pic/pics%2Flogo.png/size/150x150");
		mockResponseStatus(HttpStatus.OK);
		given(response.getHeaders()).willReturn(new HttpHeaders());
		given(response.getBody()).willReturn(StreamUtils.emptyInput());

		Map<String, String> uriVariables = new HashMap<>(2);
		uriVariables.put("hotel", "1");
		uriVariables.put("publicpath", "pics/logo.png");
		uriVariables.put("scale", "150x150");

		String url = "https://example.com/hotels/{hotel}/pic/{publicpath}/size/{scale}";
		template.getForObject(url, String.class, uriVariables);

		verify(response).close();
	}

	@Test
	void headForHeaders() throws Exception {
		mockSentRequest(HEAD, "https://example.com");
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		given(response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders result = template.headForHeaders("https://example.com");

		assertThat(result).as("Invalid headers returned").isSameAs(responseHeaders);

		verify(response).close();
	}

	@Test
	void postForLocation() throws Exception {
		mockSentRequest(POST, "https://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		String helloWorld = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("https://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);

		URI result = template.postForLocation("https://example.com", helloWorld);
		assertThat(result).as("Invalid POST result").isEqualTo(expected);

		verify(response).close();
	}

	@Test
	void postForLocationEntityContentType() throws Exception {
		mockSentRequest(POST, "https://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);

		String helloWorld = "Hello World";
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("https://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);

		URI result = template.postForLocation("https://example.com", entity);
		assertThat(result).as("Invalid POST result").isEqualTo(expected);

		verify(response).close();
	}

	@Test
	void postForLocationEntityCustomHeader() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		URI expected = new URI("https://example.com/hotels");
		responseHeaders.setLocation(expected);
		given(response.getHeaders()).willReturn(responseHeaders);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);

		URI result = template.postForLocation("https://example.com", entity);
		assertThat(result).as("Invalid POST result").isEqualTo(expected);
		assertThat(requestHeaders.getFirst("MyHeader")).as("No custom header set").isEqualTo("MyValue");

		verify(response).close();
	}

	@Test
	void postForLocationNoLocation() throws Exception {
		mockSentRequest(POST, "https://example.com");
		mockTextPlainHttpMessageConverter();
		mockResponseStatus(HttpStatus.OK);

		URI result = template.postForLocation("https://example.com", "Hello World");
		assertThat(result).as("Invalid POST result").isNull();

		verify(response).close();
	}

	@Test
	void postForLocationNull() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		template.postForLocation("https://example.com", null);
		assertThat(requestHeaders.getContentLength()).as("Invalid content length").isEqualTo(0);

		verify(response).close();
	}

	@Test
	void postForObject() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		String result = template.postForObject("https://example.com", "Hello World", String.class);
		assertThat(result).as("Invalid POST result").isEqualTo(expected);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);

		verify(response).close();
	}

	@Test
	void postForEntity() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		ResponseEntity<String> result = template.postForEntity("https://example.com", "Hello World", String.class);
		assertThat(result.getBody()).as("Invalid POST result").isEqualTo(expected);
		assertThat(result.getHeaders().getContentType()).as("Invalid Content-Type").isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

		verify(response).close();
	}

	@Test
	void postForObjectNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(StreamUtils.emptyInput());
		given(converter.read(String.class, response)).willReturn(null);

		String result = template.postForObject("https://example.com", null, String.class);
		assertThat(result).as("Invalid POST result").isNull();
		assertThat(requestHeaders.getContentLength()).as("Invalid content length").isEqualTo(0);

		verify(response).close();
	}

	@Test
	void postForEntityNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(StreamUtils.emptyInput());
		given(converter.read(String.class, response)).willReturn(null);

		ResponseEntity<String> result = template.postForEntity("https://example.com", null, String.class);
		assertThat(result.hasBody()).as("Invalid POST result").isFalse();
		assertThat(result.getHeaders().getContentType()).as("Invalid Content-Type").isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(requestHeaders.getContentLength()).as("Invalid content length").isEqualTo(0);
		assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

		verify(response).close();
	}

	@Test
	void put() throws Exception {
		mockTextPlainHttpMessageConverter();
		mockSentRequest(PUT, "https://example.com");
		mockResponseStatus(HttpStatus.OK);

		template.put("https://example.com", "Hello World");

		verify(response).close();
	}

	@Test
	void putNull() throws Exception {
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PUT, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		template.put("https://example.com", null);
		assertThat(requestHeaders.getContentLength()).as("Invalid content length").isEqualTo(0);

		verify(response).close();
	}

	@Test // gh-23740
	void headerAcceptAllOnPut() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setResponseCode(500).setBody("internal server error"));
			server.start();
			template.setRequestFactory(new SimpleClientHttpRequestFactory());
			template.put(server.url("/internal/server/error").uri(), null);
			assertThat(server.takeRequest().getHeader("Accept")).isEqualTo("*/*");
		}
	}

	@Test // gh-23740
	void keepGivenAcceptHeaderOnPut() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setResponseCode(500).setBody("internal server error"));
			server.start();
			HttpHeaders headers = new HttpHeaders();
			headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
			HttpEntity<String> entity = new HttpEntity<>(null, headers);
			template.setRequestFactory(new SimpleClientHttpRequestFactory());
			template.exchange(server.url("/internal/server/error").uri(), PUT, entity, Void.class);

			RecordedRequest request = server.takeRequest();

			final List<List<String>> accepts = request.getHeaders().toMultimap().entrySet().stream()
					.filter(entry -> entry.getKey().equalsIgnoreCase("accept"))
					.map(Entry::getValue)
					.collect(Collectors.toList());

			assertThat(accepts).hasSize(1);
			assertThat(accepts.get(0)).hasSize(1);
			assertThat(accepts.get(0).get(0)).isEqualTo("application/json");
		}
	}

	@Test
	void patchForObject() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PATCH, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody("42", MediaType.TEXT_PLAIN);

		String result = template.patchForObject("https://example.com", "Hello World", String.class);
		assertThat(result).as("Invalid POST result").isEqualTo(expected);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);

		verify(response).close();
	}

	@Test
	void patchForObjectNull() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(PATCH, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(StreamUtils.emptyInput());

		String result = template.patchForObject("https://example.com", null, String.class);
		assertThat(result).as("Invalid POST result").isNull();
		assertThat(requestHeaders.getContentLength()).as("Invalid content length").isEqualTo(0);

		verify(response).close();
	}

	@Test
	void delete() throws Exception {
		mockSentRequest(DELETE, "https://example.com");
		mockResponseStatus(HttpStatus.OK);

		template.delete("https://example.com");

		verify(response).close();
	}

	@Test // gh-23740
	void headerAcceptAllOnDelete() throws Exception {
		try (MockWebServer server = new MockWebServer()) {
			server.enqueue(new MockResponse().setResponseCode(500).setBody("internal server error"));
			server.start();
			template.setRequestFactory(new SimpleClientHttpRequestFactory());
			template.delete(server.url("/internal/server/error").uri());
			assertThat(server.takeRequest().getHeader("Accept")).isEqualTo("*/*");
		}
	}

	@Test
	void optionsForAllow() throws Exception {
		mockSentRequest(OPTIONS, "https://example.com");
		mockResponseStatus(HttpStatus.OK);
		HttpHeaders responseHeaders = new HttpHeaders();
		Set<HttpMethod> expected = Set.of(GET, POST);
		responseHeaders.setAllow(expected);
		given(response.getHeaders()).willReturn(responseHeaders);

		Set<HttpMethod> result = template.optionsForAllow("https://example.com");
		assertThat(result).as("Invalid OPTIONS result").isEqualTo(expected);

		verify(response).close();
	}

	@Test  // SPR-9325, SPR-13860
	void ioException() throws Exception {
		String url = "https://example.com/resource?access_token=123";
		mockSentRequest(GET, url);
		mockHttpMessageConverter(new MediaType("foo", "bar"), String.class);
		given(request.execute()).willThrow(new IOException("Socket failure"));

		assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() ->
				template.getForObject(url, String.class))
			.withMessage("I/O error on GET request for \"https://example.com/resource\": " +
							"Socket failure; nested exception is java.io.IOException: Socket failure");
	}

	@Test  // SPR-15900
	void ioExceptionWithEmptyQueryString() throws Exception {
		// https://example.com/resource?
		URI uri = new URI("https", "example.com", "/resource", "", null);

		given(converter.canRead(String.class, null)).willReturn(true);
		given(converter.getSupportedMediaTypes()).willReturn(Collections.singletonList(parseMediaType("foo/bar")));
		given(requestFactory.createRequest(uri, GET)).willReturn(request);
		given(request.getHeaders()).willReturn(new HttpHeaders());
		given(request.execute()).willThrow(new IOException("Socket failure"));

		assertThatExceptionOfType(ResourceAccessException.class).isThrownBy(() ->
				template.getForObject(uri, String.class))
			.withMessage("I/O error on GET request for \"https://example.com/resource\": " +
					"Socket failure; nested exception is java.io.IOException: Socket failure");
	}

	@Test
	void exchange() throws Exception {
		mockTextPlainHttpMessageConverter();
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);
		String expected = "42";
		mockResponseBody(expected, MediaType.TEXT_PLAIN);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);
		ResponseEntity<String> result = template.exchange("https://example.com", POST, entity, String.class);
		assertThat(result.getBody()).as("Invalid POST result").isEqualTo(expected);
		assertThat(result.getHeaders().getContentType()).as("Invalid Content-Type").isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(requestHeaders.getFirst("MyHeader")).as("Invalid custom header").isEqualTo("MyValue");
		assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

		verify(response).close();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void exchangeParameterizedType() throws Exception {
		GenericHttpMessageConverter converter = mock(GenericHttpMessageConverter.class);
		template.setMessageConverters(Collections.<HttpMessageConverter<?>>singletonList(converter));
		ParameterizedTypeReference<List<Integer>> intList = new ParameterizedTypeReference<List<Integer>>() {};
		given(converter.canRead(intList.getType(), null, null)).willReturn(true);
		given(converter.getSupportedMediaTypes(any())).willReturn(Collections.singletonList(MediaType.TEXT_PLAIN));
		given(converter.canWrite(String.class, String.class, null)).willReturn(true);

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		List<Integer> expected = Collections.singletonList(42);
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(MediaType.TEXT_PLAIN);
		responseHeaders.setContentLength(10);
		mockResponseStatus(HttpStatus.OK);
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(Integer.toString(42).getBytes()));
		given(converter.canRead(intList.getType(), null, MediaType.TEXT_PLAIN)).willReturn(true);
		given(converter.read(eq(intList.getType()), eq(null), any(HttpInputMessage.class))).willReturn(expected);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<>("Hello World", entityHeaders);
		ResponseEntity<List<Integer>> result = template.exchange("https://example.com", POST, requestEntity, intList);
		assertThat(result.getBody()).as("Invalid POST result").isEqualTo(expected);
		assertThat(result.getHeaders().getContentType()).as("Invalid Content-Type").isEqualTo(MediaType.TEXT_PLAIN);
		assertThat(requestHeaders.getFirst("Accept")).as("Invalid Accept header").isEqualTo(MediaType.TEXT_PLAIN_VALUE);
		assertThat(requestHeaders.getFirst("MyHeader")).as("Invalid custom header").isEqualTo("MyValue");
		assertThat(result.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);

		verify(response).close();
	}

	@Test  // SPR-15066
	void requestInterceptorCanAddExistingHeaderValueWithoutBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		template.setInterceptors(Collections.singletonList(interceptor));

		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<Void> entity = new HttpEntity<>(null, entityHeaders);
		template.exchange("https://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader")).contains("MyEntityValue", "MyInterceptorValue");

		verify(response).close();
	}

	@Test  // SPR-15066
	void requestInterceptorCanAddExistingHeaderValueWithBody() throws Exception {
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		template.setInterceptors(Collections.singletonList(interceptor));

		MediaType contentType = MediaType.TEXT_PLAIN;
		given(converter.canWrite(String.class, contentType)).willReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		entityHeaders.add("MyHeader", "MyEntityValue");
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);
		template.exchange("https://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader")).contains("MyEntityValue", "MyInterceptorValue");

		verify(response).close();
	}

	@Test
	void clientHttpRequestInitializerAndRequestInterceptorAreBothApplied() throws Exception {
		ClientHttpRequestInitializer initializer = request ->
			request.getHeaders().add("MyHeader", "MyInitializerValue");
		ClientHttpRequestInterceptor interceptor = (request, body, execution) -> {
			request.getHeaders().add("MyHeader", "MyInterceptorValue");
			return execution.execute(request, body);
		};
		template.setClientHttpRequestInitializers(Collections.singletonList(initializer));
		template.setInterceptors(Collections.singletonList(interceptor));

		MediaType contentType = MediaType.TEXT_PLAIN;
		given(converter.canWrite(String.class, contentType)).willReturn(true);
		HttpHeaders requestHeaders = new HttpHeaders();
		mockSentRequest(POST, "https://example.com", requestHeaders);
		mockResponseStatus(HttpStatus.OK);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(contentType);
		HttpEntity<String> entity = new HttpEntity<>("Hello World", entityHeaders);
		template.exchange("https://example.com", POST, entity, Void.class);
		assertThat(requestHeaders.get("MyHeader")).contains("MyInterceptorValue", "MyInitializerValue");

		verify(response).close();
	}

	private void mockSentRequest(HttpMethod method, String uri) throws Exception {
		mockSentRequest(method, uri, new HttpHeaders());
	}

	private void mockSentRequest(HttpMethod method, String uri, HttpHeaders requestHeaders) throws Exception {
		given(requestFactory.createRequest(new URI(uri), method)).willReturn(request);
		given(request.getHeaders()).willReturn(requestHeaders);
	}

	private void mockResponseStatus(HttpStatus responseStatus) throws Exception {
		given(request.execute()).willReturn(response);
		given(errorHandler.hasError(response)).willReturn(responseStatus.isError());
		given(response.getStatusCode()).willReturn(responseStatus);
		given(response.getRawStatusCode()).willReturn(responseStatus.value());
		given(response.getStatusText()).willReturn(responseStatus.getReasonPhrase());
	}

	private void mockTextPlainHttpMessageConverter() {
		mockHttpMessageConverter(MediaType.TEXT_PLAIN, String.class);
	}

	private void mockHttpMessageConverter(MediaType mediaType, Class<?> type) {
		given(converter.canRead(type, null)).willReturn(true);
		given(converter.canRead(type, mediaType)).willReturn(true);
		given(converter.getSupportedMediaTypes(type)).willReturn(Collections.singletonList(mediaType));
		given(converter.canRead(type, mediaType)).willReturn(true);
		given(converter.canWrite(type, null)).willReturn(true);
		given(converter.canWrite(type, mediaType)).willReturn(true);
	}

	private void mockTextResponseBody(String expectedBody) throws Exception {
		mockResponseBody(expectedBody, MediaType.TEXT_PLAIN);
	}

	private void mockResponseBody(String expectedBody, MediaType mediaType) throws Exception {
		HttpHeaders responseHeaders = new HttpHeaders();
		responseHeaders.setContentType(mediaType);
		responseHeaders.setContentLength(expectedBody.length());
		given(response.getHeaders()).willReturn(responseHeaders);
		given(response.getBody()).willReturn(new ByteArrayInputStream(expectedBody.getBytes()));
		given(converter.read(eq(String.class), any(HttpInputMessage.class))).willReturn(expectedBody);
	}

}
