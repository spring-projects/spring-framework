/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.test.context.web;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.test.context.BootstrapWith;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.context.web.WebAppConfigurationBootstrapWithTests.CustomWebTestContextBootstrapper;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * JUnit-based integration tests that verify support for loading a
 * {@link WebApplicationContext} with a custom {@link WebTestContextBootstrapper}.
 *
 * @author Sam Brannen
 * @author Phillip Webb
 * @since 4.3
 */
@SpringJUnitWebConfig
@BootstrapWith(CustomWebTestContextBootstrapper.class)
class WebAppConfigurationBootstrapWithTests {

	@Autowired
	WebApplicationContext wac;


	@Test
	void webApplicationContextIsLoaded() {
		// from: src/test/webapp/resources/Spring.js
		Resource resource = wac.getResource("/resources/Spring.js");
		assertThat(resource).isNotNull();
		assertThat(resource.exists()).isTrue();
	}


	@Configuration
	static class Config {
	}

	/**
	 * Custom {@link WebTestContextBootstrapper} that requires {@code @WebAppConfiguration}
	 * but hard codes the resource base path.
	 */
	static class CustomWebTestContextBootstrapper extends WebTestContextBootstrapper {

		@Override
		protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
			return new WebMergedContextConfiguration(mergedConfig, "src/test/webapp");
		}
	}

}
