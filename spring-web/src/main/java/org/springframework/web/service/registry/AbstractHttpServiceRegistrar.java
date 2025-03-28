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

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.web.service.annotation.HttpExchange;

/**
 * Abstract registrar class that imports:
 * <ul>
 * <li>Bean definitions for HTTP Service interface client proxies organized by
 * {@link HttpServiceGroup}.
 * <li>Bean definition for an {@link HttpServiceProxyRegistryFactoryBean} that
 * initializes the infrastructure for each group, {@code RestClient} or
 * {@code WebClient} and a proxy factory, necessary to create the proxies.
 * </ul>
 *
 * <p>Subclasses determine the HTTP Service types (interfaces with
 * {@link HttpExchange @HttpExchange} methods) to register by implementing
 * {@link #registerHttpServices}.
 *
 * <p>There is built-in support for declaring HTTP Services through
 * {@link ImportHttpServices} annotations. It is also possible to perform
 * registrations directly, sourced in another way, by extending this class.
 *
 * <p>It is possible to import multiple instances of this registrar type.
 * Subsequent imports update the existing registry {@code FactoryBean}
 * definition, and likewise merge HTTP Service group definitions.
 *
 * <p>An application can autowire HTTP Service proxy beans, or autowire the
 * {@link HttpServiceProxyRegistry} from which to obtain proxies.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @since 7.0
 * @see ImportHttpServices
 * @see HttpServiceProxyRegistryFactoryBean
 */
