/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.web.cors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;

/**
 * Test case for {@link CorsConfiguration}.
 *
 * @author Sebastien Deleuze
 */
public class CorsConfigurationTests {

	private CorsConfiguration config;

	@Before
	public void setup() {
		config = new CorsConfiguration();
	}

	@Test
	public void checkOriginAllowed() {
		config.setAllowedOrigins(Arrays.asList("*"));
		assertEquals("*", config.checkOrigin("http://domain.com"));
		config.setAllowCredentials(true);
		assertEquals("http://domain.com", config.checkOrigin("http://domain.com"));
		config.setAllowedOrigins(Arrays.asList("http://domain.com"));
		assertEquals("http://domain.com", config.checkOrigin("http://domain.com"));
		config.setAllowCredentials(false);
		assertEquals("http://domain.com", config.checkOrigin("http://domain.com"));
	}

	@Test
	public void checkOriginNotAllowed() {
		assertNull(config.checkOrigin(null));
		assertNull(config.checkOrigin("http://domain.com"));
		config.addAllowedOrigin("*");
		assertNull(config.checkOrigin(null));
		config.setAllowedOrigins(Arrays.asList("http://domain1.com"));
		assertNull(config.checkOrigin("http://domain2.com"));
		config.setAllowedOrigins(new ArrayList<>());
		assertNull(config.checkOrigin("http://domain.com"));
	}

	@Test
	public void checkMethodAllowed() {
		assertEquals(Arrays.asList(HttpMethod.GET), config.checkHttpMethod(HttpMethod.GET));
		config.addAllowedMethod("GET");
		assertEquals(Arrays.asList(HttpMethod.GET), config.checkHttpMethod(HttpMethod.GET));
		config.addAllowedMethod("POST");
		assertEquals(Arrays.asList(HttpMethod.GET, HttpMethod.POST), config.checkHttpMethod(HttpMethod.GET));
		assertEquals(Arrays.asList(HttpMethod.GET, HttpMethod.POST), config.checkHttpMethod(HttpMethod.POST));
	}

	@Test
	public void checkMethodNotAllowed() {
		assertNull(config.checkHttpMethod(null));
		assertNull(config.checkHttpMethod(HttpMethod.DELETE));
		config.setAllowedMethods(new ArrayList<>());
		assertNull(config.checkHttpMethod(HttpMethod.HEAD));
	}

	@Test
	public void checkHeadersAllowed() {
		assertEquals(Collections.emptyList(), config.checkHeaders(Collections.emptyList()));
		config.addAllowedHeader("header1");
		config.addAllowedHeader("header2");
		assertEquals(Arrays.asList("header1"), config.checkHeaders(Arrays.asList("header1")));
		assertEquals(Arrays.asList("header1", "header2"), config.checkHeaders(Arrays.asList("header1", "header2")));
		assertEquals(Arrays.asList("header1", "header2"), config.checkHeaders(Arrays.asList("header1", "header2", "header3")));
	}

	@Test
	public void checkHeadersNotAllowed() {
		assertNull(config.checkHeaders(null));
		assertNull(config.checkHeaders(Arrays.asList("header1")));
		config.addAllowedHeader("header2");
		assertNull(config.checkHeaders(Arrays.asList("header1")));
		config.setAllowedHeaders(new ArrayList<>());
		assertNull(config.checkHeaders(Arrays.asList("header1")));
	}

}
