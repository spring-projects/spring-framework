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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;

/**
 * Convenient adapter for programmatic registration of annotated bean classes.
 * 
 *
 * @author Juergen Hoeller
 * @since 3.0
 */
public class AnnotatedBeanDefinitionReader {

	private final BeanDefinitionRegistry registry;

	private BeanNameGenerator beanNameGenerator = new AnnotationBeanNameGenerator();

	private ScopeMetadataResolver scopeMetadataResolver = new AnnotationScopeMetadataResolver();

	private boolean includeAnnotationConfig = true;


	public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}


	/**
	 * Return the BeanDefinitionRegistry that this scanner operates on.
	 */
	public final BeanDefinitionRegistry getRegistry() {
		return this.registry;
	}

	/**
	 * Set the BeanNameGenerator to use for detected bean classes.
	 * <p>Default is a {@link AnnotationBeanNameGenerator}.
	 */
	public void setBeanNameGenerator(BeanNameGenerator beanNameGenerator) {
		this.beanNameGenerator = (beanNameGenerator != null ? beanNameGenerator : new AnnotationBeanNameGenerator());
	}

	/**
	 * Set the ScopeMetadataResolver to use for detected bean classes.
	 * Note that this will override any custom "scopedProxyMode" setting.
	 * <p>The default is an {@link AnnotationScopeMetadataResolver}.
	 * @see #setScopedProxyMode
	 */
	public void setScopeMetadataResolver(ScopeMetadataResolver scopeMetadataResolver) {
		this.scopeMetadataResolver = scopeMetadataResolver;
	}

	/**
	 * Specify the proxy behavior for non-singleton scoped beans.
	 * Note that this will override any custom "scopeMetadataResolver" setting.
	 * <p>The default is {@link ScopedProxyMode#NO}.
	 * @see #setScopeMetadataResolver
	 */
	public void setScopedProxyMode(ScopedProxyMode scopedProxyMode) {
		this.scopeMetadataResolver = new AnnotationScopeMetadataResolver(scopedProxyMode);
	}

	/**
	 * Specify whether to register annotation config post-processors.
	 * <p>The default is to register the post-processors. Turn this off
	 * to be able to ignore the annotations or to process them differently.
	 */
	public void setIncludeAnnotationConfig(boolean includeAnnotationConfig) {
		this.includeAnnotationConfig = includeAnnotationConfig;
	}


	public void registerBeans(Class<?>... annotatedClasses) {
		for (Class<?> annotatedClass : annotatedClasses) {
			registerBean(annotatedClass);
		}
	}

	public void registerBean(Class<?> annotatedClass) {
		registerBean(annotatedClass, null, (Class<? extends Annotation>[]) null);
	}

	public void registerBean(Class<?> annotatedClass, Class<? extends Annotation>... qualifiers) {
		registerBean(annotatedClass, null, qualifiers);
	}

	public void registerBean(Class<?> annotatedClass, String name, Class<? extends Annotation>... qualifiers) {
		AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(annotatedClass);
		ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);
		abd.setScope(scopeMetadata.getScopeName());
		String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
		if (abd.getMetadata().isAnnotated(Primary.class.getName())) {
			abd.setPrimary(true);
		}
		if (abd.getMetadata().isAnnotated(Lazy.class.getName())) {
			Boolean value = (Boolean) abd.getMetadata().getAnnotationAttributes(Lazy.class.getName()).get("value");
			abd.setLazyInit(value);
		}
		if (abd.getMetadata().isAnnotated(DependsOn.class.getName())) {
			String[] value = (String[]) abd.getMetadata().getAnnotationAttributes(DependsOn.class.getName()).get("value");
			abd.setDependsOn(value);
		}
		if (qualifiers != null) {
			for (Class<? extends Annotation> qualifier : qualifiers) {
				if (Primary.class.equals(qualifier)) {
					abd.setPrimary(true);
				}
				else if (Lazy.class.equals(qualifier)) {
					abd.setLazyInit(true);
				}
				else {
					abd.addQualifier(new AutowireCandidateQualifier(qualifier));
				}
			}
		}
		BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);
		definitionHolder = applyScopedProxyMode(definitionHolder, scopeMetadata);
		BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);

		// Register annotation config processors, if necessary.
		if (this.includeAnnotationConfig) {
			AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);
		}
	}

	/**
	 * Apply the specified scope to the given bean definition.
	 * @param definition the bean definition to configure
	 * @param metadata the corresponding scope metadata
	 * @return the final bean definition to use (potentially a proxy)
	 */
	private BeanDefinitionHolder applyScopedProxyMode(BeanDefinitionHolder definition, ScopeMetadata metadata) {
		ScopedProxyMode scopedProxyMode = metadata.getScopedProxyMode();
		if (scopedProxyMode.equals(ScopedProxyMode.NO)) {
			return definition;
		}
		boolean proxyTargetClass = scopedProxyMode.equals(ScopedProxyMode.TARGET_CLASS);
		return ScopedProxyCreator.createScopedProxy(definition, this.registry, proxyTargetClass);
	}

}
