/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import java.lang.classfile.AccessFlags;
import java.lang.classfile.Attributes;
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


	static ClassFileMethodMetadata of(MethodModel methodModel, @Nullable ClassLoader classLoader) {
		String declaringClassName = methodModel.parent()
				.map(parent -> ClassUtils.convertResourcePathToClassName(parent.thisClass().name().stringValue()))
				.orElse(null);
		return of(methodModel, declaringClassName, classLoader);
	}

	static ClassFileMethodMetadata of(MethodModel methodModel, @Nullable String declaringClassName,
			@Nullable ClassLoader classLoader) {

		String methodName = methodModel.methodName().stringValue();
		AccessFlags flags = methodModel.flags();
		MethodTypeDesc methodType = methodModel.methodTypeSymbol();
		ClassDesc returnType = methodType.returnType();
		String returnTypeName = ClassFileAnnotationMetadata.resolveTypeName(returnType);
		Source source = new Source(declaringClassName, flags, methodName, methodType);
		RuntimeVisibleAnnotationsAttribute annotationsAttribute =
				methodModel.findAttribute(Attributes.runtimeVisibleAnnotations()).orElse(null);
		MergedAnnotations mergedAnnotations = (annotationsAttribute != null ?
				ClassFileAnnotationDelegate.createMergedAnnotations(methodName, annotationsAttribute, classLoader) :
				MergedAnnotations.of(Collections.emptyList()));
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
					.map(ClassFileAnnotationMetadata::resolveTypeName)
					.collect(Collectors.joining(",")));
			builder.append(')');
			return builder.toString();
		}
	}

}
