/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
