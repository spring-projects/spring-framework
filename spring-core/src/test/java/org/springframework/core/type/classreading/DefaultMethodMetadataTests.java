/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;

import org.springframework.core.type.AbstractMethodMetadataTests;
import org.springframework.core.type.AnnotationMetadata;


/**
 * Tests for {@link SimpleMethodMetadata} and
 * {@link SimpleMethodMetadataReadingVisitor} on Java < 24,
 * and for the ClassFile API variant on Java >= 24.
 *
 * @author Phillip Webb
 * @author Brian Clozel
 */
class DefaultMethodMetadataTests extends AbstractMethodMetadataTests {


	@Override
	protected AnnotationMetadata get(Class<?> source) {
		try {
			return MetadataReaderFactory.create(source.getClassLoader())
					.getMetadataReader(source.getName()).getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
