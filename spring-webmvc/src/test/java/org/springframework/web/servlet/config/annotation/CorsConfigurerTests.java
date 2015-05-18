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

package org.springframework.web.servlet.config.annotation;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import org.springframework.web.cors.CorsConfiguration;

/**
 * Test fixture with a {@link CorsConfigurer}.
 * 
 * @author Sebastien Deleuze
 */
public class CorsConfigurerTests {
	
	private CorsConfigurer configurer;
	
	@Before
	public void setUp() {
		this.configurer = new CorsConfigurer();
	}
	
	@Test
	public void noCorsConfigured() {
		assertTrue(this.configurer.getCorsConfigurations().isEmpty());
	}
	
	@Test
	public void multipleCorsConfigured() {
		this.configurer.enableCors("/foo");
		this.configurer.enableCors("/bar");
		assertEquals(2, this.configurer.getCorsConfigurations().size());
	}
	
	@Test
	public void defaultCorsRegistration() {
		this.configurer.enableCors();
		Map<String, CorsConfiguration> configs = this.configurer.getCorsConfigurations();
		assertEquals(1, configs.size());
		CorsConfiguration config = configs.get("/**");
		assertEquals(Arrays.asList("*"), config.getAllowedOrigins());
		assertEquals(Arrays.asList("GET", "HEAD", "POST"), config.getAllowedMethods());
		assertEquals(Arrays.asList("*"), config.getAllowedHeaders());
		assertEquals(true, config.getAllowCredentials());
		assertEquals(Long.valueOf(1800), config.getMaxAge());
	}
	
	@Test
	public void customizedCorsRegistration() {
		this.configurer.enableCors("/foo").allowedOrigins("http://domain2.com", "http://domain2.com")
				.allowedMethods("DELETE").allowCredentials(false).allowedHeaders("header1", "header2")
				.exposedHeaders("header3", "header4").maxAge(3600);
		Map<String, CorsConfiguration> configs = this.configurer.getCorsConfigurations();
		assertEquals(1, configs.size());
		CorsConfiguration config = configs.get("/foo");
		assertEquals(Arrays.asList("http://domain2.com", "http://domain2.com"), config.getAllowedOrigins());
		assertEquals(Arrays.asList("DELETE"), config.getAllowedMethods());
		assertEquals(Arrays.asList("header1", "header2"), config.getAllowedHeaders());
		assertEquals(Arrays.asList("header3", "header4"), config.getExposedHeaders());
		assertEquals(false, config.getAllowCredentials());
		assertEquals(Long.valueOf(3600), config.getMaxAge());
	}
	
}
