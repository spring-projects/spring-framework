package org.springframework.context.annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;

/**
 * Context object holding the {@link AnnotationMetadata} of the @{@link Configuration}
 * class that imported the current {@link ImportSelector} as well as the enclosing
 * {@link BeanDefinitionRegistry} to allow for conditional bean definition
 * registration when necessary.
 *
 * @author Chris Beams
 * @since 3.1
 * @see Import
 * @see ImportSelector
 */
public class ImportSelectorContext {
	private final AnnotationMetadata importingClassMetadata;
	private final BeanDefinitionRegistry registry;

	ImportSelectorContext(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		this.importingClassMetadata = importingClassMetadata;
		this.registry = registry;
	}

	public AnnotationMetadata getImportingClassMetadata() {
		return this.importingClassMetadata;
	}

	public BeanDefinitionRegistry getBeanDefinitionRegistry() {
		return registry;
	}
}