public abstract class AbstractHttpServiceRegistrar implements
		ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware {

	private HttpServiceGroup.ClientType defaultClientType = HttpServiceGroup.ClientType.UNSPECIFIED;

	private @Nullable Environment environment;

	private @Nullable ResourceLoader resourceLoader;

	private @Nullable BeanFactory beanFactory;

	private final Map<String, RegisteredGroup> groupMap = new LinkedHashMap<>();

	private @Nullable ClassPathScanningCandidateComponentProvider scanner;


	/**
	 * Set the client type to use when the client type for an HTTP Service group
	 * remains {@link HttpServiceGroup.ClientType#UNSPECIFIED}.
	 * <p>By default, when this property is not set, then {@code REST_CLIENT}
	 * is used for any HTTP Service group whose client type remains unspecified.
	 */
	public void setDefaultClientType(HttpServiceGroup.ClientType defaultClientType) {
		this.defaultClientType = defaultClientType;
	}

	@Override
	public void setEnvironment(Environment environment) {
		this.environment = environment;
	}

	@Override
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}


	@Override
	public final void registerBeanDefinitions(
			AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry beanRegistry,
			BeanNameGenerator beanNameGenerator) {

		registerHttpServices(new DefaultGroupRegistry(), importingClassMetadata);

		String proxyRegistryBeanName = HttpServiceProxyRegistry.class.getName();
		GenericBeanDefinition proxyRegistryBeanDef;

		if (!beanRegistry.containsBeanDefinition(proxyRegistryBeanName)) {
			proxyRegistryBeanDef = new GenericBeanDefinition();
			proxyRegistryBeanDef.setBeanClass(HttpServiceProxyRegistryFactoryBean.class);
			ConstructorArgumentValues args = proxyRegistryBeanDef.getConstructorArgumentValues();
			args.addIndexedArgumentValue(0, new LinkedHashMap<String, HttpServiceGroup>());
			beanRegistry.registerBeanDefinition(proxyRegistryBeanName, proxyRegistryBeanDef);
		}
		else {
			proxyRegistryBeanDef = (GenericBeanDefinition) beanRegistry.getBeanDefinition(proxyRegistryBeanName);
		}

		mergeHttpServices(proxyRegistryBeanDef);

		this.groupMap.forEach((groupName, group) -> group.httpServiceTypeNames().forEach(type -> {
			GenericBeanDefinition proxyBeanDef = new GenericBeanDefinition();
			proxyBeanDef.setBeanClassName(type);
			String beanName = (groupName + "." + beanNameGenerator.generateBeanName(proxyBeanDef, beanRegistry));
			proxyBeanDef.setInstanceSupplier(() -> getProxyInstance(proxyRegistryBeanName, groupName, type));
			if (!beanRegistry.containsBeanDefinition(beanName)) {
				beanRegistry.registerBeanDefinition(beanName, proxyBeanDef);
			}
		}));
	}

	@Override
	public final void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
	}

	/**
	 * This method is called before any bean definition registrations are made.
	 * Subclasses must implement it to register the HTTP Services for which bean
	 * definitions for which proxies need to be created.
	 * @param registry to perform HTTP Service registrations with
	 * @param importingClassMetadata annotation metadata of the importing class
	 */
	protected abstract void registerHttpServices(
			GroupRegistry registry, AnnotationMetadata importingClassMetadata);

	private ClassPathScanningCandidateComponentProvider getScanner() {
		if (this.scanner == null) {
			Assert.state(this.environment != null, "Environment has not been set");
			Assert.state(this.resourceLoader != null, "ResourceLoader has not been set");
			this.scanner = new HttpExchangeClassPathScanningCandidateComponentProvider();
			this.scanner.setEnvironment(this.environment);
			this.scanner.setResourceLoader(this.resourceLoader);
		}
		return this.scanner;
	}

	@SuppressWarnings("unchecked")
	private void mergeHttpServices(GenericBeanDefinition proxyRegistryBeanDef) {
		ConstructorArgumentValues args = proxyRegistryBeanDef.getConstructorArgumentValues();
		ConstructorArgumentValues.ValueHolder valueHolder = args.getArgumentValue(0, Map.class);
		Assert.state(valueHolder != null, "Expected Map constructor argument at index 0");
		Map<String, RegisteredGroup> targetMap = (Map<String, RegisteredGroup>) valueHolder.getValue();
		Assert.state(targetMap != null, "No constructor argument value");

		this.groupMap.forEach((name, group) -> {
			RegisteredGroup previousGroup = targetMap.putIfAbsent(name, group);
			if (previousGroup != null) {
				if (!compatibleClientTypes(group.clientType(), previousGroup.clientType())) {
					throw new IllegalArgumentException("ClientType conflict for group '" + name + "'");
				}
				previousGroup.addHttpServiceTypeNames(group.httpServiceTypeNames());
			}
		});
	}

	private static boolean compatibleClientTypes(
			HttpServiceGroup.ClientType clientTypeA, HttpServiceGroup.ClientType clientTypeB) {

		return (clientTypeA == clientTypeB ||
				clientTypeA == HttpServiceGroup.ClientType.UNSPECIFIED ||
				clientTypeB == HttpServiceGroup.ClientType.UNSPECIFIED);
	}

	private Object getProxyInstance(String registryBeanName, String groupName, String type) {
		Assert.state(this.beanFactory != null, "BeanFactory has not been set");
		HttpServiceProxyRegistry registry = this.beanFactory.getBean(registryBeanName, HttpServiceProxyRegistry.class);
		Object proxy = registry.getClient(groupName, loadClass(type));
		Assert.notNull(proxy, "No proxy for HTTP Service [" + type + "]");
		return proxy;
	}

	private static Class<?> loadClass(String type) {
		try {
			return ClassUtils.forName(type, AbstractHttpServiceRegistrar.class.getClassLoader());
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("Failed to load '" + type + "'", ex);
		}
	}


	/**
	 * Registry API to allow subclasses to register HTTP Services.
	 */
	protected interface GroupRegistry {

		/**
		 * Perform HTTP Service registrations for the given group.
		 */
		GroupSpec forGroup(String name);

		/**
		 * Variant of {@link #forGroup(String)} with a client type.
		 */
		GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType);

		/**
		 * Perform HTTP Service registrations for the
		 * {@link HttpServiceGroup#DEFAULT_GROUP_NAME} group.
		 */
		default GroupSpec forDefaultGroup() {
			return forGroup(HttpServiceGroup.DEFAULT_GROUP_NAME);
		}

		/**
		 * Spec to list or scan for HTTP Service types.
		 */
		interface GroupSpec {

			/**
			 * List HTTP Service types to create proxies for.
			 */
			GroupSpec register(Class<?>... serviceTypes);

			/**
			 * Detect HTTP Service types in the given packages, looking for
			 * interfaces with a type and/or method {@link HttpExchange} annotation.
			 */
			GroupSpec detectInBasePackages(Class<?>... packageClasses);

			/**
			 * Variant of {@link #detectInBasePackages(Class[])} with a String package name.
			 */
			GroupSpec detectInBasePackages(String... packageNames);

		}
	}


	/**
	 * Default implementation of {@link GroupRegistry}.
	 */
	private class DefaultGroupRegistry implements GroupRegistry {

		@Override
		public GroupSpec forGroup(String name) {
			return forGroup(name, HttpServiceGroup.ClientType.UNSPECIFIED);
		}

		@Override
		public GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType) {
			return new DefaultGroupSpec(name, clientType);
		}

		private class DefaultGroupSpec implements GroupSpec {

			private final String groupName;

			private final HttpServiceGroup.ClientType clientType;

			public DefaultGroupSpec(String groupName, HttpServiceGroup.ClientType clientType) {
				this.groupName = groupName;
				this.clientType = initClientType(clientType);
			}

			private HttpServiceGroup.ClientType initClientType(HttpServiceGroup.ClientType clientType) {
				if (clientType != HttpServiceGroup.ClientType.UNSPECIFIED) {
					return clientType;
				}
				else if (defaultClientType != HttpServiceGroup.ClientType.UNSPECIFIED) {
					return defaultClientType;
				}
				else {
					return HttpServiceGroup.ClientType.REST_CLIENT;
				}
			}

			@Override
			public GroupSpec register(Class<?>... serviceTypes) {
				getOrCreateGroup(groupName, clientType).addHttpServiceTypes(serviceTypes);
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(Class<?>... packageClasses) {
				for (Class<?> packageClass : packageClasses) {
					detect(this.groupName, this.clientType, packageClass.getPackageName());
				}
				return this;
			}

			@Override
			public GroupSpec detectInBasePackages(String... packageNames) {
				for (String packageName : packageNames) {
					detect(this.groupName, this.clientType, packageName);
				}
				return this;
			}

			private void detect(String groupName, HttpServiceGroup.ClientType clientType, String packageName) {
				for (BeanDefinition definition : getScanner().findCandidateComponents(packageName)) {
					if (definition.getBeanClassName() != null) {
						getOrCreateGroup(groupName, clientType).addHttpServiceTypeName(definition.getBeanClassName());
					}
				}
			}

			private RegisteredGroup getOrCreateGroup(String groupName, HttpServiceGroup.ClientType clientType) {
				return groupMap.computeIfAbsent(groupName, name -> new RegisteredGroup(name, clientType));
			}
		}
	}


	/**
	 * A simple holder of registered HTTP Service type names, deferring the
	 * loading of classes until {@link #httpServiceTypes()} is called.
	 */
	private static class RegisteredGroup implements HttpServiceGroup {

		private final String name;

		private final Set<String> httpServiceTypeNames = new LinkedHashSet<>();

		private final ClientType clientType;

		public RegisteredGroup(String name, ClientType clientType) {
			this.name = name;
			this.clientType = clientType;
		}

		@Override
		public String name() {
			return this.name;
		}

		public Set<String> httpServiceTypeNames() {
			return this.httpServiceTypeNames;
		}

		@Override
		public Set<Class<?>> httpServiceTypes() {
			return httpServiceTypeNames.stream()
					.map(AbstractHttpServiceRegistrar::loadClass)
					.collect(Collectors.toSet());
		}

		@Override
		public ClientType clientType() {
			return this.clientType;
		}

		public void addHttpServiceTypes(Class<?>... httpServiceTypes) {
			for (Class<?> type : httpServiceTypes) {
				this.httpServiceTypeNames.add(type.getName());
			}
		}

		public void addHttpServiceTypeNames(Collection<String> httpServiceTypeNames) {
			this.httpServiceTypeNames.addAll(httpServiceTypeNames);
		}

		public void addHttpServiceTypeName(String httpServiceTypeName) {
			this.httpServiceTypeNames.add(httpServiceTypeName);
		}

		@Override
		public final boolean equals(Object other) {
			return (other instanceof RegisteredGroup otherGroup && this.name.equals(otherGroup.name));
		}

		@Override
		public int hashCode() {
			return this.name.hashCode();
		}

		@Override
		public String toString() {
			return "RegisteredGroup[name='" + this.name + "', httpServiceTypes=" +
					this.httpServiceTypeNames + ", clientType=" + this.clientType + "]";
		}
	}


	/**
	 * Extension of ClassPathScanningCandidateComponentProvider to look for HTTP Services.
	 */
	private static class HttpExchangeClassPathScanningCandidateComponentProvider
			extends ClassPathScanningCandidateComponentProvider {

		public HttpExchangeClassPathScanningCandidateComponentProvider() {
			addIncludeFilter(new HttpExchangeFilter());
		}

		@Override
		protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
			AnnotationMetadata metadata = beanDefinition.getMetadata();
			return (metadata.isIndependent() && !metadata.isAnnotation());
		}

		/**
		 * Find interfaces with type and/or method {@code @HttpExchange}.
		 */
		private static class HttpExchangeFilter extends AnnotationTypeFilter {

			public HttpExchangeFilter() {
				super(HttpExchange.class, true, true);
			}

			@Override
			protected boolean matchSelf(MetadataReader metadataReader) {
				if (metadataReader.getClassMetadata().isInterface()) {
					for (MethodMetadata metadata : metadataReader.getAnnotationMetadata().getDeclaredMethods()) {
						if (metadata.getAnnotations().isPresent(HttpExchange.class)) {
							return true;
						}
					}
				}
				return false;
			}
		}
	}

}
