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

package org.springframework.web.service.registry;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.basic.BasicClient;
import org.springframework.web.service.registry.echo.EchoClientA;
import org.springframework.web.service.registry.echo.EchoClientB;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link AbstractClientHttpServiceRegistrar}.
 *
 * @author Rossen Stoyanchev
 */
public class ClientHttpServiceRegistrarTests {

	private final TestGroupRegistry groupRegistry = new TestGroupRegistry();


	@Test
	void register() {

		List<String> basePackages = List.of(
				BasicClient.class.getPackageName(), EchoClientA.class.getPackageName());

		AbstractClientHttpServiceRegistrar registrar = new AbstractClientHttpServiceRegistrar() {

			@Override
			protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata importingClassMetadata) {
				findAndRegisterHttpServiceClients(groupRegistry, basePackages);
			}
		};
		registrar.setEnvironment(new StandardEnvironment());
		registrar.setResourceLoader(new PathMatchingResourcePatternResolver());
		registrar.registerHttpServices(groupRegistry, mock(AnnotationMetadata.class));

		assertGroups(
				TestGroup.ofListing("default", BasicClient.class),
				TestGroup.ofListing("echo", EchoClientA.class, EchoClientB.class));
	}

	@Test
	void registerWhenNoClientsDoesNotCreateBeans() {
		try (AnnotationConfigApplicationContext cxt = new AnnotationConfigApplicationContext(NoOpImportConfig.class)) {
			assertThat(cxt.getBeanNamesForType(HttpServiceProxyRegistry.class)).isEmpty();
		}
	}

	private void assertGroups(TestGroup... expectedGroups) {
		Map<String, TestGroup> groupMap = this.groupRegistry.groupMap();
		assertThat(groupMap.size()).isEqualTo(expectedGroups.length);
		for (TestGroup expected : expectedGroups) {
			TestGroup actual = groupMap.get(expected.name());
			assertThat(actual.httpServiceTypes()).isEqualTo(expected.httpServiceTypes());
			assertThat(actual.clientType()).isEqualTo(expected.clientType());
			assertThat(actual.packageNames()).isEqualTo(expected.packageNames());
			assertThat(actual.packageClasses()).isEqualTo(expected.packageClasses());
		}
	}


	@Configuration(proxyBeanMethods = false)
	@Import(NoOpRegistrar.class)
	static class NoOpImportConfig {
	}


	static class NoOpRegistrar extends AbstractClientHttpServiceRegistrar {

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
		}
	}

}
