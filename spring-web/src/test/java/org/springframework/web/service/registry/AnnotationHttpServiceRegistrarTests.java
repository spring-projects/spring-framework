/*
 * Copyright 2002-2025 the original author or authors.
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

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.test.generate.TestGenerationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.aot.ApplicationContextAotGenerator;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.test.tools.CompileWithForkedClassLoader;
import org.springframework.core.test.tools.Compiled;
import org.springframework.core.test.tools.TestCompiler;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.echo.EchoA;
import org.springframework.web.service.registry.echo.EchoB;
import org.springframework.web.service.registry.greeting.GreetingA;
import org.springframework.web.service.registry.greeting.GreetingB;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link AnnotationHttpServiceRegistrar}.
 *
 * @author Rossen Stoyanchev
 * @author Stephane Nicoll
 */
public class AnnotationHttpServiceRegistrarTests {

	private static final String ECHO_GROUP = "echo";

	private static final String GREETING_GROUP = "greeting";


	private final TestGroupRegistry groupRegistry = new TestGroupRegistry();

	private final TestAnnotationHttpServiceRegistrar registrar = new TestAnnotationHttpServiceRegistrar();


	@Test
	void basicListing() {
		doRegister(ListingConfig.class);
		assertGroups(StubGroup.ofListing(ECHO_GROUP, EchoA.class, EchoB.class));
	}

