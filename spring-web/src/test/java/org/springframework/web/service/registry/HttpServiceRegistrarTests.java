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

import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.web.service.registry.HttpServiceGroup.ClientType;
import org.springframework.web.service.registry.echo.EchoA;
import org.springframework.web.service.registry.echo.EchoB;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Unit tests for {@link AbstractHttpServiceRegistrar}.
 * @author Rossen Stoyanchev
 */
@SuppressWarnings("unchecked")
public class HttpServiceRegistrarTests {

	private static final String ECHO_GROUP = "echo";


	private final SimpleBeanDefinitionRegistry beanDefRegistry = new SimpleBeanDefinitionRegistry();


	@Test
	void basicListing() {
		doRegister(registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class, EchoB.class));

		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, EchoA.class, EchoB.class));
		assertProxyBeanDef(ECHO_GROUP, EchoA.class);
		assertProxyBeanDef(ECHO_GROUP, EchoB.class);
		assertBeanDefinitionCount(3);
	}

	@Test
	void basicScan() {
		doRegister(registry -> registry.forGroup(ECHO_GROUP).detectInBasePackages(EchoA.class));

		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, EchoA.class, EchoB.class));
		assertProxyBeanDef(ECHO_GROUP, EchoA.class);
		assertProxyBeanDef(ECHO_GROUP, EchoB.class);
		assertBeanDefinitionCount(3);
	}

	@Test
	void merge() {
		doRegister(
				registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class),
				registry -> registry.forGroup(ECHO_GROUP).register(EchoB.class));

		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, EchoA.class, EchoB.class));
		assertProxyBeanDef(ECHO_GROUP, EchoA.class);
		assertProxyBeanDef(ECHO_GROUP, EchoB.class);
		assertBeanDefinitionCount(3);
	}

	@Test
	void mergeWithOverlap() {
		doRegister(
				registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class),
				registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class));

		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, EchoA.class));
		assertProxyBeanDef(ECHO_GROUP, EchoA.class);
		assertBeanDefinitionCount(2);
	}

	@Test
	void mergeWithClientTypeConflict() {
		assertThatIllegalArgumentException().isThrownBy(() -> doRegister(
				registry -> registry.forGroup(ECHO_GROUP, ClientType.REST_CLIENT).register(EchoA.class),
				registry -> registry.forGroup(ECHO_GROUP, ClientType.WEB_CLIENT).register(EchoB.class)));
	}

	@Test
	void mergeWithClientTypeOverride() {
		doRegister(
				registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class),
				registry -> registry.forGroup(ECHO_GROUP, ClientType.WEB_CLIENT).register(EchoA.class));

		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, ClientType.WEB_CLIENT, EchoA.class));
		assertProxyBeanDef(ECHO_GROUP, EchoA.class);
		assertBeanDefinitionCount(2);
	}

	@Test
	void defaultClientType() {
		doRegister(ClientType.WEB_CLIENT, registry -> registry.forGroup(ECHO_GROUP).register(EchoA.class));
		assertRegistryBeanDef(new TestGroup(ECHO_GROUP, ClientType.WEB_CLIENT, EchoA.class));
	}

	@Test
	void noRegistrations() {
		doRegister(registry -> {});
		assertBeanDefinitionCount(0);
	}


	@SuppressWarnings("unchecked")
	private void doRegister(Consumer<AbstractHttpServiceRegistrar.GroupRegistry>... registrars) {
		doRegister(ClientType.UNSPECIFIED, registrars);
	}

	@SuppressWarnings("DataFlowIssue")
	private void doRegister(ClientType clientType, Consumer<AbstractHttpServiceRegistrar.GroupRegistry>... consumers) {
		for (Consumer<AbstractHttpServiceRegistrar.GroupRegistry> consumer : consumers) {
			TestRegistrar registrar = new TestRegistrar(consumer, clientType);
			registrar.registerBeanDefinitions(null, beanDefRegistry);
		}
	}

	private void assertRegistryBeanDef(HttpServiceGroup... expectedGroups) {
		Map<String, HttpServiceGroup> groupMap = groupMap();
		assertThat(groupMap.size()).isEqualTo(expectedGroups.length);
		for (HttpServiceGroup expected : expectedGroups) {
			HttpServiceGroup actual = groupMap.get(expected.name());
			assertThat(actual.httpServiceTypes()).isEqualTo(expected.httpServiceTypes());
			assertThat(actual.clientType()).isEqualTo(expected.clientType());
		}
	}

	private Map<String, HttpServiceGroup> groupMap() {
		BeanDefinition beanDef = this.beanDefRegistry.getBeanDefinition(AbstractHttpServiceRegistrar.HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME);
		assertThat(beanDef.getBeanClassName()).isEqualTo(HttpServiceProxyRegistryFactoryBean.class.getName());

		ConstructorArgumentValues args = beanDef.getConstructorArgumentValues();
		ConstructorArgumentValues.ValueHolder valueHolder = args.getArgumentValue(0, Map.class);
		assertThat(valueHolder).isNotNull();

		GroupsMetadata metadata = (GroupsMetadata) valueHolder.getValue();
		assertThat(metadata).isNotNull();

		return metadata.groups(null).stream()
				.collect(Collectors.toMap(HttpServiceGroup::name, Function.identity()));
	}

	private void assertProxyBeanDef(String group, Class<?> httpServiceType) {
		String beanName = group + "#" + httpServiceType.getName();
		assertThat(this.beanDefRegistry.containsBeanDefinition(beanName)).isTrue();
		BeanDefinition beanDef = this.beanDefRegistry.getBeanDefinition(beanName);
		assertThat(beanDef.getBeanClassName()).isEqualTo(httpServiceType.getName());

	}

	private void assertBeanDefinitionCount(int count) {
		assertThat(beanDefRegistry.getBeanDefinitionCount()).isEqualTo(count);
	}


	private static class TestRegistrar extends AbstractHttpServiceRegistrar {

		private final Consumer<GroupRegistry> registrar;

		TestRegistrar(Consumer<GroupRegistry> registrar, ClientType clientType) {
			this.registrar = registrar;
			setDefaultClientType(clientType);
			setEnvironment(new StandardEnvironment());
			setResourceLoader(new PathMatchingResourcePatternResolver());
		}

		@Override
		protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {
			this.registrar.accept(registry);
		}
	}

	private record TestGroup(String name, Set<Class<?>> httpServiceTypes, ClientType clientType)
			implements HttpServiceGroup {

		TestGroup(String name, Class<?>... httpServiceTypes) {
			this(name, Set.of(httpServiceTypes), ClientType.REST_CLIENT);
		}

		TestGroup(String name, ClientType clientType, Class<?>... httpServiceTypes) {
			this(name, Set.of(httpServiceTypes), clientType);
		}
	}
}
