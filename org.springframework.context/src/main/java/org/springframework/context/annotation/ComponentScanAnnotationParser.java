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

import java.util.Map;

import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@link FeatureAnnotationParser} implementation that reads attributes from a
 * {@link ComponentScan @ComponentScan} annotation into a {@link ComponentScanSpec}
 * which can in turn be executed by {@link ComponentScanExecutor}.
 * {@link ComponentScanBeanDefinitionParser} serves the same role for
 * the {@code <context:component-scan>} XML element.
 *
 * <p>Note that {@link ComponentScanSpec} objects may be directly
 * instantiated and returned from {@link Feature @Feature} methods as an
 * alternative to using the {@link ComponentScan @ComponentScan} annotation.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ComponentScan
 * @see ComponentScanSpec
 * @see ComponentScanExecutor
 * @see ComponentScanBeanDefinitionParser
 * @see ConfigurationClassBeanDefinitionReader
 */
final class ComponentScanAnnotationParser implements FeatureAnnotationParser {

	/**
	 * Create and return a new {@link ComponentScanSpec} from the given
	 * {@link ComponentScan} annotation metadata.
	 * @throws IllegalArgumentException if ComponentScan attributes are not present in metadata
	 */
	public ComponentScanSpec process(AnnotationMetadata metadata) {
		Map<String, Object> attribs = metadata.getAnnotationAttributes(ComponentScan.class.getName(), true);
		Assert.notNull(attribs, String.format("@ComponentScan annotation not found " +
				"while parsing metadata for class [%s].", metadata.getClassName()));

		ClassLoader classLoader = ClassUtils.getDefaultClassLoader();
		ComponentScanSpec spec = new ComponentScanSpec();

		for (String pkg : (String[])attribs.get("value")) {
			spec.addBasePackage(pkg);
		}
		for (String pkg : (String[])attribs.get("basePackages")) {
			spec.addBasePackage(pkg);
		}
		for (String className : (String[])attribs.get("basePackageClasses")) {
			spec.addBasePackage(className.substring(0, className.lastIndexOf('.')));
		}

		String resolverAttribute = "scopeResolver";
		if (!((String)attribs.get(resolverAttribute)).equals(((Class<?>)AnnotationUtils.getDefaultValue(ComponentScan.class, resolverAttribute)).getName())) {
			spec.scopeMetadataResolver((String)attribs.get(resolverAttribute), classLoader);
		}
		String scopedProxyAttribute = "scopedProxy";
		ScopedProxyMode scopedProxyMode = (ScopedProxyMode) attribs.get(scopedProxyAttribute);
		if (scopedProxyMode != ((ScopedProxyMode)AnnotationUtils.getDefaultValue(ComponentScan.class, scopedProxyAttribute))) {
			spec.scopedProxyMode(scopedProxyMode);
		}

		for (Filter filter : (Filter[]) attribs.get("includeFilters")) {
			spec.addIncludeFilter(filter.type().toString(), filter.value().getName(), classLoader);
		}
		for (Filter filter : (Filter[]) attribs.get("excludeFilters")) {
			spec.addExcludeFilter(filter.type().toString(), filter.value().getName(), classLoader);
		}

		spec.resourcePattern((String)attribs.get("resourcePattern"))
			.useDefaultFilters((Boolean)attribs.get("useDefaultFilters"))
			.beanNameGenerator((String)attribs.get("nameGenerator"), classLoader)
			.source(metadata.getClassName())
			.sourceName(metadata.getClassName());

		return spec;
	}

}
