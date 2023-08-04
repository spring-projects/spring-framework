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

import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.MethodMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ASM class visitor that creates {@link SimpleAnnotationMetadata}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 5.2
 */
final class SimpleAnnotationMetadataReadingVisitor extends ClassVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private String className = "";

	private int access;

	@Nullable
	private String superClassName;

	@Nullable
	private String enclosingClassName;

	private boolean independentInnerClass;

	private final Set<String> interfaceNames = new LinkedHashSet<>(4);

	private final Set<String> memberClassNames = new LinkedHashSet<>(4);

	private final Set<MergedAnnotation<?>> annotations = new LinkedHashSet<>(4);

	private final Set<MethodMetadata> declaredMethods = new LinkedHashSet<>(4);

	@Nullable
	private SimpleAnnotationMetadata metadata;

	@Nullable
	private Source source;


	SimpleAnnotationMetadataReadingVisitor(@Nullable ClassLoader classLoader) {
		super(SpringAsmInfo.ASM_VERSION);
		this.classLoader = classLoader;
	}


	@Override
	public void visit(int version, int access, String name, String signature,
			@Nullable String supername, String[] interfaces) {

		this.className = toClassName(name);
		this.access = access;
		if (supername != null && !isInterface(access)) {
			this.superClassName = toClassName(supername);
		}
		for (String element : interfaces) {
			this.interfaceNames.add(toClassName(element));
		}
	}

	@Override
	public void visitOuterClass(String owner, String name, String desc) {
		this.enclosingClassName = toClassName(owner);
	}

	@Override
	public void visitInnerClass(String name, @Nullable String outerName, String innerName, int access) {
		if (outerName != null) {
			String className = toClassName(name);
			String outerClassName = toClassName(outerName);
			if (this.className.equals(className)) {
				this.enclosingClassName = outerClassName;
				this.independentInnerClass = ((access & Opcodes.ACC_STATIC) != 0);
			}
			else if (this.className.equals(outerClassName)) {
				this.memberClassNames.add(className);
			}
		}
	}

	@Override
	@Nullable
	public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
		return MergedAnnotationReadingVisitor.get(this.classLoader, getSource(),
				descriptor, visible, this.annotations::add);
	}

	@Override
	@Nullable
	public MethodVisitor visitMethod(
			int access, String name, String descriptor, String signature, String[] exceptions) {

		// Skip bridge methods and constructors - we're only interested in original user methods.
		if (isBridge(access) || name.equals("<init>")) {
			return null;
		}
		return new SimpleMethodMetadataReadingVisitor(this.classLoader, this.className,
				access, name, descriptor, this.declaredMethods::add);
	}

	@Override
	public void visitEnd() {
		MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
		this.metadata = new SimpleAnnotationMetadata(this.className, this.access,
				this.enclosingClassName, this.superClassName, this.independentInnerClass,
				this.interfaceNames, this.memberClassNames, this.declaredMethods, annotations);
	}

	public SimpleAnnotationMetadata getMetadata() {
		Assert.state(this.metadata != null, "AnnotationMetadata not initialized");
		return this.metadata;
	}

	private Source getSource() {
		Source source = this.source;
		if (source == null) {
			source = new Source(this.className);
			this.source = source;
		}
		return source;
	}

	private String toClassName(String name) {
		return ClassUtils.convertResourcePathToClassName(name);
	}

	private boolean isBridge(int access) {
		return (access & Opcodes.ACC_BRIDGE) != 0;
	}

	private boolean isInterface(int access) {
		return (access & Opcodes.ACC_INTERFACE) != 0;
	}


	/**
	 * {@link MergedAnnotation} source.
	 */
	private static final class Source {

		private final String className;

		Source(String className) {
			this.className = className;
		}

		@Override
		public boolean equals(@Nullable Object other) {
			return (this == other || (other instanceof Source that && this.className.equals(that.className)));
		}

		@Override
		public int hashCode() {
			return this.className.hashCode();
		}

		@Override
		public String toString() {
			return this.className;
		}
	}

}
