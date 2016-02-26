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

package org.springframework.test.context.web;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebbAppConfigurationBootstrapWithTests.TestWebTestContextBootstrapper;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.Assert.*;

/**
 * JUnit-based integration tests that verify support for loading a
 * {@link WebApplicationContext} with a custom {@code @BootstrapWith}.
 *
 * @author Phillip Webb
 * @since 4.3
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
@WebAppConfiguration
@BootstrapWith(TestWebTestContextBootstrapper.class)
public class WebbAppConfigurationBootstrapWithTests {

	@Autowired
	private ApplicationContext context;


	@Test
	public void testApplicationContextIsWebApplicationContext() throws Exception {
		assertTrue(this.context instanceof WebApplicationContext);
	}


	@Configuration
	static class Config {

	}


	static class TestWebTestContextBootstrapper extends WebTestContextBootstrapper {

	}

}
