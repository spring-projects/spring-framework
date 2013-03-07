/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.hierarchies.web;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.ContextHierarchy;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;

/**
 * @author Sam Brannen
 * @since 3.2.2
 */
@WebAppConfiguration
@ContextHierarchy(@ContextConfiguration)
public class RootWacEarTests extends EarTests {

	@Configuration
	static class RootWacConfig {

		@Bean
		public String root() {
			return "root";
		}
	}


	// -------------------------------------------------------------------------

	@Autowired
	private WebApplicationContext wac;

	@Autowired
	private String ear;

	@Autowired
	private String root;


	@Test
	@Override
	public void verifyEarConfig() {
		/* no-op */
	}

	@Test
	public void verifyRootWacConfig() {
		ApplicationContext parent = wac.getParent();
		assertNotNull(parent);
		assertFalse(parent instanceof WebApplicationContext);
		assertEquals("ear", ear);
		assertEquals("root", root);
	}

}
