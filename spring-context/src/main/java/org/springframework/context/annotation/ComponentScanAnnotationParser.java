/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.core.type.filter.TypeFilter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the @{@link ComponentScan} annotation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ClassPathBeanDefinitionScanner#scan(String...)
 * @see ComponentScanBeanDefinitionParser
 */
class ComponentScanAnnotationParser {

	private final ResourceLoader resourceLoader;
	private final Environment environment;
	private final BeanDefinitionRegistry registry;

	public ComponentScanAnnotationParser(ResourceLoader resourceLoader, Environment environment, BeanDefinitionRegistry registry) {
		this.resourceLoader = resourceLoader;
		this.environment = environment;
		this.registry = registry;
	}

	public Set<BeanDefinitionHolder> parse(Map<String, Object> componentScanAttributes) {
		ClassPathBeanDefinitionScanner scanner =
			new ClassPathBeanDefinitionScanner(registry, (Boolean)componentScanAttributes.get("useDefaultFilters"));

		Assert.notNull(this.environment, "Environment must not be null");
		scanner.setEnvironment(this.environment);

		Assert.notNull(this.resourceLoader, "ResourceLoader must not be null");
		scanner.setResourceLoader(this.resourceLoader);

		scanner.setBeanNameGenerator(BeanUtils.instantiateClass(
				(Class<?>)componentScanAttributes.get("nameGenerator"), BeanNameGenerator.class));

		ScopedProxyMode scopedProxyMode = (ScopedProxyMode) componentScanAttributes.get("scopedProxy");
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			scanner.setScopedProxyMode(scopedProxyMode);
		} else {
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(
					(Class<?>)componentScanAttributes.get("scopeResolver"), ScopeMetadataResolver.class));
		}

		scanner.setResourcePattern((String)componentScanAttributes.get("resourcePattern"));

		for (Filter filterAnno : (Filter[])componentScanAttributes.get("includeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filterAnno)) {
				scanner.addIncludeFilter(typeFilter);
			}
		}
		for (Filter filterAnno : (Filter[])componentScanAttributes.get("excludeFilters")) {
			for (TypeFilter typeFilter : typeFiltersFor(filterAnno)) {
				scanner.addExcludeFilter(typeFilter);
			}
		}

		List<String> basePackages = new ArrayList<String>();
		for (String pkg : (String[])componentScanAttributes.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : (String[])componentScanAttributes.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class<?>[])componentScanAttributes.get("basePackageClasses")) {
			// TODO: loading user types directly here. implications on load-time
			// weaving may mean we need to revert to stringified class names in
			// annotation metadata
			basePackages.add(clazz.getPackage().getName());
		}

		if (basePackages.isEmpty()) {
			throw new IllegalStateException("At least one base package must be specified");
		}

		return scanner.doScan(basePackages.toArray(new String[]{}));
	}

	private List<TypeFilter> typeFiltersFor(Filter filterAnno) {
		List<TypeFilter> typeFilters = new ArrayList<TypeFilter>();
		for (Class<?> filterClass : (Class<?>[])filterAnno.value()) {
			switch (filterAnno.type()) {
				case ANNOTATION:
					Assert.isAssignable(Annotation.class, filterClass,
							"An error occured when processing a @ComponentScan " +
							"ANNOTATION type filter: ");
					@SuppressWarnings("unchecked")
					Class<Annotation> annoClass = (Class<Annotation>)filterClass;
					typeFilters.add(new AnnotationTypeFilter(annoClass));
					break;
				case ASSIGNABLE_TYPE:
					typeFilters.add(new AssignableTypeFilter(filterClass));
					break;
				case CUSTOM:
					Assert.isAssignable(TypeFilter.class, filterClass,
							"An error occured when processing a @ComponentScan " +
							"CUSTOM type filter: ");
					typeFilters.add(BeanUtils.instantiateClass(filterClass, TypeFilter.class));
					break;
				default:
					throw new IllegalArgumentException("unknown filter type " + filterAnno.type());
			}
		}
		return typeFilters;
	}
}
