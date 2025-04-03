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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.service.invoker.HttpExchangeAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

/**
 * {@link FactoryBean} for {@link HttpServiceProxyRegistry} responsible for
 * initializing {@link HttpServiceGroup}s and creating the HTTP Service client
 * proxies for each group.
 *
 * <p>This class is imported as a bean definition through an
 * {@link AbstractHttpServiceRegistrar}.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 * @see AbstractHttpServiceRegistrar
 */
public final class HttpServiceProxyRegistryFactoryBean
		implements ApplicationContextAware, InitializingBean, FactoryBean<HttpServiceProxyRegistry> {

	private static final Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> groupAdapters =
			GroupAdapterInitializer.initGroupAdapters();


	private final Set<ProxyHttpServiceGroup> groupSet;

	private @Nullable ApplicationContext applicationContext;

	private @Nullable HttpServiceProxyRegistry proxyRegistry;


	HttpServiceProxyRegistryFactoryBean(GroupsMetadata groupsMetadata) {
		this.groupSet = groupsMetadata.groups().stream()
				.map(group -> {
					HttpServiceGroupAdapter<?> adapter = groupAdapters.get(group.clientType());
					Assert.state(adapter != null, "No HttpServiceGroupAdapter for type " + group.clientType());
					return new ProxyHttpServiceGroup(group, adapter);
				})
				.collect(Collectors.toSet());
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return HttpServiceProxyRegistry.class;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.applicationContext, "ApplicationContext not initialized");

		// Apply group configurers
		groupAdapters.forEach((clientType, groupAdapter) ->
				this.applicationContext.getBeanProvider(groupAdapter.getConfigurerType())
						.orderedStream()
						.forEach(configurer -> configurer.configureGroups(new DefaultGroups<>(clientType))));

		// Create proxies
		Map<String, Map<Class<?>, Object>> groupProxyMap = this.groupSet.stream()
				.collect(Collectors.toMap(ProxyHttpServiceGroup::name, ProxyHttpServiceGroup::createProxies));

		this.proxyRegistry = new DefaultHttpServiceProxyRegistry(groupProxyMap);
	}


	@Override
	public HttpServiceProxyRegistry getObject() {
		Assert.state(this.proxyRegistry != null, "HttpServiceProxyRegistry not initialized");
		return this.proxyRegistry;
	}


	private static class GroupAdapterInitializer {

		static Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> initGroupAdapters() {
			Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> map = new LinkedHashMap<>(2);

			addGroupAdapter(map, HttpServiceGroup.ClientType.REST_CLIENT,
					"org.springframework.web.client.support.RestClientHttpServiceGroupAdapter");

			addGroupAdapter(map, HttpServiceGroup.ClientType.WEB_CLIENT,
					"org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupAdapter");

			return map;
		}

		private static void addGroupAdapter(
				Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> groupAdapters,
				HttpServiceGroup.ClientType clientType, String className) {

			try {
				Class<?> clazz = ClassUtils.forName(className, HttpServiceGroupAdapter.class.getClassLoader());
				groupAdapters.put(clientType, (HttpServiceGroupAdapter<?>) BeanUtils.instantiateClass(clazz));
			}
			catch (ClassNotFoundException ex) {
				// ignore
			}
		}
	}


	/**
	 * {@link HttpServiceGroup} that creates client proxies.
	 */
	private static final class ProxyHttpServiceGroup implements HttpServiceGroup {

		private final HttpServiceGroup declaredGroup;

		private final HttpServiceGroupAdapter<?> groupAdapter;

		private final Object clientBuilder;

		private BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer = (group, builder) -> {};

		ProxyHttpServiceGroup(HttpServiceGroup group, HttpServiceGroupAdapter<?> groupAdapter) {
			this.declaredGroup = group;
			this.groupAdapter = groupAdapter;
			this.clientBuilder = groupAdapter.createClientBuilder();
		}

		@Override
		public String name() {
			return this.declaredGroup.name();
		}

		@Override
		public Set<Class<?>> httpServiceTypes() {
			return this.declaredGroup.httpServiceTypes();
		}

		@Override
		public ClientType clientType() {
			return this.declaredGroup.clientType();
		}

		@SuppressWarnings("unchecked")
		public <CB> void apply(
				BiConsumer<HttpServiceGroup, CB> clientConfigurer,
				BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer) {

			clientConfigurer.accept(this, (CB) this.clientBuilder);
			this.proxyFactoryConfigurer = this.proxyFactoryConfigurer.andThen(proxyFactoryConfigurer);
		}

		public Map<Class<?>, Object> createProxies() {
			Map<Class<?>, Object> map = new LinkedHashMap<>(httpServiceTypes().size());
			HttpExchangeAdapter exchangeAdapter = initExchangeAdapter();
			HttpServiceProxyFactory.Builder proxyFactoryBuilder = HttpServiceProxyFactory.builderFor(exchangeAdapter);
			this.proxyFactoryConfigurer.accept(this, proxyFactoryBuilder);
			HttpServiceProxyFactory factory = proxyFactoryBuilder.build();
			httpServiceTypes().forEach(type -> map.put(type, factory.createClient(type)));
			return map;
		}

		@SuppressWarnings("unchecked")
		private <CB> HttpExchangeAdapter initExchangeAdapter() {
			return ((HttpServiceGroupAdapter<CB>) this.groupAdapter).createExchangeAdapter((CB) this.clientBuilder);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[id=" + name() + "]";
		}
	}


	/**
	 * Default implementation of Groups that helps to configure the set of declared groups.
	 */
	private final class DefaultGroups<CB> implements HttpServiceGroupConfigurer.Groups<CB> {

		private Predicate<HttpServiceGroup> filter;

		DefaultGroups(HttpServiceGroup.ClientType clientType) {
			this.filter = group -> group.clientType().equals(clientType);
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<CB> filterByName(String... groupNames) {
			return filter(group -> Arrays.stream(groupNames).anyMatch(id -> id.equals(group.name())));
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<CB> filter(Predicate<HttpServiceGroup> predicate) {
			this.filter = this.filter.or(predicate);
			return this;
		}

		@Override
		public void configureClient(Consumer<CB> clientConfigurer) {
			configureClient((group, builder) -> clientConfigurer.accept(builder));
		}

		@Override
		public void configureClient(BiConsumer<HttpServiceGroup, CB> clientConfigurer) {
			configure(clientConfigurer, (group, builder) -> {});
		}

		@Override
		public void configureProxyFactory(
				BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer) {

			configure((group, builder) -> {}, proxyFactoryConfigurer);
		}

		@Override
		public void configure(
				BiConsumer<HttpServiceGroup, CB> clientConfigurer,
				BiConsumer<HttpServiceGroup, HttpServiceProxyFactory.Builder> proxyFactoryConfigurer) {

			groupSet.stream().filter(this.filter).forEach(group ->
					group.apply(clientConfigurer, proxyFactoryConfigurer));
		}
	}


	/**
	 * Default {@link HttpServiceProxyRegistry} with a map of proxies.
	 */
	private static final class DefaultHttpServiceProxyRegistry implements HttpServiceProxyRegistry {

		private final Map<String, Map<Class<?>, Object>> groupProxyMap;

		private final MultiValueMap<Class<?>, Object> directLookupMap;

		DefaultHttpServiceProxyRegistry(Map<String, Map<Class<?>, Object>> groupProxyMap) {
			this.groupProxyMap = groupProxyMap;
			this.directLookupMap = new LinkedMultiValueMap<>();
			groupProxyMap.values().forEach(map -> map.forEach(this.directLookupMap::add));
		}

		@SuppressWarnings("unchecked")
		@Override
		public <P> @Nullable P getClient(Class<P> type) {
			List<Object> proxies = this.directLookupMap.getOrDefault(type, Collections.emptyList());
			Assert.state(proxies.size() <= 1, "No unique client of type " + type.getName());
			return (!proxies.isEmpty() ? (P) proxies.get(0) : null);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <P> @Nullable P getClient(String groupName, Class<P> httpServiceType) {
			return (P) this.groupProxyMap.getOrDefault(groupName, Collections.emptyMap()).get(httpServiceType);
		}
	}

}
