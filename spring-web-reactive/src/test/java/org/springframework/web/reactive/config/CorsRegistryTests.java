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

package org.springframework.web.reactive.config;

import java.util.Arrays;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.Test;

import org.springframework.web.cors.CorsConfiguration;

/**
 * Test fixture with a {@link CorsRegistry}.
 *
 * @author Sebastien Deleuze
 */
public class CorsRegistryTests {

	private CorsRegistry registry;

	@Before
	public void setUp() {
		this.registry = new CorsRegistry();
	}

	@Test
	public void noMapping() {
		assertTrue(this.registry.getCorsConfigurations().isEmpty());
	}

	@Test
	public void multipleMappings() {
		this.registry.addMapping("/foo");
		this.registry.addMapping("/bar");
		assertEquals(2, this.registry.getCorsConfigurations().size());
	}

	@Test
	public void customizedMapping() {
		this.registry.addMapping("/foo").allowedOrigins("http://domain2.com", "http://domain2.com")
				.allowedMethods("DELETE").allowCredentials(false).allowedHeaders("header1", "header2")
				.exposedHeaders("header3", "header4").maxAge(3600);
		Map<String, CorsConfiguration> configs = this.registry.getCorsConfigurations();
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
