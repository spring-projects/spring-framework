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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanNameGenerator;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
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

	public void parse(AnnotationMetadata annotationMetadata) {
		Map<String, Object> attribs = annotationMetadata.getAnnotationAttributes(ComponentScan.class.getName());
		if (attribs == null) {
			// @ComponentScan annotation is not present -> do nothing
			return;
		}

		ClassPathBeanDefinitionScanner scanner =
			new ClassPathBeanDefinitionScanner(registry, (Boolean)attribs.get("useDefaultFilters"));

		Assert.notNull(this.environment, "Environment must not be null");
		scanner.setEnvironment(this.environment);

		Assert.notNull(this.resourceLoader, "ResourceLoader must not be null");
		scanner.setResourceLoader(this.resourceLoader);

		scanner.setBeanNameGenerator(BeanUtils.instantiateClass(
				(Class<?>)attribs.get("nameGenerator"), BeanNameGenerator.class));

		ScopedProxyMode scopedProxyMode = (ScopedProxyMode) attribs.get("scopedProxy");
		if (scopedProxyMode != ScopedProxyMode.DEFAULT) {
			scanner.setScopedProxyMode(scopedProxyMode);
		} else {
			scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(
					(Class<?>)attribs.get("scopeResolver"), ScopeMetadataResolver.class));
		}

		scanner.setResourcePattern((String)attribs.get("resourcePattern"));

		for (Filter filter : (Filter[])attribs.get("includeFilters")) {
			scanner.addIncludeFilter(createTypeFilter(filter));
		}
		for (Filter filter : (Filter[])attribs.get("excludeFilters")) {
			scanner.addExcludeFilter(createTypeFilter(filter));
		}

		List<String> basePackages = new ArrayList<String>();
		for (String pkg : (String[])attribs.get("value")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (String pkg : (String[])attribs.get("basePackages")) {
			if (StringUtils.hasText(pkg)) {
				basePackages.add(pkg);
			}
		}
		for (Class<?> clazz : (Class<?>[])attribs.get("basePackageClasses")) {
			// TODO: loading user types directly here. implications on load-time
			// weaving may mean we need to revert to stringified class names in
			// annotation metadata
			basePackages.add(clazz.getPackage().getName());
		}

		if (basePackages.isEmpty()) {
			throw new IllegalStateException("At least one base package must be specified");
		}

		scanner.scan(basePackages.toArray(new String[]{}));
	}

	private TypeFilter createTypeFilter(Filter filter) {
		switch (filter.type()) {
			case ANNOTATION:
				@SuppressWarnings("unchecked")
				Class<Annotation> filterClass = (Class<Annotation>)filter.value();
				return new AnnotationTypeFilter(filterClass);
			case ASSIGNABLE_TYPE:
				return new AssignableTypeFilter(filter.value());
			case CUSTOM:
				return BeanUtils.instantiateClass(filter.value(), TypeFilter.class);
			default:
				throw new IllegalArgumentException("unknown filter type " + filter.type());
		}
	}
}
