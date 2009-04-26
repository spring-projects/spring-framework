/*
 * Copyright 2002-2009 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.context.annotation;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.io.Resource;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Reads a given fully-populated configuration model, registering bean definitions
 * with the given {@link BeanDefinitionRegistry} based on its contents.
 *
 * <p>This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does
 * not implement/extend any of its artifacts as a configuration model is not a {@link Resource}.
 *
 * @author Chris Beams
 * @author Juergen Hoeller
 * @since 3.0
 */
class ConfigurationClassBeanDefinitionReader {

	private static final Log logger = LogFactory.getLog(ConfigurationClassBeanDefinitionReader.class);

	private final BeanDefinitionRegistry registry;


	public ConfigurationClassBeanDefinitionReader(BeanDefinitionRegistry registry) {
		Assert.notNull(registry, "BeanDefinitionRegistry must not be null");
		this.registry = registry;
	}


	/**
	 * Reads {@code configurationModel}, registering bean definitions with {@link #registry}
	 * based on its contents.
	 */
	public void loadBeanDefinitions(Set<ConfigurationClass> configurationModel) {
		for (ConfigurationClass configClass : configurationModel) {
			loadBeanDefinitionsForConfigurationClass(configClass);
		}
	}

	/**
	 * Reads a particular {@link ConfigurationClass}, registering bean definitions for the
	 * class itself, all its {@link Bean} methods
	 */
	private void loadBeanDefinitionsForConfigurationClass(ConfigurationClass configClass) {
		doLoadBeanDefinitionForConfigurationClass(configClass);
		for (ConfigurationClassMethod method : configClass.getConfigurationMethods()) {
			loadBeanDefinitionsForModelMethod(method);
		}
	}

	/**
	 * Registers the {@link Configuration} class itself as a bean definition.
	 */
	private void doLoadBeanDefinitionForConfigurationClass(ConfigurationClass configClass) {
		if (configClass.getBeanName() == null) {
			GenericBeanDefinition configBeanDef = new GenericBeanDefinition();
			configBeanDef.setBeanClassName(configClass.getMetadata().getClassName());
			String configBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(configBeanDef, registry);
			configClass.setBeanName(configBeanName);
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("Registered bean definition for imported @Configuration class %s", configBeanName));
			}
		}
	}

	/**
	 * Reads a particular {@link ConfigurationClassMethod}, registering bean definitions with
	 * {@link #registry} based on its contents.
	 */
	private void loadBeanDefinitionsForModelMethod(ConfigurationClassMethod method) {
		MethodMetadata metadata = method.getMetadata();

		RootBeanDefinition beanDef = new ConfigurationClassBeanDefinition();
		ConfigurationClass configClass = method.getDeclaringClass();
		beanDef.setFactoryBeanName(configClass.getBeanName());
		beanDef.setUniqueFactoryMethodName(metadata.getMethodName());
		beanDef.setAutowireMode(RootBeanDefinition.AUTOWIRE_CONSTRUCTOR);

		// consider name and any aliases
		Map<String, Object> beanAttributes = metadata.getAnnotationAttributes(Bean.class.getName());
		List<String> names = new ArrayList<String>(Arrays.asList((String[]) beanAttributes.get("name")));
		String beanName = (names.size() > 0 ? names.remove(0) : method.getMetadata().getMethodName());
		for (String alias : names) {
			registry.registerAlias(beanName, alias);
		}

		// has this already been overriden (i.e.: via XML)?
		if (registry.containsBeanDefinition(beanName)) {
			BeanDefinition existingBeanDef = registry.getBeanDefinition(beanName);
			// is the existing bean definition one that was created from a configuration class?
			if (!(existingBeanDef instanceof ConfigurationClassBeanDefinition)) {
				// no -> then it's an external override, probably XML
				// overriding is legal, return immediately
				if (logger.isDebugEnabled()) {
					logger.debug(String.format("Skipping loading bean definition for %s: a definition for bean " +
							"'%s' already exists. This is likely due to an override in XML.", method, beanName));
				}
				return;
			}
		}

		if (metadata.hasAnnotation(Primary.class.getName())) {
			beanDef.setPrimary(true);
		}

		// is this bean to be instantiated lazily?
		if (metadata.hasAnnotation(Lazy.class.getName())) {
			beanDef.setLazyInit((Boolean) metadata.getAnnotationAttributes(Lazy.class.getName()).get("value"));
		}
		else if (configClass.getMetadata().hasAnnotation(Lazy.class.getName())){
			beanDef.setLazyInit((Boolean) configClass.getMetadata().getAnnotationAttributes(Lazy.class.getName()).get("value"));
		}

		if (metadata.hasAnnotation(DependsOn.class.getName())) {
			String[] dependsOn = (String[]) metadata.getAnnotationAttributes(DependsOn.class.getName()).get("value");
			if (dependsOn.length > 0) {
				beanDef.setDependsOn(dependsOn);
			}
		}

		Autowire autowire = (Autowire) beanAttributes.get("autowire");
		if (autowire.isAutowire()) {
			beanDef.setAutowireMode(autowire.value());
		}

		String initMethodName = (String) beanAttributes.get("initMethod");
		if (StringUtils.hasText(initMethodName)) {
			beanDef.setInitMethodName(initMethodName);
		}

		String destroyMethodName = (String) beanAttributes.get("destroyMethod");
		if (StringUtils.hasText(destroyMethodName)) {
			beanDef.setDestroyMethodName(destroyMethodName);
		}

		// consider scoping
		ScopedProxyMode proxyMode = ScopedProxyMode.NO;
		if (metadata.hasAnnotation(Scope.class.getName())) {
			Map<String, Object> scopeAttributes = metadata.getAnnotationAttributes(Scope.class.getName());
			beanDef.setScope((String) scopeAttributes.get("value"));
			proxyMode = (ScopedProxyMode) scopeAttributes.get("proxyMode");
			if (proxyMode == ScopedProxyMode.DEFAULT) {
				proxyMode = ScopedProxyMode.NO;
			}
		}

		// replace the original bean definition with the target one, if necessary
		BeanDefinition beanDefToRegister = beanDef;
		if (proxyMode != ScopedProxyMode.NO) {
			BeanDefinitionHolder proxyDef = ScopedProxyCreator.createScopedProxy(
					new BeanDefinitionHolder(beanDef, beanName), registry, proxyMode == ScopedProxyMode.TARGET_CLASS);
			beanDefToRegister = proxyDef.getBeanDefinition();
		}

		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Registering bean definition for @Bean method %s.%s()", configClass.getMetadata().getClassName(), beanName));
		}

		registry.registerBeanDefinition(beanName, beanDefToRegister);
	}


	/**
	 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition
	 * created from a configuration class as opposed to any other configuration source.
	 * Used in bean overriding cases where it's necessary to determine whether the bean
	 * definition was created externally.
	 */
	private class ConfigurationClassBeanDefinition extends RootBeanDefinition {

		public ConfigurationClassBeanDefinition() {
		}

		private ConfigurationClassBeanDefinition(ConfigurationClassBeanDefinition original) {
			super(original);
		}

		@Override
		public boolean isFactoryMethod(Method candidate) {
			return (super.isFactoryMethod(candidate) && candidate.isAnnotationPresent(Bean.class));
		}

		@Override
		public ConfigurationClassBeanDefinition cloneBeanDefinition() {
			return new ConfigurationClassBeanDefinition(this);
		}
	}

}
