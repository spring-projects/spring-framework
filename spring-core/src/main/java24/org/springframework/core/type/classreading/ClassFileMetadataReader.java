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
 */
final class ClassFileMetadataReader implements MetadataReader {

	private final Resource resource;

	private final AnnotationMetadata annotationMetadata;


	ClassFileMetadataReader(Resource resource, @Nullable ClassLoader classLoader) throws IOException {
		this.resource = resource;
		this.annotationMetadata = ClassFileClassMetadata.of(parseClassModel(resource), classLoader);
	}

	private static ClassModel parseClassModel(Resource resource) throws IOException {
		try (InputStream is = resource.getInputStream()) {
			byte[] bytes = is.readAllBytes();
			return ClassFile.of().parse(bytes);
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
