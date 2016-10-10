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

package org.springframework.web.reactive.result.method.annotation;

import java.util.Properties;

import org.junit.Before;
import org.junit.Test;

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
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.WebReactiveConfiguration;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * Integration tests with {@code @CrossOrigin} and {@code @RequestMapping}
 * annotated handler methods.
 *
 * @author Sebastien Deleuze
 * @author Rossen Stoyanchev
 */
public class CrossOriginAnnotationIntegrationTests extends AbstractRequestMappingIntegrationTests {

	private HttpHeaders headers;


	@Before
	public void setup() throws Exception {
		super.setup();
		this.headers = new HttpHeaders();
		this.headers.setOrigin("http://site1.com");
	}


	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
		context.register(WebConfig.class);
		Properties props = new Properties();
		props.setProperty("myOrigin", "http://site1.com");
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


	@Test
	public void actualGetRequestWithoutAnnotation() throws Exception {
		ResponseEntity<String> entity = performGet("/no", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("no", entity.getBody());
	}

	@Test
	public void actualPostRequestWithoutAnnotation() throws Exception {
		ResponseEntity<String> entity = performPost("/no", this.headers, null, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("no-post", entity.getBody());
	}

	@Test
	public void actualRequestWithDefaultAnnotation() throws Exception {
		ResponseEntity<String> entity = performGet("/default", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("default", entity.getBody());
	}

	@Test
	public void preflightRequestWithDefaultAnnotation() throws Exception {
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<Void> entity = performOptions("/default", this.headers, Void.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(1800, entity.getHeaders().getAccessControlMaxAge());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
	}

	@Test
	public void actualRequestWithDefaultAnnotationAndNoOrigin() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		ResponseEntity<String> entity = performGet("/default", headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("default", entity.getBody());
	}

	@Test
	public void actualRequestWithCustomizedAnnotation() throws Exception {
		ResponseEntity<String> entity = performGet("/customized", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals(-1, entity.getHeaders().getAccessControlMaxAge());
		assertEquals("customized", entity.getBody());
	}

	@Test
	public void preflightRequestWithCustomizedAnnotation() throws Exception {
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		ResponseEntity<String> entity = performOptions("/customized", this.headers, String.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET},
				entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertArrayEquals(new String[] {"header1", "header2"},
				entity.getHeaders().getAccessControlAllowHeaders().toArray());
		assertArrayEquals(new String[] {"header3", "header4"},
				entity.getHeaders().getAccessControlExposeHeaders().toArray());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals(123, entity.getHeaders().getAccessControlMaxAge());
	}

	@Test
	public void customOriginDefinedViaValueAttribute() throws Exception {
		ResponseEntity<String> entity = performGet("/origin-value-attribute", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("value-attribute", entity.getBody());
	}

	@Test
	public void customOriginDefinedViaPlaceholder() throws Exception {
		ResponseEntity<String> entity = performGet("/origin-placeholder", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("placeholder", entity.getBody());
	}

	@Test
	public void classLevel() throws Exception {
		ResponseEntity<String> entity = performGet("/foo", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("*", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("foo", entity.getBody());

		entity = performGet("/bar", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("*", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("bar", entity.getBody());

		entity = performGet("/baz", this.headers, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("baz", entity.getBody());
	}

	@Test
	public void ambiguousHeaderPreflightRequest() throws Exception {
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		ResponseEntity<String> entity = performOptions("/ambiguous-header", this.headers, String.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET},
				entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertArrayEquals(new String[] {"header1"},
				entity.getHeaders().getAccessControlAllowHeaders().toArray());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
	}

	@Test
	public void ambiguousProducesPreflightRequest() throws Exception {
		this.headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		ResponseEntity<String> entity = performOptions("/ambiguous-produces", this.headers, String.class);

		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET},
				entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/CrossOriginAnnotationIntegrationTests*")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebReactiveConfiguration {
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
		public void ambigousHeader1a() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-header", headers = "header1=b")
		public void ambigousHeader1b() {
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/xml")
		public String ambigousProducesXml() {
			return "<a></a>";
		}

		@CrossOrigin
		@GetMapping(path = "/ambiguous-produces", produces = "application/json")
		public String ambigousProducesJson() {
			return "{}";
		}

		@CrossOrigin(
				origins = { "http://site1.com", "http://site2.com" },
				allowedHeaders = { "header1", "header2" },
				exposedHeaders = { "header3", "header4" },
				methods = RequestMethod.GET,
				maxAge = 123,
				allowCredentials = "false")
		@RequestMapping(path = "/customized", method = { RequestMethod.GET, RequestMethod.POST })
		public String customized() {
			return "customized";
		}

		@CrossOrigin("http://site1.com")
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
