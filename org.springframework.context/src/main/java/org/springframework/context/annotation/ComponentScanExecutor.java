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

import java.util.Set;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.config.AbstractSpecificationExecutor;
import org.springframework.context.config.ExecutorContext;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.filter.TypeFilter;

/**
 * Executes the {@link ComponentScanSpec} feature specification.
 *
 * @author Chris Beams
 * @since 3.1
 * @see ComponentScanSpec
 * @see ComponentScanBeanDefinitionParser
 * @see ComponentScan
 */
final class ComponentScanExecutor extends AbstractSpecificationExecutor<ComponentScanSpec> {

	/**
	 * Configure a {@link ClassPathBeanDefinitionScanner} based on the content of
	 * the given specification and perform actual scanning and bean definition
	 * registration.
	 */
	protected void doExecute(ComponentScanSpec spec, ExecutorContext executorContext) {
		BeanDefinitionRegistry registry = executorContext.getRegistry();
		ResourceLoader resourceLoader = executorContext.getResourceLoader();
		Environment environment = executorContext.getEnvironment();

		ClassPathBeanDefinitionScanner scanner = spec.useDefaultFilters() == null ?
			new ClassPathBeanDefinitionScanner(registry) :
			new ClassPathBeanDefinitionScanner(registry, spec.useDefaultFilters());

		scanner.setResourceLoader(resourceLoader);
		scanner.setEnvironment(environment);

		if (spec.beanDefinitionDefaults() != null) {
			scanner.setBeanDefinitionDefaults(spec.beanDefinitionDefaults());
		}
		if (spec.autowireCandidatePatterns() != null) {
			scanner.setAutowireCandidatePatterns(spec.autowireCandidatePatterns());
		}

		if (spec.resourcePattern() != null) {
			scanner.setResourcePattern(spec.resourcePattern());
		}
		if (spec.beanNameGenerator() != null) {
			scanner.setBeanNameGenerator(spec.beanNameGenerator());
		}
		if (spec.includeAnnotationConfig() != null) {
			scanner.setIncludeAnnotationConfig(spec.includeAnnotationConfig());
		}
		if (spec.scopeMetadataResolver() != null) {
			scanner.setScopeMetadataResolver(spec.scopeMetadataResolver());
		}
		if (spec.scopedProxyMode() != null) {
			scanner.setScopedProxyMode(spec.scopedProxyMode());
		}
		for (TypeFilter filter : spec.includeFilters()) {
			scanner.addIncludeFilter(filter);
		}
		for (TypeFilter filter : spec.excludeFilters()) {
			scanner.addExcludeFilter(filter);
		}

		Set<BeanDefinitionHolder> scannedBeans = scanner.doScan(spec.basePackages());

		Object source = spec.source();
		String sourceName = spec.sourceName();
		CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(sourceName, source);

		for (BeanDefinitionHolder beanDefHolder : scannedBeans) {
			compositeDef.addNestedComponent(new BeanComponentDefinition(beanDefHolder));
		}

		// Register annotation config processors, if necessary.
		if ((spec.includeAnnotationConfig() != null) && spec.includeAnnotationConfig()) {
			Set<BeanDefinitionHolder> processorDefinitions =
					AnnotationConfigUtils.registerAnnotationConfigProcessors(registry, source);
			for (BeanDefinitionHolder processorDefinition : processorDefinitions) {
				compositeDef.addNestedComponent(new BeanComponentDefinition(processorDefinition));
			}
		}

		executorContext.getRegistrar().registerComponent(compositeDef);
	}

}
