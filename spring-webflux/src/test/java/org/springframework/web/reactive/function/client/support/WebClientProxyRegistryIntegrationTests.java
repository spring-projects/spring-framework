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

package org.springframework.web.reactive.function.client.support;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.reactive.function.client.support.echo.EchoA;
import org.springframework.web.reactive.function.client.support.echo.EchoB;
import org.springframework.web.reactive.function.client.support.greeting.GreetingA;
import org.springframework.web.reactive.function.client.support.greeting.GreetingB;
import org.springframework.web.service.registry.AbstractHttpServiceRegistrar;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.HttpServiceProxyRegistry;
import org.springframework.web.service.registry.ImportHttpServices;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link HttpServiceProxyRegistry} with a
 * {@link org.springframework.web.reactive.function.client.WebClient}.
 *
 * @author Rossen Stoyanchev
 */
public class WebClientProxyRegistryIntegrationTests {

	private final MockWebServer server = new MockWebServer();


	@BeforeEach
	void setUp() throws Exception {
		this.server.start(9090);
	}

	@AfterEach
	void shutdown() {
		this.server.close();
	}


	@ParameterizedTest
	@ValueSource(classes = {
			ListingConfig.class, DetectConfig.class, ManualListingConfig.class, ManualDetectionConfig.class
	})
	void basic(Class<?> configClass) throws InterruptedException {

		AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext(configClass);

		EchoA echoA = context.getBean(EchoA.class);
		EchoB echoB = context.getBean(EchoB.class);

		GreetingA greetingA = context.getBean(GreetingA.class);
		GreetingB greetingB = context.getBean(GreetingB.class);

		HttpServiceProxyRegistry registry = context.getBean(HttpServiceProxyRegistry.class);

		assertThat(registry.getClient(EchoA.class)).isSameAs(echoA);
		assertThat(registry.getClient(EchoB.class)).isSameAs(echoB);

		assertThat(registry.getClient(GreetingA.class)).isSameAs(greetingA);
		assertThat(registry.getClient(GreetingB.class)).isSameAs(greetingB);

		for (int i = 0; i < 4; i++) {
			this.server.enqueue(new MockResponse.Builder().body("body").build());
		}

		echoA.handle("a");
		echoB.handle("b");

		RecordedRequest request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/echoA?input=a");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/echoB?input=b");

		greetingA.handle("a");
		greetingB.handle("b");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greetingA?input=a");

		request = this.server.takeRequest();
		assertThat(request.getMethod()).isEqualTo("GET");
		assertThat(request.getTarget()).isEqualTo("/greetingB?input=b");
	}


	private static class BaseEchoConfig {

		@Bean
		public WebClientHttpServiceGroupConfigurer groupConfigurer() {
			return groups -> groups.filterByName("echo", "greeting")
					.forEachClient((group, builder) -> builder.baseUrl("http://localhost:9090"));
		}
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = "echo", types = {EchoA.class, EchoB.class})
	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = "greeting", types = {GreetingA.class, GreetingB.class})
	private static class ListingConfig extends BaseEchoConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = "echo", basePackageClasses = EchoA.class)
	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = "greeting", basePackageClasses = GreetingA.class)
	private static class DetectConfig extends BaseEchoConfig {
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ManualListingRegistrar.class)
	private static class ManualListingConfig extends BaseEchoConfig {
	}


	static class ManualListingRegistrar extends AbstractHttpServiceRegistrar {

		public ManualListingRegistrar() {
			setDefaultClientType(ClientType.WEB_CLIENT);
		}

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			setDefaultClientType(ClientType.WEB_CLIENT);
			registry.forGroup("echo").register(EchoA.class, EchoB.class);
			registry.forGroup("greeting").register(GreetingA.class, GreetingB.class);
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Import(ManualDetectionRegistrar.class)
	private static class ManualDetectionConfig extends BaseEchoConfig {
	}

	private static class ManualDetectionRegistrar extends AbstractHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			setDefaultClientType(ClientType.WEB_CLIENT);
			registry.forGroup("echo").detectInBasePackages(EchoA.class);
			registry.forGroup("greeting").detectInBasePackages(GreetingA.class);
		}
	}

}
