/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.test.context.support;

import org.junit.jupiter.api.Test;
import org.junit.platform.testkit.engine.EngineTestKit;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.SpringProperties;
import org.springframework.test.context.CacheAwareContextLoaderDelegate;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.cache.DefaultCacheAwareContextLoaderDelegate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.platform.engine.discovery.DiscoverySelectors.selectClass;

/**
 * Integration tests for configuring a custom default {@link CacheAwareContextLoaderDelegate}
 * via {@link SpringProperties}.
 *
 * @author sbrannen
 * @since 5.3.11
 */
class CustomDefaultCacheAwareContextLoaderDelegateTests {

	@Test
	void customDefaultCacheAwareContextLoaderDelegateConfiguredViaSpringProperties() {
		String key = CacheAwareContextLoaderDelegate.DEFAULT_CACHE_AWARE_CONTEXT_LOADER_DELEGATE_PROPERTY_NAME;

		try {
			SpringProperties.setProperty(key, AotCacheAwareContextLoaderDelegate.class.getName());

			EngineTestKit.engine("junit-jupiter")//
					.selectors(selectClass(TestCase.class))//
					.execute()//
					.testEvents()//
					.assertStatistics(stats -> stats.started(1).succeeded(1).failed(0));
		}
		finally {
			SpringProperties.setProperty(key, null);
		}
	}


	@SpringJUnitConfig
	static class TestCase {

		@Test
		void test(@Autowired String foo) {
			// foo will be "bar" unless the AotCacheAwareContextLoaderDelegate is registered.
			assertThat(foo).isEqualTo("AOT");
		}


		@Configuration
		static class Config {

			@Bean
			String foo() {
				return "bar";
			}
		}
	}

	static class AotCacheAwareContextLoaderDelegate extends DefaultCacheAwareContextLoaderDelegate {

		@Override
		protected ApplicationContext loadContextInternal(MergedContextConfiguration mergedContextConfiguration) {
			GenericApplicationContext applicationContext = new GenericApplicationContext();
			applicationContext.registerBean("foo", String.class, () -> "AOT");
			applicationContext.refresh();
			return applicationContext;
		}
	}

}
