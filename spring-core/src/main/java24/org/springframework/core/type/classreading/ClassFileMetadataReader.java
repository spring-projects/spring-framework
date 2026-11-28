/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.io.IOException;
import java.io.InputStream;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;

import org.jspecify.annotations.Nullable;

import org.springframework.core.io.Resource;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;

/**
 * {@link MetadataReader} implementation based on the {@link ClassFile} API.
 *
 * @author Brian Clozel
 * @since 7.0
 */
final class ClassFileMetadataReader implements MetadataReader {

	private static final ClassFile CLASS_FILE = ClassFile.of(
			ClassFile.DebugElementsOption.DROP_DEBUG,
			ClassFile.LineNumbersOption.DROP_LINE_NUMBERS,
			ClassFile.AttributesProcessingOption.DROP_UNKNOWN_ATTRIBUTES);

	private final Resource resource;

	private final AnnotationMetadata annotationMetadata;


	ClassFileMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
		this.resource = resource;
		this.annotationMetadata = ClassFileAnnotationMetadata.of(parseClassModel(resource), classLoader);
	}

	private static ClassModel parseClassModel(Resource resource) throws IOException {
		try (InputStream inputStream = resource.getInputStream()) {
			return CLASS_FILE.parse(inputStream.readAllBytes());
		}
	}


	@Override
	public Resource getResource() {
		return this.resource;
	}

	@Override
	public ClassMetadata getClassMetadata() {
		return this.annotationMetadata;
	}

	@Override
	public AnnotationMetadata getAnnotationMetadata() {
		return this.annotationMetadata;
	}

}
