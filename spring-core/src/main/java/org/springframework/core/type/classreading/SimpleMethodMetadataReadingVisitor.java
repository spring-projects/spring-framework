/*
 * Copyright 2002-2019 the original author or authors.
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
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.Type;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.lang.Nullable;

/**
 * ASM method visitor that creates {@link SimpleMethodMetadata}.
 *
 * @author Phillip Webb
 * @since 5.2
 */
final class SimpleMethodMetadataReadingVisitor extends MethodVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private final String declaringClassName;

	private final int access;

	private final String name;

	private final String descriptor;

	private final List<MergedAnnotation<?>> annotations = new ArrayList<>(4);

	private final Consumer<SimpleMethodMetadata> consumer;

	@Nullable
	private Source source;


	SimpleMethodMetadataReadingVisitor(@Nullable ClassLoader classLoader, String declaringClassName,
			int access, String name, String descriptor, Consumer<SimpleMethodMetadata> consumer) {

		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
		this.declaringClassName = declaringClassName;
		this.access = access;
		this.name = name;
		this.descriptor = descriptor;
		this.consumer = consumer;
	}


	@Override
	@Nullable
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return MergedAnnotationReadingVisitor.get(this.classLoader, this::getSource,
				descriptor, visible, this.annotations::add);
	}

	@Override
	public void visitEnd() {
		if (!this.annotations.isEmpty()) {
			String returnTypeName = Type.getReturnType(this.descriptor).getClassName();
			MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
			SimpleMethodMetadata metadata = new SimpleMethodMetadata(this.name,
					this.access, this.declaringClassName, returnTypeName, annotations);
			this.consumer.accept(metadata);
		}
	}

	private Object getSource() {
		Source source = this.source;
		if (source == null) {
			source = new Source(this.declaringClassName, this.name, this.descriptor);
			this.source = source;
		}
		return source;
	}


	/**
	 * {@link MergedAnnotation} source.
	 */
	static final class Source {

		private final String declaringClassName;

		private final String name;

		private final String descriptor;

		@Nullable
		private String toStringValue;

		Source(String declaringClassName, String name, String descriptor) {
			this.declaringClassName = declaringClassName;
			this.name = name;
			this.descriptor = descriptor;
		}

		@Override
		public int hashCode() {
			int result = 1;
			result = 31 * result + this.declaringClassName.hashCode();
			result = 31 * result + this.name.hashCode();
			result = 31 * result + this.descriptor.hashCode();
			return result;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			if (this == other) {
				return true;
			}
			if (other == null || getClass() != other.getClass()) {
				return false;
			}
			Source otherSource = (Source) other;
			return (this.declaringClassName.equals(otherSource.declaringClassName) &&
					this.name.equals(otherSource.name) && this.descriptor.equals(otherSource.descriptor));
		}

		@Override
		public String toString() {
			String value = this.toStringValue;
			if (value == null) {
				StringBuilder builder = new StringBuilder();
				builder.append(this.declaringClassName);
				builder.append(".");
				builder.append(this.name);
				Type[] argumentTypes = Type.getArgumentTypes(this.descriptor);
				builder.append("(");
				for (Type type : argumentTypes) {
					builder.append(type.getClassName());
				}
				builder.append(")");
				value = builder.toString();
				this.toStringValue = value;
			}
			return value;
		}
	}

}
