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

import java.util.Properties;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.server.reactive.bootstrap.HttpServer;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.EnableWebFlux;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests with {@code @CrossOrigin} and {@code @RequestMapping}
 * annotated handler methods.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 * @author Sam Brannen
 */
class CrossOriginAnnotationIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private final HttpHeaders headers = new HttpHeaders();


	@Override
	protected void startServer(HttpServer httpServer) throws Exception {
		super.startServer(httpServer);
		this.headers.setOrigin("https://site1.com");
	}

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		Properties props = new Properties();
		props.setProperty("myOrigin", "https://site1.com");
		context.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("ps", props));
		context.register(PropertySourcesPlaceholderConfigurer.class);
		context.refresh();
		return context;
	}

	@Override
	protected RestTemplate initRestTemplate() {
		// JDK default HTTP client blacklist headers like Origin
		return new RestTemplate(new HttpComponentsClientHttpRequestFactory());
	}


	@ParameterizedHttpServerTest
	void actualGetRequestWithoutAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/no", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isNull();
		assertThat(entity.getBody()).isEqualTo("no");
	}

	@ParameterizedHttpServerTest
	void actualPostRequestWithoutAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performPost("/no", this.headers, null, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isNull();
		assertThat(entity.getBody()).isEqualTo("no-post");
	}

	@ParameterizedHttpServerTest
	void actualRequestWithDefaultAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/default", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
		assertThat(entity.getBody()).isEqualTo("default");
	}

	@ParameterizedHttpServerTest
	void preflightRequestWithDefaultAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<Void> entity = performOptions("/default", this.headers, Void.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getHeaders().getAccessControlMaxAge()).isEqualTo(1800);
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
	}

	@ParameterizedHttpServerTest
	void actualRequestWithDefaultAnnotationAndNoOrigin(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> entity = performGet("/default", headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isNull();
		assertThat(entity.getBody()).isEqualTo("default");
	}

	@ParameterizedHttpServerTest
	void actualRequestWithCustomizedAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/customized", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
		assertThat(entity.getHeaders().getAccessControlMaxAge()).isEqualTo(-1);
		assertThat(entity.getBody()).isEqualTo("customized");
	}

	@ParameterizedHttpServerTest
	void preflightRequestWithCustomizedAnnotation(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		ResponseEntity<String> entity = performOptions("/customized", this.headers, String.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getHeaders().getAccessControlAllowMethods().toArray()).isEqualTo(new HttpMethod[] {HttpMethod.GET});
		assertThat(entity.getHeaders().getAccessControlAllowHeaders().toArray()).isEqualTo(new String[] {"header1", "header2"});
		assertThat(entity.getHeaders().getAccessControlExposeHeaders().toArray()).isEqualTo(new String[] {"header3", "header4"});
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
		assertThat(entity.getHeaders().getAccessControlMaxAge()).isEqualTo(123);
	}

	@ParameterizedHttpServerTest
	void customOriginDefinedViaValueAttribute(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/origin-value-attribute", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getBody()).isEqualTo("value-attribute");
	}

	@ParameterizedHttpServerTest
	void customOriginDefinedViaPlaceholder(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/origin-placeholder", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getBody()).isEqualTo("placeholder");
	}

	@ParameterizedHttpServerTest
	void classLevel(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		ResponseEntity<String> entity = performGet("/foo", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
		assertThat(entity.getBody()).isEqualTo("foo");

		entity = performGet("/bar", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("*");
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isFalse();
		assertThat(entity.getBody()).isEqualTo("bar");

		entity = performGet("/baz", this.headers, String.class);
		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isTrue();
		assertThat(entity.getBody()).isEqualTo("baz");
	}

	@ParameterizedHttpServerTest
	void ambiguousHeaderPreflightRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		ResponseEntity<String> entity = performOptions("/ambiguous-header", this.headers, String.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getHeaders().getAccessControlAllowMethods().toArray()).isEqualTo(new HttpMethod[] {HttpMethod.GET});
		assertThat(entity.getHeaders().getAccessControlAllowHeaders().toArray()).isEqualTo(new String[] {"header1"});
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isTrue();
	}

	@ParameterizedHttpServerTest
	void ambiguousProducesPreflightRequest(HttpServer httpServer) throws Exception {
		startServer(httpServer);

		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<String> entity = performOptions("/ambiguous-produces", this.headers, String.class);

		assertThat(entity.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(entity.getHeaders().getAccessControlAllowOrigin()).isEqualTo("https://site1.com");
		assertThat(entity.getHeaders().getAccessControlAllowMethods().toArray()).isEqualTo(new HttpMethod[] {HttpMethod.GET});
		assertThat(entity.getHeaders().getAccessControlAllowCredentials()).isTrue();
	}


	@Configuration
	@EnableWebFlux
	@ComponentScan(resourcePattern = "**/CrossOriginAnnotationIntegrationTests*")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig {
	}


	@RestController @SuppressWarnings("unused")
	private static class MethodLevelController {

		@GetMapping("/no")
		public String noAnnotation() {
			return "no";
		}

		@PostMapping("/no")
		public String noAnnotationPost() {
			return "no-post";
		}

		@CrossOrigin
		@GetMapping("/default")
		public String defaultAnnotation() {
			return "default";
		}

		@CrossOrigin
		@GetMapping(path = "/default", params = "q")
		public void defaultAnnotationWithParams() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-header", headers = "header1=a")
		public void ambiguousHeader1a() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-header", headers = "header1=b")
		public void ambiguousHeader1b() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/xml")
		public String ambiguousProducesXml() {
			return "<a></a>";
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/json")
		public String ambiguousProducesJson() {
			return "{}";
		}

		@CrossOrigin(
				origins = { "https://site1.com", "https://site2.com" },
				allowedHeaders = { "header1", "header2" },
				exposedHeaders = { "header3", "header4" },
				methods = RequestMethod.GET,
				maxAge = 123,
				allowCredentials = "false")
		@RequestMapping(path = "/customized", method = { RequestMethod.GET, RequestMethod.POST })
		public String customized() {
			return "customized";
		}

		@CrossOrigin("https://site1.com")
		@GetMapping("/origin-value-attribute")
		public String customOriginDefinedViaValueAttribute() {
			return "value-attribute";
		}

		@CrossOrigin("${myOrigin}")
		@GetMapping("/origin-placeholder")
		public String customOriginDefinedViaPlaceholder() {
			return "placeholder";
		}
	}


	@RestController
	@CrossOrigin(allowCredentials = "false")
	@SuppressWarnings("unused")
	private static class ClassLevelController {

		@GetMapping("/foo")
		public String foo() {
			return "foo";
		}

		@CrossOrigin
		@GetMapping("/bar")
		public String bar() {
			return "bar";
		}

		@CrossOrigin(allowCredentials = "true")
		@GetMapping("/baz")
		public String baz() {
			return "baz";
		}
	}

}
