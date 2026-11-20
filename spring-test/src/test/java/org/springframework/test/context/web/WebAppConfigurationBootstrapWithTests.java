/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
