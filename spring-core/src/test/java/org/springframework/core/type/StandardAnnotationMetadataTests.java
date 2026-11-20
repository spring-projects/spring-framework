/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type;

/**
 * Tests for {@link StandardAnnotationMetadata}.
 *
 * @author Phillip Webb
 */
class StandardAnnotationMetadataTests extends AbstractAnnotationMetadataTests {

	@Override
	@SuppressWarnings("deprecation")
	protected AnnotationMetadata get(Class<?> source) {
		return new StandardAnnotationMetadata(source);
	}

}
