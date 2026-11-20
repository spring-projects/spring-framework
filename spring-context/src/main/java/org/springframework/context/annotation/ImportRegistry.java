/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.context.annotation;

import org.jspecify.annotations.Nullable;

import org.springframework.core.type.AnnotationMetadata;

/**
 * Registry of imported class {@link AnnotationMetadata}.
 *
 * @author Juergen Hoeller
 * @author Phillip Webb
 */
interface ImportRegistry {

	@Nullable AnnotationMetadata getImportingClassFor(String importedClass);

	void removeImportingClass(String importingClass);

}
