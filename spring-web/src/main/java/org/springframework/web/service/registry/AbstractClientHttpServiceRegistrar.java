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


import java.util.List;

import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Base class for an HTTP Service registrar that detects
 * {@link HttpServiceClient @HttpServiceClient} annotated interfaces and
 * registers them.
 *
 * <p>Subclasses need to implement
 * {@link #registerHttpServices(GroupRegistry, AnnotationMetadata)} and invoke
 * {@link #findAndRegisterHttpServiceClients(GroupRegistry, List)} with the
 * list of base packages to scan.
 *
 * @author Rossen Stoyanchev
 * @since 7.0
 */
public abstract class AbstractClientHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	/**
	 * Find all HTTP Services under the given base packages that also have an
	 * {@link HttpServiceClient @HttpServiceClient} annotation, and register them
	 * in the group specified on the annotation.
	 * @param registry the registry from {@link #registerHttpServices(GroupRegistry, AnnotationMetadata)}
	 * @param basePackages the base packages to scan
	 */
	protected void findAndRegisterHttpServiceClients(GroupRegistry registry, List<String> basePackages) {
		basePackages.stream()
				.flatMap(this::findHttpServices)
				.filter(definition -> definition instanceof AnnotatedBeanDefinition)
				.map(definition -> (AnnotatedBeanDefinition) definition)
				.filter(definition -> definition.getMetadata().hasAnnotation(HttpServiceClient.class.getName()))
				.filter(definition -> definition.getBeanClassName() != null)
				.forEach(definition -> {
					MergedAnnotations annotations = definition.getMetadata().getAnnotations();
					String group = annotations.get(HttpServiceClient.class).getString("group");
					registry.forGroup(group).registerTypeNames(definition.getBeanClassName());
				});
	}

}