	@Test
	@CompileWithForkedClassLoader
	void basicListingWithAot() {
		GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.registerBean(ListingConfig.class);
		compile(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
			HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
			assertThat(registry.getGroupNames()).containsOnly(ECHO_GROUP);
			assertThat(registry.getClientTypesInGroup(ECHO_GROUP)).containsOnly(EchoA.class, EchoB.class);
		});
	}

	@Test
	void basicScan() {
		doRegister(ScanConfig.class);
		assertGroups(
				StubGroup.ofPackageClasses(ECHO_GROUP, EchoA.class),
				StubGroup.ofPackageClasses(GREETING_GROUP, GreetingA.class));
	}

	@Test
	@CompileWithForkedClassLoader
	void basicScanWithAot() {
		GenericApplicationContext applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.registerBean(ScanConfig.class);
		compile(applicationContext, (initializer, compiled) -> {
			GenericApplicationContext freshApplicationContext = toFreshApplicationContext(initializer);
			HttpServiceProxyRegistry registry = freshApplicationContext.getBean(HttpServiceProxyRegistry.class);
			assertThat(registry.getGroupNames()).containsOnly(ECHO_GROUP, GREETING_GROUP);
			assertThat(registry.getClientTypesInGroup(ECHO_GROUP)).containsOnly(EchoA.class, EchoB.class);
			assertThat(registry.getClientTypesInGroup(GREETING_GROUP)).containsOnly(GreetingA.class, GreetingB.class);
		});
	}

	@Test
	void clientType() {
		doRegister(ClientTypeConfig.class);
		assertGroups(
				StubGroup.ofListing(ECHO_GROUP, ClientType.WEB_CLIENT, EchoA.class),
				StubGroup.ofListing(GREETING_GROUP, ClientType.WEB_CLIENT, GreetingA.class));
	}

	private void doRegister(Class<?> configClass) {
		AnnotationMetadata metadata = AnnotationMetadata.introspect(configClass);
		this.registrar.registerHttpServices(this.groupRegistry, metadata);
	}

	@SuppressWarnings("unchecked")
	private void compile(GenericApplicationContext applicationContext,
			BiConsumer<ApplicationContextInitializer<GenericApplicationContext>, Compiled> result) {
		ApplicationContextAotGenerator generator = new ApplicationContextAotGenerator();
		TestGenerationContext generationContext = new TestGenerationContext();
		generator.processAheadOfTime(applicationContext, generationContext);
		generationContext.writeGeneratedContent();
		TestCompiler.forSystem().with(generationContext).compile(compiled ->
				result.accept(compiled.getInstance(ApplicationContextInitializer.class), compiled));
	}

	private GenericApplicationContext toFreshApplicationContext(
			ApplicationContextInitializer<GenericApplicationContext> initializer) {
		GenericApplicationContext freshApplicationContext = new GenericApplicationContext();
		initializer.initialize(freshApplicationContext);
		freshApplicationContext.refresh();
		return freshApplicationContext;
	}

	private void assertGroups(StubGroup... expectedGroups) {
		Map<String, StubGroup> groupMap = this.groupRegistry.groupMap();
		assertThat(groupMap.size()).isEqualTo(expectedGroups.length);
		for (StubGroup expected : expectedGroups) {
			StubGroup actual = groupMap.get(expected.name());
			assertThat(actual.httpServiceTypes()).isEqualTo(expected.httpServiceTypes());
			assertThat(actual.clientType()).isEqualTo(expected.clientType());
			assertThat(actual.packageNames()).isEqualTo(expected.packageNames());
			assertThat(actual.packageClasses()).isEqualTo(expected.packageClasses());
		}
	}


	@ImportHttpServices(group = ECHO_GROUP, types = { EchoA.class, EchoB.class })
	static class ListingConfig {
	}

	@ImportHttpServices(group = ECHO_GROUP, basePackageClasses = { EchoA.class })
	@ImportHttpServices(group = GREETING_GROUP, basePackageClasses = { GreetingA.class })
	static class ScanConfig {
	}

	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = ECHO_GROUP, types = { EchoA.class })
	@ImportHttpServices(clientType = ClientType.WEB_CLIENT, group = GREETING_GROUP, types = { GreetingA.class })
	static class ClientTypeConfig {
	}


	private static class TestAnnotationHttpServiceRegistrar extends AnnotationHttpServiceRegistrar {

		@Override
		public void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			super.registerHttpServices(registry, metadata);
		}
	}


	private static class TestGroupRegistry implements AbstractHttpServiceRegistrar.GroupRegistry {

		private final Map<String, StubGroup> groupMap = new LinkedHashMap<>();

		public Map<String, StubGroup> groupMap() {
			return this.groupMap;
		}

		@Override
		public GroupSpec forGroup(String name, ClientType clientType) {
			return new TestGroupSpec(name, clientType);
		}

		private class TestGroupSpec implements GroupSpec {

			private final String groupName;

			private final ClientType clientType;

			public TestGroupSpec(String groupName, ClientType clientType) {
				this.groupName = groupName;
				this.clientType = clientType;
			}

			@Override
			public GroupSpec register(Class<?>... serviceTypes) {
				getOrCreateGroup().httpServiceTypes().addAll(Arrays.asList(serviceTypes));
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(Class<?>... packageClasses) {
				getOrCreateGroup().packageClasses().addAll(Arrays.asList(packageClasses));
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(String... packageNames) {
				getOrCreateGroup().packageNames().addAll(Arrays.asList(packageNames));
				return this;
			}

			private StubGroup getOrCreateGroup() {
				return groupMap.computeIfAbsent(this.groupName, name -> new StubGroup(name, this.clientType));
			}
		}
	}


	private record StubGroup(
			String name, ClientType clientType, Set<Class<?>> httpServiceTypes,
			Set<Class<?>> packageClasses, Set<String> packageNames) implements HttpServiceGroup {

		StubGroup(String name, ClientType clientType) {
			this(name, clientType, new LinkedHashSet<>(), new LinkedHashSet<>(), new LinkedHashSet<>());
		}

		public static StubGroup ofListing(String name, Class<?>... httpServiceTypes) {
			return ofListing(name, ClientType.UNSPECIFIED, httpServiceTypes);
		}

		public static StubGroup ofListing(String name, ClientType clientType, Class<?>... httpServiceTypes) {
			StubGroup group = new StubGroup(name, clientType);
			group.httpServiceTypes().addAll(Arrays.asList(httpServiceTypes));
			return group;
		}

		public static StubGroup ofPackageClasses(String name, Class<?>... packageClasses) {
			return ofPackageClasses(name, ClientType.UNSPECIFIED, packageClasses);
		}

		public static StubGroup ofPackageClasses(String name, ClientType clientType, Class<?>... packageClasses) {
			StubGroup group = new StubGroup(name, clientType);
			group.packageClasses().addAll(Arrays.asList(packageClasses));
			return group;
		}
	}

}
