/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.context;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link DynamicPropertyRegistry} bean support.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see DynamicPropertySourceIntegrationTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "api.url: https://example.com/test")
class DynamicPropertyRegistryIntegrationTests {

	private static final String API_URL = "api.url";


	@Test
	void dynamicPropertySourceOverridesTestPropertySource(@Autowired ConfigurableEnvironment env) {
		assertApiUrlIsDynamic(env.getProperty(API_URL));

		MutablePropertySources propertySources = env.getPropertySources();
		assertThat(propertySources.size()).isGreaterThanOrEqualTo(4);
		assertThat(propertySources.contains("Inlined Test Properties")).isTrue();
		assertThat(propertySources.contains("Dynamic Test Properties")).isTrue();
		assertThat(propertySources.get("Inlined Test Properties").getProperty(API_URL)).isEqualTo("https://example.com/test");
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(API_URL)).isEqualTo("https://example.com/dynamic");
	}

	@Test
	void testReceivesDynamicProperty(@Value("${api.url}") String apiUrl) {
		assertApiUrlIsDynamic(apiUrl);
	}

	@Test
	void environmentInjectedServiceCanRetrieveDynamicProperty(@Autowired EnvironmentInjectedService service) {
		assertApiUrlIsDynamic(service);
	}

	@Test
	void constructorInjectedServiceReceivesDynamicProperty(@Autowired ConstructorInjectedService service) {
		assertApiUrlIsDynamic(service);
	}

	@Test
	void setterInjectedServiceReceivesDynamicProperty(@Autowired SetterInjectedService service) {
		assertApiUrlIsDynamic(service);
	}


	private static void assertApiUrlIsDynamic(ApiUrlClient service) {
		assertApiUrlIsDynamic(service.getApiUrl());
	}

	private static void assertApiUrlIsDynamic(String apiUrl) {
		assertThat(apiUrl).isEqualTo("https://example.com/dynamic");
	}


	@Configuration
	@Import({ EnvironmentInjectedService.class, ConstructorInjectedService.class, SetterInjectedService.class })
	static class Config {

		// Annotating this @Bean method with @DynamicPropertySource ensures that
		// this bean will be instantiated before any other singleton beans in the
		// context which further ensures that the dynamic "api.url" property is
		// available to all standard singleton beans.
		@Bean
		@DynamicPropertySource
		ApiServer apiServer(DynamicPropertyRegistry registry) {
			ApiServer apiServer = new ApiServer();
			registry.add(API_URL, apiServer::getUrl);
			return apiServer;
		}

	}

	interface ApiUrlClient {

		String getApiUrl();
	}

	static class EnvironmentInjectedService implements ApiUrlClient {

		private final Environment env;


		EnvironmentInjectedService(Environment env) {
			this.env = env;
		}

		@Override
		public String getApiUrl() {
			return this.env.getProperty(API_URL);
		}
	}

	static class ConstructorInjectedService implements ApiUrlClient {

		private final String apiUrl;


		ConstructorInjectedService(@Value("${api.url}") String apiUrl) {
			this.apiUrl = apiUrl;
		}

		@Override
		public String getApiUrl() {
			return this.apiUrl;
		}
	}

	static class SetterInjectedService implements ApiUrlClient {

		private String apiUrl;


		@Autowired
		void setApiUrl(@Value("${api.url}") String apiUrl) {
			this.apiUrl = apiUrl;
		}

		@Override
		public String getApiUrl() {
			return this.apiUrl;
		}
	}

	static class ApiServer {

		String getUrl() {
			return "https://example.com/dynamic";
		}
	}

}
