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
import static org.assertj.core.api.Assertions.assertThatRuntimeException;

/**
 * Integration tests for {@link DynamicPropertyRegistrar} bean support.
 *
 * @author Sam Brannen
 * @since 6.2
 * @see DynamicPropertySourceIntegrationTests
 */
@SpringJUnitConfig
@TestPropertySource(properties = "api.url.1: https://example.com/test")
class DynamicPropertyRegistrarIntegrationTests {

	private static final String API_URL_1 = "api.url.1";
	private static final String API_URL_2 = "api.url.2";


	@Test
	void customDynamicPropertyRegistryCanExistInApplicationContext(
			@Autowired DynamicPropertyRegistry dynamicPropertyRegistry) {

		assertThatRuntimeException()
				.isThrownBy(() -> dynamicPropertyRegistry.add("test", () -> "test"))
				.withMessage("Boom!");
	}

	@Test
	void dynamicPropertySourceOverridesTestPropertySource(@Autowired ConfigurableEnvironment env) {
		assertApiUrlIsDynamic1(env.getProperty(API_URL_1));

		MutablePropertySources propertySources = env.getPropertySources();
		assertThat(propertySources.size()).isGreaterThanOrEqualTo(4);
		assertThat(propertySources.contains("Inlined Test Properties")).isTrue();
		assertThat(propertySources.contains("Dynamic Test Properties")).isTrue();
		assertThat(propertySources.get("Inlined Test Properties").getProperty(API_URL_1)).isEqualTo("https://example.com/test");
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(API_URL_1)).isEqualTo("https://example.com/dynamic/1");
		assertThat(propertySources.get("Dynamic Test Properties").getProperty(API_URL_2)).isEqualTo("https://example.com/dynamic/2");
	}

	@Test
	void testReceivesDynamicProperties(@Value("${api.url.1}") String apiUrl1, @Value("${api.url.2}") String apiUrl2) {
		assertApiUrlIsDynamic1(apiUrl1);
		assertApiUrlIsDynamic2(apiUrl2);
	}

	@Test
	void environmentInjectedServiceCanRetrieveDynamicProperty(@Autowired EnvironmentInjectedService service) {
		assertApiUrlIsDynamic1(service);
	}

	@Test
	void constructorInjectedServiceReceivesDynamicProperty(@Autowired ConstructorInjectedService service) {
		assertApiUrlIsDynamic1(service);
	}

	@Test
	void setterInjectedServiceReceivesDynamicProperty(@Autowired SetterInjectedService service) {
		assertApiUrlIsDynamic1(service);
	}


	private static void assertApiUrlIsDynamic1(ApiUrlClient service) {
		assertApiUrlIsDynamic1(service.getApiUrl());
	}

	private static void assertApiUrlIsDynamic1(String apiUrl) {
		assertThat(apiUrl).isEqualTo("https://example.com/dynamic/1");
	}

	private static void assertApiUrlIsDynamic2(String apiUrl) {
		assertThat(apiUrl).isEqualTo("https://example.com/dynamic/2");
	}


	@Configuration
	@Import({ EnvironmentInjectedService.class, ConstructorInjectedService.class, SetterInjectedService.class })
	static class Config {

		@Bean
		ApiServer apiServer() {
			return new ApiServer();
		}

		// Accepting ApiServer as a method argument ensures that the apiServer
		// bean will be instantiated before any other singleton beans in the
		// context which further ensures that the dynamic "api.url" property is
		// available to all standard singleton beans.
		@Bean
		DynamicPropertyRegistrar apiPropertiesRegistrar1(ApiServer apiServer) {
			return registry -> registry.add(API_URL_1, () -> apiServer.getUrl() + "/1");
		}

		@Bean
		DynamicPropertyRegistrar apiPropertiesRegistrar2(ApiServer apiServer) {
			return registry -> registry.add(API_URL_2, () -> apiServer.getUrl() + "/2");
		}

		@Bean
		DynamicPropertyRegistry dynamicPropertyRegistry() {
			return (name, valueSupplier) -> {
				throw new RuntimeException("Boom!");
			};
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
			return this.env.getProperty(API_URL_1);
		}
	}

	static class ConstructorInjectedService implements ApiUrlClient {

		private final String apiUrl;


		ConstructorInjectedService(@Value("${api.url.1}") String apiUrl) {
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
		void setApiUrl(@Value("${api.url.1}") String apiUrl) {
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
