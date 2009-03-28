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

import static java.lang.String.*;
import static org.springframework.util.StringUtils.*;

import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.aop.framework.autoproxy.AutoProxyUtils;
import org.springframework.aop.scope.ScopedProxyFactoryBean;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;


/**
 * Reads a given fully-populated {@link ConfigurationModel}, registering bean definitions
 * with the given {@link BeanDefinitionRegistry} based on its contents.
 * <p>
 * This class was modeled after the {@link BeanDefinitionReader} hierarchy, but does not
 * implement/extend any of its artifacts as {@link ConfigurationModel} is not a
 * {@link Resource}.
 * 
 * @author Chris Beams
 * @see ConfigurationModel
 * @see AbstractConfigurationClassProcessor#processConfigBeanDefinitions()
 */
class ConfigurationModelBeanDefinitionReader {

	private static final Log log = LogFactory.getLog(ConfigurationModelBeanDefinitionReader.class);

	private BeanDefinitionRegistry registry;


	/**
	 * Reads {@code configurationModel}, registering bean definitions with {@link #registry}
	 * based on its contents.
	 * 
	 * @return new {@link BeanDefinitionRegistry} containing {@link BeanDefinition}s read
	 *         from the model.
	 */
	public BeanDefinitionRegistry loadBeanDefinitions(ConfigurationModel configurationModel) {
		registry = new SimpleBeanDefinitionRegistry();

		for (ConfigurationClass configClass : configurationModel)
			loadBeanDefinitionsForConfigurationClass(configClass);

		return registry;
	}

	/**
	 * Reads a particular {@link ConfigurationClass}, registering bean definitions for the
	 * class itself, all its {@link Bean} methods
	 */
	private void loadBeanDefinitionsForConfigurationClass(ConfigurationClass configClass) {
		doLoadBeanDefinitionForConfigurationClass(configClass);

		for (BeanMethod method : configClass.getBeanMethods())
			loadBeanDefinitionsForModelMethod(method);
	}

	/**
	 * Registers the {@link Configuration} class itself as a bean definition.
	 * @param beanDefs 
	 */
	private void doLoadBeanDefinitionForConfigurationClass(ConfigurationClass configClass) {

		GenericBeanDefinition configBeanDef = new GenericBeanDefinition();
		configBeanDef.setBeanClassName(configClass.getName());

		String configBeanName = configClass.getBeanName();

		// consider the case where it's already been defined (probably in XML)
		// and potentially has PropertyValues and ConstructorArgs)
		if (registry.containsBeanDefinition(configBeanName)) {
			if (log.isInfoEnabled())
				log.info(format("Copying property and constructor arg values from existing bean definition for "
				              + "@Configuration class %s to new bean definition", configBeanName));
			AbstractBeanDefinition existing = (AbstractBeanDefinition) registry.getBeanDefinition(configBeanName);
			configBeanDef.setPropertyValues(existing.getPropertyValues());
			configBeanDef.setConstructorArgumentValues(existing.getConstructorArgumentValues());
			configBeanDef.setResource(existing.getResource());
		}

		if (log.isInfoEnabled())
			log.info(format("Registering bean definition for @Configuration class %s", configBeanName));

		registry.registerBeanDefinition(configBeanName, configBeanDef);
	}

