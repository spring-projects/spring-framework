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
import java.lang.reflect.AccessFlag;
import java.util.Collections;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.Opcodes;
import org.springframework.asm.Type;
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

	private final int access;

	private final @Nullable String declaringClassName;

	private final String returnTypeName;

	// The source implements equals(), hashCode(), and toString() for the underlying method.
	private final Object source;

	private final MergedAnnotations mergedAnnotations;


	ClassFileMethodMetadata(String methodName, int access, @Nullable String declaringClassName,
			String returnTypeName, Object source, MergedAnnotations mergedAnnotations) {

		this.methodName = methodName;
		this.access = access;
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
		return hasAccessFlag(AccessFlag.ABSTRACT);
	}

	@Override
	public boolean isStatic() {
		return hasAccessFlag(AccessFlag.STATIC);
	}

	@Override
	public boolean isFinal() {
		return hasAccessFlag(AccessFlag.FINAL);
	}

	@Override
	public boolean isOverridable() {
		return !isStatic() && !isFinal() && !isPrivate();
	}

	private boolean isPrivate() {
		return hasAccessFlag(AccessFlag.PRIVATE);
	}

	public boolean isSynthetic() {
		return hasAccessFlag(AccessFlag.SYNTHETIC);
	}

	public boolean isDefaultConstructor() {
		return this.methodName.equals("<init>");
	}

	private boolean hasAccessFlag(AccessFlag flag) {
		return (this.access & flag.mask()) != 0;
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
		int access = flags.flagsMask();
		String declaringClassName = methodModel.parent()
				.map(parent -> ClassUtils.convertResourcePathToClassName(parent.thisClass().name().stringValue()))
				.orElse(null);
		String descriptor = methodModel.methodTypeSymbol().descriptorString();
		ClassDesc returnType = methodModel.methodTypeSymbol().returnType();
		String returnTypeName = ClassFileAnnotationMetadata.resolveTypeName(returnType);
		Source source = new Source(declaringClassName, access, methodName, descriptor);
		MergedAnnotations mergedAnnotations = methodModel.elementStream()
				.filter(RuntimeVisibleAnnotationsAttribute.class::isInstance)
				.map(RuntimeVisibleAnnotationsAttribute.class::cast)
				.findFirst()
				.map(annotations -> ClassFileAnnotationDelegate.createMergedAnnotations(methodName, annotations, classLoader))
				.orElseGet(() -> MergedAnnotations.of(Collections.emptyList()));
		return new ClassFileMethodMetadata(methodName, access, declaringClassName, returnTypeName, source, mergedAnnotations);
	}


	/**
	 * {@link org.springframework.core.annotation.MergedAnnotation} source.
	 */
	static final class Source {

		private final @Nullable String declaringClassName;

		private final int access;

		private final String methodName;

		private final String descriptor;

		private @Nullable String toStringValue;

		Source(@Nullable String declaringClassName, int access, String methodName, String descriptor) {
			this.declaringClassName = declaringClassName;
			this.methodName = methodName;
			this.access = access;
			this.descriptor = descriptor;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (other instanceof Source that &&
					Objects.equals(this.declaringClassName, that.declaringClassName) &&
					this.access == that.access &&
					Objects.equals(this.methodName, that.methodName) &&
					Objects.equals(this.descriptor, that.descriptor));
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.declaringClassName, this.access, this.methodName, this.descriptor);
		}

		@Override
		public String toString() {
			String value = this.toStringValue;
			if (value == null) {
				StringBuilder builder = new StringBuilder();
				appendAccessModifier(builder, Opcodes.ACC_PUBLIC, "public ");
				appendAccessModifier(builder, Opcodes.ACC_PROTECTED, "protected ");
				appendAccessModifier(builder, Opcodes.ACC_PRIVATE, "private ");
				appendAccessModifier(builder, Opcodes.ACC_ABSTRACT, "abstract ");
				appendAccessModifier(builder, Opcodes.ACC_STATIC, "static ");
				appendAccessModifier(builder, Opcodes.ACC_FINAL, "final ");
				Type returnType = Type.getReturnType(this.descriptor);
				builder.append(returnType.getClassName());
				builder.append(' ');
				builder.append(this.declaringClassName);
				builder.append('.');
				builder.append(this.methodName);
				builder.append('(');
				Type[] argumentTypes = Type.getArgumentTypes(this.descriptor);
				for (int i = 0; i < argumentTypes.length; i++) {
					if (i != 0) {
						builder.append(',');
					}
					builder.append(argumentTypes[i].getClassName());
				}
				builder.append(')');
				value = builder.toString();
				this.toStringValue = value;
			}
			return value;
		}

		private void appendAccessModifier(StringBuilder builder, int flag, String modifier) {
			if ((this.access & flag) != 0) {
				builder.append(modifier);
			}
		}
	}

}
