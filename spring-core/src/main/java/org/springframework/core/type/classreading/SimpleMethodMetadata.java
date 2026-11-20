/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.type.classreading;

import org.jspecify.annotations.Nullable;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;

/**
 * {@link MethodMetadata} created from a {@link SimpleMethodMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @since 5.2
 */
final class SimpleMethodMetadata implements MethodMetadata {

	private final String methodName;

	private final int access;

	private final String declaringClassName;

	private final String returnTypeName;

	// The source implements equals(), hashCode(), and toString() for the underlying method.
	private final Object source;

	private final MergedAnnotations annotations;


	SimpleMethodMetadata(String methodName, int access, String declaringClassName,
			String returnTypeName, Object source, MergedAnnotations annotations) {

		this.methodName = methodName;
		this.access = access;
		this.declaringClassName = declaringClassName;
		this.returnTypeName = returnTypeName;
		this.source = source;
		this.annotations = annotations;
	}


	@Override
	public String getMethodName() {
		return this.methodName;
	}

	@Override
	public String getDeclaringClassName() {
		return this.declaringClassName;
	}

	@Override
	public String getReturnTypeName() {
		return this.returnTypeName;
	}

	@Override
	public boolean isAbstract() {
		return (this.access & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public boolean isStatic() {
		return (this.access & Opcodes.ACC_STATIC) != 0;
	}

	@Override
	public boolean isFinal() {
		return (this.access & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public boolean isOverridable() {
		return !isStatic() && !isFinal() && !isPrivate();
	}

	private boolean isPrivate() {
		return (this.access & Opcodes.ACC_PRIVATE) != 0;
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}


	@Override
	public boolean equals(@Nullable Object other) {
		return (this == other || (other instanceof SimpleMethodMetadata that && this.source.equals(that.source)));
	}

	@Override
	public int hashCode() {
		return this.source.hashCode();
	}

	@Override
	public String toString() {
		return this.source.toString();
	}

}
