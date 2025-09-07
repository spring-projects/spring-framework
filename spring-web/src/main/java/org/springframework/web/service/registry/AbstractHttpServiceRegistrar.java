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

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.beans.factory.support.RootBeanDefinition;
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
 * @author Olga Maciaszek-Sharma
 * @author Stephane Nicoll
 * @since 7.0
 * @see ImportHttpServices
 * @see HttpServiceProxyRegistryFactoryBean
 */
public abstract class AbstractHttpServiceRegistrar implements
		ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanFactoryAware, BeanClassLoaderAware {

	/**
	 * The bean name of the {@link HttpServiceProxyRegistry}.
	 */
	public static final String HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME = "httpServiceProxyRegistry";

	static final String HTTP_SERVICE_GROUP_NAME_ATTRIBUTE = "httpServiceGroupName";

	private HttpServiceGroup.ClientType defaultClientType = HttpServiceGroup.ClientType.UNSPECIFIED;

	private @Nullable Environment environment;

	private @Nullable ResourceLoader resourceLoader;

	private @Nullable BeanFactory beanFactory;

	private @Nullable ClassLoader beanClassLoader;

	private final GroupsMetadata groupsMetadata = new GroupsMetadata();

	private @Nullable ClassPathScanningCandidateComponentProvider scanner;


	/**
	 * Set the client type to use when an HTTP Service group's client type
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
	public void setBeanClassLoader(ClassLoader beanClassLoader) {
		this.beanClassLoader = beanClassLoader;
	}


	@Override
	public final void registerBeanDefinitions(
			AnnotationMetadata metadata, BeanDefinitionRegistry registry, BeanNameGenerator generator) {

		registerBeanDefinitions(metadata, registry);
	}

	@Override
	public final void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry beanRegistry) {

		registerHttpServices(new DefaultGroupRegistry(), metadata);

		if (this.groupsMetadata.isEmpty()) {
			return;
		}

		RootBeanDefinition proxyRegistryBeanDef = createOrGetRegistry(beanRegistry);
		mergeGroups(proxyRegistryBeanDef);

		this.groupsMetadata.forEachRegistration((groupName, types) -> types.forEach(type -> {
			RootBeanDefinition proxyBeanDef = new RootBeanDefinition();
			proxyBeanDef.setBeanClassName(type);
			proxyBeanDef.setAttribute(HTTP_SERVICE_GROUP_NAME_ATTRIBUTE, groupName);
			proxyBeanDef.setInstanceSupplier(() -> getProxyInstance(groupName, type));
			String beanName = (groupName + "#" + type);
			if (!beanRegistry.containsBeanDefinition(beanName)) {
				beanRegistry.registerBeanDefinition(beanName, proxyBeanDef);
			}
		}));
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

	private RootBeanDefinition createOrGetRegistry(BeanDefinitionRegistry beanRegistry) {
		if (!beanRegistry.containsBeanDefinition(HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME)) {
			RootBeanDefinition proxyRegistryBeanDef = new RootBeanDefinition();
			proxyRegistryBeanDef.setBeanClass(HttpServiceProxyRegistryFactoryBean.class);
			ConstructorArgumentValues args = proxyRegistryBeanDef.getConstructorArgumentValues();
			args.addIndexedArgumentValue(0, new GroupsMetadata());
			beanRegistry.registerBeanDefinition(HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME, proxyRegistryBeanDef);
			return proxyRegistryBeanDef;
		}
		else {
			return (RootBeanDefinition) beanRegistry.getBeanDefinition(HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME);
		}
	}

	private void mergeGroups(RootBeanDefinition proxyRegistryBeanDef) {
		ConstructorArgumentValues args = proxyRegistryBeanDef.getConstructorArgumentValues();
		ConstructorArgumentValues.ValueHolder valueHolder = args.getArgumentValue(0, GroupsMetadata.class);
		Assert.state(valueHolder != null, "Expected GroupsMetadata constructor argument at index 0");
		GroupsMetadata target = (GroupsMetadata) valueHolder.getValue();
		Assert.state(target != null, "No constructor argument value");
		target.mergeWith(this.groupsMetadata);
	}

	private Object getProxyInstance(String groupName, String httpServiceType) {
		Assert.state(this.beanFactory != null, "BeanFactory has not been set");
		HttpServiceProxyRegistry registry = this.beanFactory.getBean(HTTP_SERVICE_PROXY_REGISTRY_BEAN_NAME, HttpServiceProxyRegistry.class);
		return registry.getClient(groupName, ClassUtils.resolveClassName(httpServiceType, this.beanClassLoader));
	}

	/**
	 * Find HTTP Service types under the given base package, looking for
	 * interfaces with type or method {@link HttpExchange} annotations.
	 * @param basePackage the names of packages to look under
	 * @return match bean definitions
	 */
	protected Stream<BeanDefinition> findHttpServices(String basePackage) {
		if (this.scanner == null) {
			Assert.state(this.environment != null, "Environment has not been set");
			Assert.state(this.resourceLoader != null, "ResourceLoader has not been set");
			this.scanner = new HttpExchangeClassPathScanningCandidateComponentProvider();
			this.scanner.setEnvironment(this.environment);
			this.scanner.setResourceLoader(this.resourceLoader);
		}
		return this.scanner.findCandidateComponents(basePackage).stream();
	}


	/**
	 * Registry API to allow subclasses to register HTTP Services.
	 */
	protected interface GroupRegistry {

		/**
		 * Perform HTTP Service registrations for the given group, either
		 * creating the group if it does not exist, or updating the existing one.
		 */
		default GroupSpec forGroup(String name) {
			return forGroup(name, HttpServiceGroup.ClientType.UNSPECIFIED);
		}

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
			 * Register HTTP Service types associated with this group.
			 */
			GroupSpec register(Class<?>... serviceTypes);

			/**
			 * Register HTTP Service types using fully qualified type names.
			 */
			GroupSpec registerTypeNames(String... serviceTypes);

			/**
			 * Detect HTTP Service types in the given packages, looking for
			 * interfaces with type or method {@link HttpExchange} annotations.
			 * <p>The performed scan, however, filters out any interfaces
			 * annotated with {@link HttpServiceClient} that are instead supported
			 * by {@link AbstractClientHttpServiceRegistrar}.
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
		public GroupSpec forGroup(String name, HttpServiceGroup.ClientType clientType) {
			return new DefaultGroupSpec(name, clientType);
		}

		private class DefaultGroupSpec implements GroupSpec {

			private final GroupsMetadata.Registration registration;

			DefaultGroupSpec(String groupName, HttpServiceGroup.ClientType clientType) {
				clientType = (clientType != HttpServiceGroup.ClientType.UNSPECIFIED ? clientType : defaultClientType);
				this.registration = groupsMetadata.getOrCreateGroup(groupName, clientType);
			}

			@Override
			public GroupRegistry.GroupSpec register(Class<?>... serviceTypes) {
				Arrays.stream(serviceTypes).map(Class::getName).forEach(this::registerServiceTypeName);
				return this;
			}

			@Override
			public GroupRegistry.GroupSpec registerTypeNames(String... serviceTypes) {
				Arrays.stream(serviceTypes).forEach(this::registerServiceTypeName);
				return this;
			}

			@Override
			public GroupRegistry.GroupSpec detectInBasePackages(Class<?>... packageClasses) {
				Arrays.stream(packageClasses).map(Class::getPackageName).forEach(this::detectInBasePackage);
				return this;
			}

			@Override
			public GroupRegistry.GroupSpec detectInBasePackages(String... packageNames) {
				Arrays.stream(packageNames).forEach(this::detectInBasePackage);
				return this;
			}

			private void detectInBasePackage(String packageName) {
				findHttpServices(packageName)
						.filter(DefaultGroupSpec::isNotHttpServiceClientAnnotated)
						.map(BeanDefinition::getBeanClassName)
						.filter(Objects::nonNull)
						.forEach(this::registerServiceTypeName);
			}

			private static boolean isNotHttpServiceClientAnnotated(BeanDefinition defintion) {
				if (defintion instanceof AnnotatedBeanDefinition abd) {
					return !abd.getMetadata().hasAnnotation(HttpServiceClient.class.getName());
				}
				return true;
			}

			private void registerServiceTypeName(String httpServiceTypeName) {
				this.registration.httpServiceTypeNames().add(httpServiceTypeName);
			}
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
