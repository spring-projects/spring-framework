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

import static org.junit.Assert.*;
import static org.junit.Assert.assertArrayEquals;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.WebReactiveConfiguration;

/**
 * @author Sebastien Deleuze
 */
public class CrossOriginAnnotationIntegrationTests extends AbstractRequestMappingIntegrationTests {

	// JDK default HTTP client blacklist headers like Origin
	private RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());


	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		Properties props = new Properties();
		props.setProperty("myOrigin", "http://site1.com");
		wac.getEnvironment().getPropertySources().addFirst(new PropertiesPropertySource("ps", props));
		wac.register(PropertySourcesPlaceholderConfigurer.class);
		wac.refresh();
		return wac;
	}

	@Override
	RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	@Test
	public void actualGetRequestWithoutAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/no"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("no", entity.getBody());
	}

	@Test
	public void actualPostRequestWithoutAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/no"),
				HttpMethod.POST, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("no-post", entity.getBody());
	}

	@Test
	public void actualRequestWithDefaultAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/default"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("default", entity.getBody());
	}

	@Test
	public void preflightRequestWithDefaultAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<Void> entity = this.restTemplate.exchange(getUrl("/default"),
				HttpMethod.OPTIONS, requestEntity, Void.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(1800, entity.getHeaders().getAccessControlMaxAge());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
	}

	@Test
	public void actualRequestWithDefaultAnnotationAndNoOrigin() {
		HttpHeaders headers = new HttpHeaders();
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/default"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("default", entity.getBody());
	}

	@Test
	public void actualRequestWithCustomizedAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/customized"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals(-1, entity.getHeaders().getAccessControlMaxAge());
		assertEquals("customized", entity.getBody());
	}

	@Test
	public void preflightRequestWithCustomizedAnnotation() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1, header2");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/customized"),
				HttpMethod.OPTIONS, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET}, entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertArrayEquals(new String[] {"header1", "header2"}, entity.getHeaders().getAccessControlAllowHeaders().toArray());
		assertArrayEquals(new String[] {"header3", "header4"}, entity.getHeaders().getAccessControlExposeHeaders().toArray());
		assertEquals(123, entity.getHeaders().getAccessControlMaxAge());
	}

	@Test
	public void customOriginDefinedViaValueAttribute() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/origin-value-attribute"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("value-attribute", entity.getBody());
	}

	@Test
	public void customOriginDefinedViaPlaceholder() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/origin-placeholder"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("placeholder", entity.getBody());
	}

	@Test
	public void classLevel() {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		HttpEntity<?> requestEntity = new HttpEntity(headers);

		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/foo"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("*", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("foo", entity.getBody());

		entity = this.restTemplate.exchange(getUrl("/bar"), HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("*", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(false, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("bar", entity.getBody());

		entity = this.restTemplate.exchange(getUrl("/baz"), HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
		assertEquals("baz", entity.getBody());
	}

	@Test
	public void ambiguousHeaderPreflightRequest() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "header1");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/ambiguous-header"),
				HttpMethod.OPTIONS, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET}, entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
		assertArrayEquals(new String[] {"header1"}, entity.getHeaders().getAccessControlAllowHeaders().toArray());
	}

	@Test
	public void ambiguousProducesPreflightRequest() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://site1.com");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/ambiguous-produces"),
				HttpMethod.OPTIONS, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://site1.com", entity.getHeaders().getAccessControlAllowOrigin());
		assertArrayEquals(new HttpMethod[] {HttpMethod.GET}, entity.getHeaders().getAccessControlAllowMethods().toArray());
		assertEquals(true, entity.getHeaders().getAccessControlAllowCredentials());
	}

	private String getUrl(String path) {
		return "http://localhost:" + this.port + path;
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/CrossOriginAnnotationIntegrationTests*")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebReactiveConfiguration {

	}

	@RestController
	private static class MethodLevelController {

		@RequestMapping(path = "/no", method = RequestMethod.GET)
		public String noAnnotation() {
			return "no";
		}

		@RequestMapping(path = "/no", method = RequestMethod.POST)
		public String noAnnotationPost() {
			return "no-post";
		}

		@CrossOrigin
		@RequestMapping(path = "/default", method = RequestMethod.GET)
		public String defaultAnnotation() {
			return "default";
		}

		@CrossOrigin
		@RequestMapping(path = "/default", method = RequestMethod.GET, params = "q")
		public void defaultAnnotationWithParams() {
		}

		@CrossOrigin
		@RequestMapping(path = "/ambiguous-header", method = RequestMethod.GET, headers = "header1=a")
		public void ambigousHeader1a() {
		}

		@CrossOrigin
		@RequestMapping(path = "/ambiguous-header", method = RequestMethod.GET, headers = "header1=b")
		public void ambigousHeader1b() {
		}

		@CrossOrigin
		@RequestMapping(path = "/ambiguous-produces", method = RequestMethod.GET, produces = "application/xml")
		public String ambigousProducesXml() {
			return "<a></a>";
		}

		@CrossOrigin
		@RequestMapping(path = "/ambiguous-produces", method = RequestMethod.GET, produces = "application/json")
		public String ambigousProducesJson() {
			return "{}";
		}

		@CrossOrigin(origins = { "http://site1.com", "http://site2.com" }, allowedHeaders = { "header1", "header2" },
				exposedHeaders = { "header3", "header4" }, methods = RequestMethod.GET, maxAge = 123, allowCredentials = "false")
		@RequestMapping(path = "/customized", method = { RequestMethod.GET, RequestMethod.POST })
		public String customized() {
			return "customized";
		}

		@CrossOrigin("http://site1.com")
		@RequestMapping("/origin-value-attribute")
		public String customOriginDefinedViaValueAttribute() {
			return "value-attribute";
		}

		@CrossOrigin("${myOrigin}")
		@RequestMapping("/origin-placeholder")
		public String customOriginDefinedViaPlaceholder() {
			return "placeholder";
		}
	}

	@RestController
	@CrossOrigin(allowCredentials = "false")
	private static class ClassLevelController {

		@RequestMapping(path = "/foo", method = RequestMethod.GET)
		public String foo() {
			return "foo";
		}

		@CrossOrigin
		@RequestMapping(path = "/bar", method = RequestMethod.GET)
		public String bar() {
			return "bar";
		}

		@CrossOrigin(allowCredentials = "true")
		@RequestMapping(path = "/baz", method = RequestMethod.GET)
		public String baz() {
			return "baz";
		}

	}

}
