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

import static org.junit.Assert.*;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.config.CorsRegistry;
import org.springframework.web.reactive.config.WebReactiveConfiguration;

/**
 * @author Sebastien Deleuze
 */
public class CorsConfigurationIntegrationTests extends AbstractRequestMappingIntegrationTests {

	// JDK default HTTP client blacklist headers like Origin
	private RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());

	@Override
	protected ApplicationContext initApplicationContext() {
		AnnotationConfigApplicationContext wac = new AnnotationConfigApplicationContext();
		wac.register(WebConfig.class);
		wac.refresh();
		return wac;
	}

	@Override
	RestTemplate getRestTemplate() {
		return this.restTemplate;
	}

	@Test
	public void actualRequestWithCorsEnabled() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/cors"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://localhost:9000", entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("cors", entity.getBody());
	}

	@Test
	public void actualRequestWithCorsRejected() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		try {
			this.restTemplate.exchange(getUrl("/cors-restricted"), HttpMethod.GET,
					requestEntity, String.class);
		}
		catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
			return;
		}
		fail();
	}

	@Test
	public void actualRequestWithoutCorsEnabled() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/welcome"),
				HttpMethod.GET, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertNull(entity.getHeaders().getAccessControlAllowOrigin());
		assertEquals("welcome", entity.getBody());
	}

	@Test
	public void preflightRequestWithCorsEnabled() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		ResponseEntity<String> entity = this.restTemplate.exchange(getUrl("/cors"),
				HttpMethod.OPTIONS, requestEntity, String.class);
		assertEquals(HttpStatus.OK, entity.getStatusCode());
		assertEquals("http://localhost:9000", entity.getHeaders().getAccessControlAllowOrigin());
	}

	@Test
	public void preflightRequestWithCorsRejected() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		try {
			this.restTemplate.exchange(getUrl("/cors-restricted"), HttpMethod.OPTIONS,
					requestEntity, String.class);
		}
		catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
			return;
		}
		fail();
	}

	@Test
	public void preflightRequestWithoutCorsEnabled() throws Exception {
		HttpHeaders headers = new HttpHeaders();
		headers.add(HttpHeaders.ORIGIN, "http://localhost:9000");
		headers.add(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET");
		HttpEntity<?> requestEntity = new HttpEntity(headers);
		try {
			this.restTemplate.exchange(getUrl("/welcome"), HttpMethod.OPTIONS,
					requestEntity, String.class);
		}
		catch (HttpClientErrorException e) {
			assertEquals(HttpStatus.FORBIDDEN, e.getStatusCode());
			return;
		}
		fail();
	}

	private String getUrl(String path) {
		return "http://localhost:" + this.port + path;
	}


	@Configuration
	@ComponentScan(resourcePattern = "**/CorsConfigurationIntegrationTests*.class")
	@SuppressWarnings({"unused", "WeakerAccess"})
	static class WebConfig extends WebReactiveConfiguration {

		@Override
		protected void addCorsMappings(CorsRegistry registry) {
			registry.addMapping("/cors-restricted").allowedOrigins("http://foo");
			registry.addMapping("/cors");
		}
	}

	@RestController
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

	}

}
