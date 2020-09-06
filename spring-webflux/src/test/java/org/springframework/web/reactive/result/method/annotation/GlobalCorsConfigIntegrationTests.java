/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.web.reactive.result.method.annotation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurationSupport;
import org.springframework.web.testfixture.http.server.reactive.bootstrap.HttpServer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 *
 * Integration tests with {@code @RequestMapping} handler methods and global
 * CORS configuration.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class GlobalCorsConfigIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.headers.setOrigin("http://localhost:9000");
	}

	@Override
	protected ApplicationContext initApplicationContext() {
		return new AnnotationConfigApplicationContext(WebConfig.class);
	}

	@Override
	protected RestTemplate initRestTemplate() {
		// JDK default HTTP client disallowed headers like Origin
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	}


	@ParameterizedHttpServerTest
	void actualRequestWithCorsEnabled(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/cors", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getBody()).isEqualTo("cors");
	}

	@ParameterizedHttpServerTest
	void actualRequestWithCorsRejected(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				performGet("/cors-restricted", this.headers, String.class))
			.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@ParameterizedHttpServerTest
	void actualRequestWithoutCorsEnabled(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/welcome", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isNull();
		assertThat(entity.getBody()).isEqualTo("welcome");
	}

	@ParameterizedHttpServerTest
	void actualRequestWithAmbiguousMapping(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE);
		ResponseEntity<String> entity = performGet("/ambiguous", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
	}

	@ParameterizedHttpServerTest
	void preFlightRequestWithCorsEnabled(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<String> entity = performOptions("/cors", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getHeaders().getAccessControlAllowMethods())
				.containsExactly(HttpMethod.GET, HttpMethod.HEAD, HttpMethod.POST);
	}

	@ParameterizedHttpServerTest
	void preFlightRequestWithCorsRejected(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				performOptions("/cors-restricted", this.headers, String.class))
			.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@ParameterizedHttpServerTest
	void preFlightRequestWithoutCorsEnabled(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		assertThatExceptionOfType(HttpClientErrorException.class).isThrownBy(() ->
				performOptions("/welcome", this.headers, String.class))
			.satisfies(ex -> assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
	}

	@ParameterizedHttpServerTest
	void preFlightRequestWithCorsRestricted(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.set(HttpHeaders.ORIGIN, "https://foo");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<String> entity = performOptions("/cors-restricted", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://foo");
		assertThat(entity.getHeaders().getAccessControlAllowMethods())
				.containsExactly(HttpMethod.GET, HttpMethod.POST);
	}

	@ParameterizedHttpServerTest
	void preFlightRequestWithAmbiguousMapping(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<String> entity = performOptions("/ambiguous", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("http://localhost:9000");
		assertThat(entity.getHeaders().getAccessControlAllowMethods()).containsExactly(HttpMethod.GET);
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isEqualTo(true);
		assertThat(entity.getHeaders().get(HttpHeaders.VARY))
				.containsExactly(HttpHeaders.ORIGIN, HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD,
						HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS);
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/GlobalCorsConfigIntegrationTests*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebFluxConfigurationSupport {

		@Override
		protected void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/cors-restricted").allowedOrigins("https://foo").allowedMethods("GET", "POST");
			registry.addMapping("/cors");
			registry.addMapping("/ambiguous").allowedMethods("GET", "POST");
		}
	}

	@RestController @SuppressWarnings("unused")
	static class TestController {

		@GetMapping("/welcome")
		public String welcome() {
			return "welcome";
		}

		@GetMapping("/cors")
		public String cors() {
			return "cors";
		}

		@GetMapping("/cors-restricted")
		public String corsRestricted() {
			return "corsRestricted";
		}

		@GetMapping(value = "/ambiguous", produces = MediaType.TEXT_PLAIN_VALUE)
		public String ambiguous1() {
			return "ambiguous";
		}

		@GetMapping(value = "/ambiguous", produces = MediaType.TEXT_HTML_VALUE)
		public String ambiguous2() {
			return "<p>ambiguous</p>";
		}
	}

}
