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

import java.lang.classfile.AccessFlags;
import java.lang.classfile.MethodModel;
import java.lang.classfile.attribute.RuntimeVisibleAnnotationsAttribute;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jspecify.annotations.Nullable;

import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.util.ClassUtils;

/**
 * {@link MethodMetadata} extracted from class bytecode using the
 * {@link java.lang.classfile.ClassFile} API.
 *
 * @author Brian Clozel
 * @since 7.0
 */
final class ClassFileMethodMetadata implements MethodMetadata {

	private final String methodName;

	private final AccessFlags accessFlags;

	private final @Nullable String declaringClassName;

	private final String returnTypeName;

	// The source implements equals(), hashCode(), and toString() for the underlying method.
	private final Object source;

	private final MergedAnnotations mergedAnnotations;


	ClassFileMethodMetadata(String methodName, AccessFlags accessFlags, @Nullable String declaringClassName,
			String returnTypeName, Object source, MergedAnnotations mergedAnnotations) {

		this.methodName = methodName;
		this.accessFlags = accessFlags;
		this.declaringClassName = declaringClassName;
		this.returnTypeName = returnTypeName;
		this.source = source;
		this.mergedAnnotations = mergedAnnotations;
	}


	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public @Nullable String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public String getReturnTypeName() {
		return this.returnTypeName;
	}

	@Override
	public boolean isAbstract() {
		return this.accessFlags.has(AccessFlag.ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return this.accessFlags.has(AccessFlag.STATIC);
	}

	@Override
	public boolean isFinal() {
		return this.accessFlags.has(AccessFlag.FINAL);
	}

	@Override
	public boolean isOverridable() {
		return !isStatic() && !isFinal() && !isPrivate();
	}

	private boolean isPrivate() {
		return this.accessFlags.has(AccessFlag.PRIVATE);
	}

	public boolean isSynthetic() {
		return this.accessFlags.has(AccessFlag.SYNTHETIC);
	}

	public boolean isDefaultConstructor() {
		return this.methodName.equals("<init>");
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.mergedAnnotations;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof ClassFileMethodMetadata that && this.source.equals(that.source)));
	}

	@Override
	public int hashCode() {
		return this.source.hashCode();
	}

	@Override
	public String toString() {
		return this.source.toString();
	}


	static ClassFileMethodMetadata of(MethodModel methodModel, ClassLoader classLoader) {
		String methodName = methodModel.methodName().stringValue();
		AccessFlags flags = methodModel.flags();
		String declaringClassName = methodModel.parent()
				.map(parent -> ClassUtils.convertResourcePathToClassName(parent.thisClass().name().stringValue()))
				.orElse(null);
		ClassDesc returnType = methodModel.methodTypeSymbol().returnType();
		String returnTypeName = ClassFileAnnotationMetadata.resolveTypeName(returnType);
		Source source = new Source(declaringClassName, flags, methodName, methodModel.methodTypeSymbol());
		MergedAnnotations mergedAnnotations = methodModel.elementStream()
				.filter(RuntimeVisibleAnnotationsAttribute.class::isInstance)
				.map(RuntimeVisibleAnnotationsAttribute.class::cast)
				.findFirst()
				.map(annotations -> ClassFileAnnotationDelegate.createMergedAnnotations(methodName, annotations, classLoader))
				.orElseGet(() -> MergedAnnotations.of(Collections.emptyList()));
		return new ClassFileMethodMetadata(methodName, flags, declaringClassName, returnTypeName, source, mergedAnnotations);
	}


	/**
	 * {@link MergedAnnotation} source.
	 * @param declaringClassName the name of the declaring class
	 * @param flags the access flags
	 * @param methodName the name of the method
	 * @param descriptor the bytecode descriptor for this method
	 */
	record Source(@Nullable String declaringClassName, AccessFlags flags, String methodName, MethodTypeDesc descriptor) {

		@Override
		public boolean equals(Object other) {
			return (other instanceof Source that &&
					Objects.equals(this.flags.flagsMask(), that.flags.flagsMask()) &&
					Objects.equals(this.methodName, that.methodName) &&
					Objects.equals(this.declaringClassName, that.declaringClassName) &&
					Objects.equals(this.descriptor.descriptorString(), that.descriptor.descriptorString()));
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.declaringClassName, this.flags.flagsMask(), this.methodName, this.descriptor.descriptorString());
		}

		@Override
		public String toString() {
			StringBuilder builder = new StringBuilder();
			this.flags.flags().forEach(flag -> {
				builder.append(flag.name().toLowerCase(Locale.ROOT));
				builder.append(' ');
			});
			builder.append(ClassFileAnnotationMetadata.resolveTypeName(this.descriptor.returnType()));
			builder.append(' ');
			builder.append(this.declaringClassName);
			builder.append('.');
			builder.append(this.methodName);
			builder.append('(');
			builder.append(Stream.of(this.descriptor.parameterArray())
					.map(desc -> desc.packageName() + "." + desc.displayName())
					.collect(Collectors.joining(",")));
			builder.append(')');
			return builder.toString();
		}
	}

}
