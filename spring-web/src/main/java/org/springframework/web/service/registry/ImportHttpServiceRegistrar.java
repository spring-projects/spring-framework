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

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Built-in implementation of {@link AbstractHttpServiceRegistrar} that uses
 * {@link ImportHttpServices} annotations on the importing configuration class
 * to determine the HTTP services and groups to register.
 *
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 * @author Olga Maciaszek-Sharma
 * @since 7.0
 */
class ImportHttpServiceRegistrar extends AbstractHttpServiceRegistrar {

	@Override
	protected void registerHttpServices(GroupRegistry registry, AnnotationMetadata metadata) {

		MergedAnnotation<?> groupsAnnot = metadata.getAnnotations().get(ImportHttpServices.Container.class);
		if (groupsAnnot.isPresent()) {
			for (MergedAnnotation<?> annot : groupsAnnot.getAnnotationArray("value", ImportHttpServices.class)) {
				processImportAnnotation(annot, registry);
			}
		}

		metadata.getAnnotations().stream(ImportHttpServices.class)
				.forEach(annot -> processImportAnnotation(annot, registry));
	}

	private void processImportAnnotation(MergedAnnotation<?> annotation, GroupRegistry groupRegistry) {

		String groupName = annotation.getString("group");
		HttpServiceGroup.ClientType clientType = annotation.getEnum("clientType", HttpServiceGroup.ClientType.class);

		groupRegistry.forGroup(groupName, clientType)
				.register(annotation.getClassArray("types"))
				.detectInBasePackages(annotation.getStringArray("basePackages"))
				.detectInBasePackages(annotation.getClassArray("basePackageClasses"));
	}

}
