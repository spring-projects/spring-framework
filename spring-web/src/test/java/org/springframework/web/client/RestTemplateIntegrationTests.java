/*
 * Copyright 2002-2024 the original author or authors.
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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import org.junit.jupiter.api.Named;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.http.client.JettyClientHttpRequestFactory;
import org.springframework.http.client.ReactorNettyClientRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.converter.FormHttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonValue;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Named.named;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.MediaType.MULTIPART_MIXED;

/**
 * Integration tests for {@link RestTemplate}.
 *
 * <h3>Logging configuration for {@code MockWebServer}</h3>
 *
 * <p>In order for our log4j2 configuration to be used in an IDE, you must
 * set the following system property before running any tests &mdash; for
 * example, in <em>Run Configurations</em> in Eclipse.
 *
 * <pre class="code">
 * -Djava.util.logging.manager=org.apache.logging.log4j.jul.LogManager
 * </pre>
 *
 * @author Arjen Poutsma
 * @author Brian Clozel
 * @author Sam Brannen
 */
class RestTemplateIntegrationTests extends AbstractMockWebServerTests {

	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.METHOD)
	@ParameterizedTest(name = "[{index}] {0}")
	@MethodSource("clientHttpRequestFactories")
	@interface ParameterizedRestTemplateTest {
	}

	@SuppressWarnings("removal")
	static Stream<Named<ClientHttpRequestFactory>> clientHttpRequestFactories() {
		return Stream.of(
			named("JDK HttpURLConnection", new SimpleClientHttpRequestFactory()),
			named("HttpComponents", new HttpComponentsClientHttpRequestFactory()),
			named("OkHttp", new org.springframework.http.client.OkHttp3ClientHttpRequestFactory()),
			named("Jetty", new JettyClientHttpRequestFactory()),
			named("JDK HttpClient", new JdkClientHttpRequestFactory()),
			named("Reactor Netty", new ReactorNettyClientRequestFactory())
		);
	}


	private RestTemplate template;

	private ClientHttpRequestFactory clientHttpRequestFactory;


	/**
	 * Custom JUnit Jupiter extension that handles exceptions thrown by test methods.
	 *
	 * <p>If the test method throws an {@link HttpServerErrorException}, this
	 * extension will throw an {@link AssertionError} that wraps the
	 * {@code HttpServerErrorException} using the
	 * {@link HttpServerErrorException#getResponseBodyAsString() response body}
	 * as the failure message.
	 *
	 * <p>This mechanism provides an actually meaningful failure message if the
	 * test fails due to an {@code AssertionError} on the server.
	 */
	@RegisterExtension
	TestExecutionExceptionHandler serverErrorToAssertionErrorConverter = (context, throwable) -> {
		if (throwable instanceof HttpServerErrorException ex) {
			String responseBody = ex.getResponseBodyAsString();
			String prefix = AssertionError.class.getName() + ": ";
			if (responseBody.startsWith(prefix)) {
				responseBody = responseBody.substring(prefix.length());
			}
			throw new AssertionError(responseBody, ex);
		}
		// Else throw as-is in order to comply with the contract of TestExecutionExceptionHandler.
		throw throwable;
	};


	private void setUpClient(ClientHttpRequestFactory clientHttpRequestFactory) {
		this.clientHttpRequestFactory = clientHttpRequestFactory;
		this.template = new RestTemplate(this.clientHttpRequestFactory);
	}

	@ParameterizedRestTemplateTest
	void getString(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.getForObject(baseUrl + "/{method}", String.class, "get");
		assertThat(s).as("Invalid content").isEqualTo(helloWorld);
	}

	@ParameterizedRestTemplateTest
	void getEntity(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/{method}", String.class, "get");
		assertThat(entity.getBody()).as("Invalid content").isEqualTo(helloWorld);
		assertThat(entity.getHeaders().isEmpty()).as("No headers").isFalse();
		assertThat(entity.getHeaders().getContentType()).as("Invalid content-type").isEqualTo(textContentType);
		assertThat(entity.getStatusCode()).as("Invalid status code").isEqualTo(HttpStatus.OK);
	}

	@ParameterizedRestTemplateTest
	void getNoResponse(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.getForObject(baseUrl + "/get/nothing", String.class);
		assertThat(s).as("Invalid content").isNull();
	}

	@ParameterizedRestTemplateTest
	void getNoContentTypeHeader(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		byte[] bytes = template.getForObject(baseUrl + "/get/nocontenttype", byte[].class);
		assertThat(bytes).as("Invalid content").isEqualTo(helloWorld.getBytes(StandardCharsets.UTF_8));
	}

	@ParameterizedRestTemplateTest
	void getNoContent(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.getForObject(baseUrl + "/status/nocontent", String.class);
		assertThat(s).as("Invalid content").isNull();

		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/nocontent", String.class);
		assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(entity.getBody()).as("Invalid content").isNull();
	}

	@ParameterizedRestTemplateTest
	void getNotModified(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.getForObject(baseUrl + "/status/notmodified", String.class);
		assertThat(s).as("Invalid content").isNull();

		ResponseEntity<String> entity = template.getForEntity(baseUrl + "/status/notmodified", String.class);
		assertThat(entity.getStatusCode()).as("Invalid response code").isEqualTo(HttpStatus.NOT_MODIFIED);
		assertThat(entity.getBody()).as("Invalid content").isNull();
	}

	@ParameterizedRestTemplateTest
	void postForLocation(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		URI location = template.postForLocation(baseUrl + "/{method}", helloWorld, "post");
		assertThat(location).as("Invalid location").isEqualTo(URI.create(baseUrl + "/post/1"));
	}

	@ParameterizedRestTemplateTest
	void postForLocationEntity(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("text", "plain", StandardCharsets.ISO_8859_1));
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, entityHeaders);
		URI location = template.postForLocation(baseUrl + "/{method}", entity, "post");
		assertThat(location).as("Invalid location").isEqualTo(URI.create(baseUrl + "/post/1"));
	}

	@ParameterizedRestTemplateTest
	void postForObject(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.postForObject(baseUrl + "/{method}", helloWorld, String.class, "post");
		assertThat(s).as("Invalid content").isEqualTo(helloWorld);
	}

	@ParameterizedRestTemplateTest
	void patchForObject(ClientHttpRequestFactory clientHttpRequestFactory) {
		assumeFalse(clientHttpRequestFactory instanceof SimpleClientHttpRequestFactory,
				"HttpURLConnection does not support the PATCH method");

		setUpClient(clientHttpRequestFactory);

		String s = template.patchForObject(baseUrl + "/{method}", helloWorld, String.class, "patch");
		assertThat(s).as("Invalid content").isEqualTo(helloWorld);
	}

	@ParameterizedRestTemplateTest
	void notFound(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String url = baseUrl + "/status/notfound";
		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				template.execute(url, HttpMethod.GET, null, null))
			.satisfies(ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
				assertThat(ex.getStatusText()).isNotNull();
				assertThat(ex.getResponseBodyAsString()).isNotNull();
				assertThat(ex.getMessage()).containsSubsequence("404", "on GET request for \"" + url + "\": [no body]");
				assumeFalse(clientHttpRequestFactory instanceof JdkClientHttpRequestFactory, "JDK HttpClient does not expose status text");
				assertThat(ex.getMessage()).isEqualTo("404 Client Error on GET request for \"" + url + "\": [no body]");
			});
	}

	@ParameterizedRestTemplateTest
	void badRequest(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String url = baseUrl + "/status/badrequest";
		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				template.execute(url, HttpMethod.GET, null, null))
			.satisfies(ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
				assertThat(ex.getMessage()).containsSubsequence("400", "on GET request for \""+url+ "\": [no body]");
				assumeFalse(clientHttpRequestFactory instanceof JdkClientHttpRequestFactory, "JDK HttpClient does not expose status text");
				assertThat(ex.getMessage()).isEqualTo("400 Client Error on GET request for \""+url+ "\": [no body]");
			});
	}

	@ParameterizedRestTemplateTest
	void serverError(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String url = baseUrl + "/status/server";
		assertThatExceptionOfType(HttpServerErrorException.class).isThrownBy(() ->
				template.execute(url, HttpMethod.GET, null, null))
			.satisfies(ex -> {
				assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
				assertThat(ex.getStatusText()).isNotNull();
				assertThat(ex.getResponseBodyAsString()).isNotNull();
				assertThat(ex.getMessage()).containsSubsequence("500", "on GET request for \"" + url + "\": [no body]");
				assumeFalse(clientHttpRequestFactory instanceof JdkClientHttpRequestFactory, "JDK HttpClient does not expose status text");
				assertThat(ex.getMessage()).isEqualTo("500 Server Error on GET request for \"" + url + "\": [no body]");
			});
	}

	@ParameterizedRestTemplateTest
	void optionsForAllow(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		Set<HttpMethod> allowed = template.optionsForAllow(URI.create(baseUrl + "/get"));
		assertThat(allowed).as("Invalid response").isEqualTo(Set.of(HttpMethod.GET, HttpMethod.OPTIONS, HttpMethod.HEAD, HttpMethod.TRACE));
	}

	@ParameterizedRestTemplateTest
	void uri(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String result = template.getForObject(baseUrl + "/uri/{query}", String.class, "Z\u00fcrich");
		assertThat(result).as("Invalid request URI").isEqualTo("/uri/Z%C3%BCrich");

		result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "foo@bar");
		assertThat(result).as("Invalid request URI").isEqualTo("/uri/query=foo@bar");

		result = template.getForObject(baseUrl + "/uri/query={query}", String.class, "T\u014dky\u014d");
		assertThat(result).as("Invalid request URI").isEqualTo("/uri/query=T%C5%8Dky%C5%8D");
	}

	@ParameterizedRestTemplateTest
	void multipartFormData(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		template.postForLocation(baseUrl + "/multipartFormData", createMultipartParts());
	}

	@ParameterizedRestTemplateTest
	void multipartMixed(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MULTIPART_MIXED);
		template.postForLocation(baseUrl + "/multipartMixed", new HttpEntity<>(createMultipartParts(), requestHeaders));
	}

	@ParameterizedRestTemplateTest
	void multipartRelated(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		addSupportedMediaTypeToFormHttpMessageConverter(MULTIPART_RELATED);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.setContentType(MULTIPART_RELATED);
		template.postForLocation(baseUrl + "/multipartRelated", new HttpEntity<>(createMultipartParts(), requestHeaders));
	}

	private MultiValueMap<String, Object> createMultipartParts() {
		MultiValueMap<String, Object> parts = new LinkedMultiValueMap<>();
		parts.add("name 1", "value 1");
		parts.add("name 2", "value 2+1");
		parts.add("name 2", "value 2+2");
		Resource logo = new ClassPathResource("/org/springframework/http/converter/logo.jpg");
		parts.add("logo", logo);
		return parts;
	}

	private void addSupportedMediaTypeToFormHttpMessageConverter(MediaType mediaType) {
		this.template.getMessageConverters().stream()
				.filter(FormHttpMessageConverter.class::isInstance)
				.map(FormHttpMessageConverter.class::cast)
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("Failed to find FormHttpMessageConverter"))
				.addSupportedMediaTypes(mediaType);
	}

	@ParameterizedRestTemplateTest
	void form(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("name 1", "value 1");
		form.add("name 2", "value 2+1");
		form.add("name 2", "value 2+2");

		template.postForLocation(baseUrl + "/form", form);
	}

	@ParameterizedRestTemplateTest
	void exchangeGet(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		HttpEntity<String> requestEntity = new HttpEntity<>(requestHeaders);
		ResponseEntity<String> response =
				template.exchange(baseUrl + "/{method}", HttpMethod.GET, requestEntity, String.class, "get");
		assertThat(response.getBody()).as("Invalid content").isEqualTo(helloWorld);
	}

	@ParameterizedRestTemplateTest
	void exchangePost(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders requestHeaders = new HttpHeaders();
		requestHeaders.set("MyHeader", "MyValue");
		requestHeaders.setContentType(MediaType.TEXT_PLAIN);
		HttpEntity<String> entity = new HttpEntity<>(helloWorld, requestHeaders);
		HttpEntity<Void> result = template.exchange(baseUrl + "/{method}", POST, entity, Void.class, "post");
		assertThat(result.getHeaders().getLocation()).as("Invalid location").isEqualTo(URI.create(baseUrl + "/post/1"));
		assertThat(result.hasBody()).isFalse();
	}

	@ParameterizedRestTemplateTest
	void jsonPostForObject(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
		MySampleBean bean = new MySampleBean();
		bean.setWith1("with");
		bean.setWith2("with");
		bean.setWithout("without");
		HttpEntity<MySampleBean> entity = new HttpEntity<>(bean, entityHeaders);
		String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
		assertThat(s).contains("\"with1\":\"with\"");
		assertThat(s).contains("\"with2\":\"with\"");
		assertThat(s).contains("\"without\":\"without\"");
	}

	@ParameterizedRestTemplateTest
	void jsonPostForObjectWithJacksonView(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		HttpHeaders entityHeaders = new HttpHeaders();
		entityHeaders.setContentType(new MediaType("application", "json", StandardCharsets.UTF_8));
		MySampleBean bean = new MySampleBean("with", "with", "without");
		MappingJacksonValue jacksonValue = new MappingJacksonValue(bean);
		jacksonValue.setSerializationView(MyJacksonView1.class);
		HttpEntity<MappingJacksonValue> entity = new HttpEntity<>(jacksonValue, entityHeaders);
		String s = template.postForObject(baseUrl + "/jsonpost", entity, String.class);
		assertThat(s).contains("\"with1\":\"with\"");
		assertThat(s).doesNotContain("\"with2\":\"with\"");
		assertThat(s).doesNotContain("\"without\":\"without\"");
	}

	@ParameterizedRestTemplateTest  // SPR-12123
	void serverPort(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		String s = template.getForObject("http://localhost:{port}/get", String.class, port);
		assertThat(s).as("Invalid content").isEqualTo(helloWorld);
	}

	@ParameterizedRestTemplateTest  // SPR-13154
	void jsonPostForObjectWithJacksonTypeInfoList(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		List<ParentClass> list = new ArrayList<>();
		list.add(new Foo("foo"));
		list.add(new Bar("bar"));
		ParameterizedTypeReference<?> typeReference = new ParameterizedTypeReference<List<ParentClass>>() {};
		RequestEntity<List<ParentClass>> entity = RequestEntity
				.post(URI.create(baseUrl + "/jsonpost"))
				.contentType(new MediaType("application", "json", StandardCharsets.UTF_8))
				.body(list, typeReference.getType());
		String content = template.exchange(entity, String.class).getBody();
		assertThat(content).contains("\"type\":\"foo\"");
		assertThat(content).contains("\"type\":\"bar\"");
	}

	@ParameterizedRestTemplateTest  // SPR-15015
	void postWithoutBody(ClientHttpRequestFactory clientHttpRequestFactory) {
		setUpClient(clientHttpRequestFactory);

		assertThat(template.postForObject(baseUrl + "/jsonpost", null, String.class)).isNull();
	}


	public interface MyJacksonView1 {}

	public interface MyJacksonView2 {}


	public static class MySampleBean {

		@JsonView(MyJacksonView1.class)
		private String with1;

		@JsonView(MyJacksonView2.class)
		private String with2;

		private String without;

		private MySampleBean() {
		}

		private MySampleBean(String with1, String with2, String without) {
			this.with1 = with1;
			this.with2 = with2;
			this.without = without;
		}

		public String getWith1() {
			return with1;
		}

		public void setWith1(String with1) {
			this.with1 = with1;
		}

		public String getWith2() {
			return with2;
		}

		public void setWith2(String with2) {
			this.with2 = with2;
		}

		public String getWithout() {
			return without;
		}

		public void setWithout(String without) {
			this.without = without;
		}
	}


	@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
	public static class ParentClass {

		private String parentProperty;

		public ParentClass() {
		}

		public ParentClass(String parentProperty) {
			this.parentProperty = parentProperty;
		}

		public String getParentProperty() {
			return parentProperty;
		}

		public void setParentProperty(String parentProperty) {
			this.parentProperty = parentProperty;
		}
	}


	@JsonTypeName("foo")
	public static class Foo extends ParentClass {

		public Foo() {
		}

		public Foo(String parentProperty) {
			super(parentProperty);
		}
	}


	@JsonTypeName("bar")
	public static class Bar extends ParentClass {

		public Bar() {
		}

		public Bar(String parentProperty) {
			super(parentProperty);
		}
	}

}
