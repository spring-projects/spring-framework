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
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.aot.hint.TypeReference;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
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
		implements ApplicationContextAware, BeanClassLoaderAware, InitializingBean,
		FactoryBean<HttpServiceProxyRegistry> {

	private static final Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> groupAdapters =
			GroupAdapterInitializer.initGroupAdapters();


	private final GroupsMetadata groupsMetadata;

	private @Nullable ApplicationContext applicationContext;

	private @Nullable ClassLoader beanClassLoader;

	private @Nullable HttpServiceProxyRegistry proxyRegistry;


	HttpServiceProxyRegistryFactoryBean(GroupsMetadata groupsMetadata) {
		this.groupsMetadata = groupsMetadata;
	}


	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}

	@Override
	public Class<?> getObjectType() {
		return HttpServiceProxyRegistry.class;
	}


	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.applicationContext, "ApplicationContext not initialized");
		Assert.notNull(this.beanClassLoader, "BeanClassLoader not initialized");

		// Create the groups from the metadata
		Set<ProxyHttpServiceGroup> groups = this.groupsMetadata.groups(this.beanClassLoader).stream()
				.map(ProxyHttpServiceGroup::new)
				.collect(Collectors.toSet());

		// Apply group configurers
		groupAdapters.forEach((clientType, groupAdapter) ->
				this.applicationContext.getBeanProvider(groupAdapter.getConfigurerType())
						.orderedStream()
						.forEach(configurer -> configurer.configureGroups(new DefaultGroups<>(groups, clientType))));

		// Create proxies
		Map<String, Map<Class<?>, Object>> proxies = groups.stream()
				.collect(Collectors.toMap(ProxyHttpServiceGroup::name, ProxyHttpServiceGroup::createProxies));

		this.proxyRegistry = new DefaultHttpServiceProxyRegistry(proxies);
	}


	@Override
	public HttpServiceProxyRegistry getObject() {
		Assert.state(this.proxyRegistry != null, "HttpServiceProxyRegistry not initialized");
		return this.proxyRegistry;
	}


	private static class GroupAdapterInitializer {

		private static final String REST_CLIENT_HTTP_SERVICE_GROUP_ADAPTER = "org.springframework.web.client.support.RestClientHttpServiceGroupAdapter";

		private static final String WEB_CLIENT_HTTP_SERVICE_GROUP_ADAPTER = "org.springframework.web.reactive.function.client.support.WebClientHttpServiceGroupAdapter";

		static Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> initGroupAdapters() {
			Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> map = new LinkedHashMap<>(2);

			addGroupAdapter(map, HttpServiceGroup.ClientType.REST_CLIENT, REST_CLIENT_HTTP_SERVICE_GROUP_ADAPTER);
			addGroupAdapter(map, HttpServiceGroup.ClientType.WEB_CLIENT, WEB_CLIENT_HTTP_SERVICE_GROUP_ADAPTER);

			return map;
		}

		private static void addGroupAdapter(
				Map<HttpServiceGroup.ClientType, HttpServiceGroupAdapter<?>> groupAdapters,
				HttpServiceGroup.ClientType clientType, String className) {

			try {
				Class<?> clazz = ClassUtils.forName(className, HttpServiceGroupAdapter.class.getClassLoader());
				groupAdapters.put(clientType, (HttpServiceGroupAdapter<?>) BeanUtils.instantiateClass(clazz));
			}
			catch (ClassNotFoundException ignored) {
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

		private final HttpServiceProxyFactory.Builder proxyFactoryBuilder = HttpServiceProxyFactory.builder();

		ProxyHttpServiceGroup(HttpServiceGroup group) {
			this.declaredGroup = group;
			this.groupAdapter = getGroupAdapter(group.clientType());
			this.clientBuilder = this.groupAdapter.createClientBuilder();
		}

		private static HttpServiceGroupAdapter<?> getGroupAdapter(HttpServiceGroup.ClientType clientType) {
			HttpServiceGroupAdapter<?> adapter = groupAdapters.get(clientType);
			Assert.state(adapter != null, "No HttpServiceGroupAdapter for type " + clientType);
			return adapter;
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
		public <CB> void applyConfigurer(HttpServiceGroupConfigurer.GroupCallback<CB> callback) {
			callback.withGroup(this, (CB) this.clientBuilder, this.proxyFactoryBuilder);
		}

		public Map<Class<?>, Object> createProxies() {
			Map<Class<?>, Object> map = new LinkedHashMap<>(httpServiceTypes().size());
			HttpServiceProxyFactory factory = this.proxyFactoryBuilder.exchangeAdapter(initExchangeAdapter()).build();
			httpServiceTypes().forEach(type -> map.put(type, factory.createClient(type)));
			return map;
		}

		@SuppressWarnings("unchecked")
		private <CB> HttpExchangeAdapter initExchangeAdapter() {
			return ((HttpServiceGroupAdapter<CB>) this.groupAdapter).createExchangeAdapter((CB) this.clientBuilder);
		}

		@Override
		public String toString() {
			return getClass().getSimpleName() + "[name=" + name() + "]";
		}

	}


	/**
	 * Default implementation of Groups that helps to configure the set of declared groups.
	 */
	private static final class DefaultGroups<CB> implements HttpServiceGroupConfigurer.Groups<CB> {

		private final Set<ProxyHttpServiceGroup> groups;

		private final Predicate<HttpServiceGroup> defaultFilter;

		private Predicate<HttpServiceGroup> filter;

		DefaultGroups(Set<ProxyHttpServiceGroup> groups, HttpServiceGroup.ClientType clientType) {
			this.groups = groups;
			this.defaultFilter = (group -> group.clientType().equals(clientType));
			this.filter = this.defaultFilter;
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<CB> filterByName(String... groupNames) {
			return filter(group -> Arrays.stream(groupNames).anyMatch(name -> name.equals(group.name())));
		}

		@Override
		public HttpServiceGroupConfigurer.Groups<CB> filter(Predicate<HttpServiceGroup> predicate) {
			this.filter = this.filter.and(predicate);
			return this;
		}

		@Override
		public void forEachClient(HttpServiceGroupConfigurer.ClientCallback<CB> callback) {
			forEachGroup((group, clientBuilder, factoryBuilder) -> callback.withClient(group, clientBuilder));
		}

		@Override
		public void forEachProxyFactory(HttpServiceGroupConfigurer.ProxyFactoryCallback callback) {
			forEachGroup((group, clientBuilder, factoryBuilder) -> callback.withProxyFactory(group, factoryBuilder));
		}

		@Override
		public void forEachGroup(HttpServiceGroupConfigurer.GroupCallback<CB> callback) {
			this.groups.stream().filter(this.filter).forEach(group -> group.applyConfigurer(callback));
			this.filter = this.defaultFilter; // reset the filter (terminal method)
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
		public <P> P getClient(Class<P> type) {
			List<Object> map = this.directLookupMap.getOrDefault(type, Collections.emptyList());
			Assert.notEmpty(map, "No client of type " + type.getName());
			Assert.isTrue(map.size() <= 1, "No unique client of type " + type.getName());
			return (P) map.get(0);
		}

		@SuppressWarnings("unchecked")
		@Override
		public <P> P getClient(String groupName, Class<P> type) {
			Map<Class<?>, Object> map = getProxyMapForGroup(groupName);
			P proxy = (P) map.get(type);
			Assert.notNull(proxy, "No client of type " + type + " in group '" + groupName + "': " + map.keySet());
			return proxy;
		}

		@Override
		public Set<String> getGroupNames() {
			return this.groupProxyMap.keySet();
		}

		@Override
		public Set<Class<?>> getClientTypesInGroup(String groupName) {
			return getProxyMapForGroup(groupName).keySet();
		}

		private Map<Class<?>, Object> getProxyMapForGroup(String groupName) {
			Map<Class<?>, Object> map = this.groupProxyMap.get(groupName);
			Assert.notNull(map, "No group with name '" + groupName + "'");
			return map;
		}
	}

	static class HttpServiceProxyRegistryRuntimeHints implements RuntimeHintsRegistrar {

		@Override
		public void registerHints(RuntimeHints hints, @Nullable ClassLoader classLoader) {
			hints.reflection()
					.registerType(TypeReference.of(GroupAdapterInitializer.REST_CLIENT_HTTP_SERVICE_GROUP_ADAPTER),
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS)
					.registerTypeIfPresent(classLoader, GroupAdapterInitializer.WEB_CLIENT_HTTP_SERVICE_GROUP_ADAPTER,
							MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS);
		}

	}

}
