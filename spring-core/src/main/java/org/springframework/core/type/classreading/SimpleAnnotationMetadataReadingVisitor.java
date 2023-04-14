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
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.asm.AnnotationVisitor;
import org.springframework.asm.ClassVisitor;
import org.springframework.asm.FieldVisitor;
import org.springframework.asm.MethodVisitor;
import org.springframework.asm.Opcodes;
import org.springframework.asm.SpringAsmInfo;
import org.springframework.asm.signature.SignatureReader;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.ConstructorMetadata;
import org.springframework.core.type.FieldMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * ASM class visitor that creates {@link SimpleAdditionalAnnotationMetadata}.
 *
 * @author Phillip Webb
 * @author Juergen Hoeller
 * @since 5.2
 */
final class SimpleAnnotationMetadataReadingVisitor extends ClassVisitor {

	@Nullable
	private final ClassLoader classLoader;

	private final MetadataReaderFactory metadataReaderFactory;

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

	private final Set<FieldMetadata> declaredFields = new LinkedHashSet<>(4);

	private final Set<ConstructorMetadata> declaredConstructors = new LinkedHashSet<>(4);

	private final Set<MethodMetadata> declaredMethods = new LinkedHashSet<>(4);

	@Nullable
	private SimpleSource classSource;

	@Nullable
	private SimpleAnnotationMetadata metadata;

	SimpleAnnotationMetadataReadingVisitor(SimpleMetadataReaderFactory metadataReaderFactory) {
		super(SpringAsmInfo.ASM_VERSION);
		this.metadataReaderFactory = metadataReaderFactory;
		this.classLoader = metadataReaderFactory.getResourceLoader().getClassLoader();
	}

	@Override
	public void visit(int version, int access, String name, String signature, @Nullable String supername,
			String[] interfaces) {
		this.className = toClassName(name);
		this.access = access;
		if (supername != null && !isInterface(access)) {
			this.superClassName = toClassName(supername);
		}
		for (String element : interfaces) {
			this.interfaceNames.add(toClassName(element));
		}

		this.classSource = new SimpleSource(this.className);
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
		return MergedAnnotationReadingVisitor.get(this.classLoader, this.classSource, descriptor, visible, this.annotations::add);
	}

	@Override
	public FieldVisitor visitField(int access, String name, String descriptor, @Nullable String signature,
			Object value) {
		SimpleSource source = new SimpleSource(this.className, name, descriptor);
		TypeMetadata fieldType = readTypeSignature(descriptor, signature);
		return new SimpleFieldMetadataReadingVisitor(this.classLoader, source, this.className, access, name, fieldType,
				value, this.declaredFields::add);
	}

	@Override
	@Nullable
	public MethodVisitor visitMethod(int access, String name, String descriptor, @Nullable String signature,
			String[] exceptions) {
		// We're only interested in user defined methods
		if (isBridge(access)) {
			return null;
		}
		SimpleSource source = new SimpleSource(this.className, name, descriptor);
		SimpleMethodSignature methodSignature = readMethodSignature(descriptor, signature);
		return new SimpleMethodMetadataReadingVisitor(this.classLoader, source, this.className, this.access, access, name,
				methodSignature.getParameterTypes(), methodSignature.getReturnType(), exceptions,
				this.declaredMethods::add, this.declaredConstructors::add);
	}

	@Override
	public void visitEnd() {
		MergedAnnotations annotations = MergedAnnotations.of(this.annotations);
		this.metadata = new SimpleAnnotationMetadata(this.className, this.access, this.enclosingClassName,
				this.superClassName, this.independentInnerClass, this.interfaceNames, this.memberClassNames,
				this.declaredFields, this.declaredConstructors, this.declaredMethods, annotations,
				this.metadataReaderFactory);
	}

	public SimpleAnnotationMetadata getMetadata() {
		Assert.state(this.metadata != null, "AnnotationMetadata not initialized");
		return this.metadata;
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

	private TypeMetadata readTypeSignature(String descriptor, @Nullable String signature) {
		AtomicReference<TypeMetadata> ref = new AtomicReference<>();
		SignatureReader reader = new SignatureReader(signature == null ? descriptor : signature);
		reader.accept(new SimpleTypeMetadataSignatureVisitor(descriptor, this.metadataReaderFactory, ref::set));
		return Objects.requireNonNull(ref.get(), "Consumer did not return a value");
	}

	private SimpleMethodSignature readMethodSignature(String descriptor, @Nullable String signature) {
		AtomicReference<SimpleMethodSignature> ref = new AtomicReference<>();
		SignatureReader reader = new SignatureReader(signature == null ? descriptor : signature);
		reader.accept(new SimpleMethodSignatureVisitor(this.metadataReaderFactory, ref::set));
		return Objects.requireNonNull(ref.get(), "Consumer did not return a value");
	}

}
