/*
 * Copyright 2002-2023 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;

/**
 * {@link FieldVisitor} producing instances of {@link SimpleFieldMetadata}.
 *
 * @author Danny Thomas
 */
final class SimpleFieldMetadataReadingVisitor extends FieldVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private final Object source;

	private final List<MergedAnnotation<?>> annotations = new ArrayList<>(4);

	private final String declaringClassName;

	private final int access;

	private final String name;

	private final TypeMetadata type;

	private final Object value;

	private final Consumer<SimpleFieldMetadata> consumer;

	SimpleFieldMetadataReadingVisitor(@Nullable ClassLoader classLoader, Object source, String declaringClassName,
			int access, String name, TypeMetadata type, Object value, Consumer<SimpleFieldMetadata> consumer) {

		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.source = source;
		this.declaringClassName = declaringClassName;
		this.access = access;
		this.name = name;
		this.type = type;
		this.value = value;
		this.consumer = consumer;
	}

	@Override
	@Nullable
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return MergedAnnotationReadingVisitor.get(this.classLoader, this.source, descriptor, visible, this.annotations::add);
	}

	@Override
	public void visitEnd() {
		MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
		SimpleFieldMetadata metadata = new SimpleFieldMetadata(this.declaringClassName, this.name, this.access,
				this.type, this.source, annotations);
		this.consumer.accept(metadata);
	}

}
