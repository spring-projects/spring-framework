/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

/**
 * Tests for {@link StandardMethodMetadata}.
 *
 * @author Phillip Webb
 */
class StandardMethodMetadataTests extends AbstractMethodMetadataTests {

	@Override
	protected AnnotationMetadata get(Class<?> source) {
		return AnnotationMetadata.introspect(source);
	}

}