	/**
	 * Reads a particular {@link BeanMethod}, registering bean definitions with
	 * {@link #registry} based on its contents.
	 */
	private void loadBeanDefinitionsForModelMethod(BeanMethod method) {
		RootBeanDefinition beanDef = new ConfigurationClassBeanDefinition();

		ConfigurationClass configClass = method.getDeclaringClass();

		beanDef.setFactoryBeanName(configClass.getBeanName());
		beanDef.setFactoryMethodName(method.getName());

		Bean bean = method.getRequiredAnnotation(Bean.class);

		// consider scoping
		Scope scope = method.getAnnotation(Scope.class);
		if(scope != null)
			beanDef.setScope(scope.value());

		// consider name and any aliases
		ArrayList<String> names = new ArrayList<String>(Arrays.asList(bean.name()));
		String beanName = (names.size() > 0) ? names.remove(0) : method.getName();
		for (String alias : bean.name())
			registry.registerAlias(beanName, alias);

		// has this already been overriden (i.e.: via XML)?
		if (containsBeanDefinitionIncludingAncestry(beanName, registry)) {
			BeanDefinition existingBeanDef = getBeanDefinitionIncludingAncestry(beanName, registry);

			// is the existing bean definition one that was created by JavaConfig?
			if (!(existingBeanDef instanceof ConfigurationClassBeanDefinition)) {
				// no -> then it's an external override, probably XML

				// overriding is legal, return immediately
				log.info(format("Skipping loading bean definition for %s: a definition for bean "
					+ "'%s' already exists. This is likely due to an override in XML.", method, beanName));
				return;
			}
		}

		if (method.getAnnotation(Primary.class) != null)
			beanDef.setPrimary(true);

		// is this bean to be instantiated lazily?
		Lazy defaultLazy = configClass.getAnnotation(Lazy.class);
		if (defaultLazy != null)
			beanDef.setLazyInit(defaultLazy.value());
		Lazy lazy = method.getAnnotation(Lazy.class);
		if (lazy != null)
			beanDef.setLazyInit(lazy.value());

		// does this bean have a custom init-method specified?
		String initMethodName = bean.initMethod();
		if (hasText(initMethodName))
			beanDef.setInitMethodName(initMethodName);

		// does this bean have a custom destroy-method specified?
		String destroyMethodName = bean.destroyMethod();
		if (hasText(destroyMethodName))
			beanDef.setDestroyMethodName(destroyMethodName);

		// is this method annotated with @Scope(scopedProxy=...)?
		if (scope != null && scope.proxyMode() != ScopedProxyMode.NO) {
			RootBeanDefinition targetDef = beanDef;

			// Create a scoped proxy definition for the original bean name,
			// "hiding" the target bean in an internal target definition.
			String targetBeanName = resolveHiddenScopedProxyBeanName(beanName);
			RootBeanDefinition scopedProxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);
			scopedProxyDefinition.getPropertyValues().addPropertyValue("targetBeanName", targetBeanName);

			if (scope.proxyMode() == ScopedProxyMode.TARGET_CLASS)
				targetDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);
			// ScopedFactoryBean's "proxyTargetClass" default is TRUE, so we
			// don't need to set it explicitly here.
			else
				scopedProxyDefinition.getPropertyValues().addPropertyValue("proxyTargetClass", Boolean.FALSE);

			// The target bean should be ignored in favor of the scoped proxy.
			targetDef.setAutowireCandidate(false);

			// Register the target bean as separate bean in the factory
			registry.registerBeanDefinition(targetBeanName, targetDef);

			// replace the original bean definition with the target one
			beanDef = scopedProxyDefinition;
		}

		if (bean.dependsOn().length > 0)
			beanDef.setDependsOn(bean.dependsOn());

		log.info(format("Registering bean definition for @Bean method %s.%s()",
			configClass.getName(), beanName));

		registry.registerBeanDefinition(beanName, beanDef);

	}

	private boolean containsBeanDefinitionIncludingAncestry(String beanName, BeanDefinitionRegistry registry) {
		try {
			getBeanDefinitionIncludingAncestry(beanName, registry);
			return true;
		} catch (NoSuchBeanDefinitionException ex) {
			return false;
		}
	}

	private BeanDefinition getBeanDefinitionIncludingAncestry(String beanName, BeanDefinitionRegistry registry) {
		if(!(registry instanceof ConfigurableListableBeanFactory))
			return registry.getBeanDefinition(beanName);

		ConfigurableListableBeanFactory clbf = (ConfigurableListableBeanFactory) registry;

		do {
			if (clbf.containsBeanDefinition(beanName))
				return registry.getBeanDefinition(beanName);

			BeanFactory parent = clbf.getParentBeanFactory();
			if (parent == null) {
				clbf = null;
			} else if (parent instanceof ConfigurableListableBeanFactory) {
				clbf = (ConfigurableListableBeanFactory) parent;
				// TODO: re-enable
				// } else if (parent instanceof AbstractApplicationContext) {
				// clbf = ((AbstractApplicationContext) parent).getBeanFactory();
			} else {
				throw new IllegalStateException("unknown parent type: " + parent.getClass().getName());
			}
		} while (clbf != null);

		throw new NoSuchBeanDefinitionException(
				format("No bean definition matching name '%s' " +
				       "could be found in %s or its ancestry", beanName, registry));
	}

	/**
	 * Return the <i>hidden</i> name based on a scoped proxy bean name.
	 * 
	 * @param originalBeanName the scope proxy bean name as declared in the
	 *        Configuration-annotated class
	 * 
	 * @return the internally-used <i>hidden</i> bean name
	 */
	public static String resolveHiddenScopedProxyBeanName(String originalBeanName) {
		Assert.hasText(originalBeanName);
		return TARGET_NAME_PREFIX.concat(originalBeanName);
	}

	/** Prefix used when registering the target object for a scoped proxy. */
	private static final String TARGET_NAME_PREFIX = "scopedTarget.";
}


/**
 * {@link RootBeanDefinition} marker subclass used to signify that a bean definition created
 * by JavaConfig as opposed to any other configuration source. Used in bean overriding cases
 * where it's necessary to determine whether the bean definition was created externally
 * (e.g. via XML).
 */
@SuppressWarnings("serial")
class ConfigurationClassBeanDefinition extends RootBeanDefinition {
}
