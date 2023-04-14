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

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.asm.Opcodes;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.ClassMetadataNotFoundException;
import org.springframework.core.type.ConstructorMetadata;
import org.springframework.core.type.FieldMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.ParameterizedTypeMetadata;
import org.springframework.core.type.TypeMetadata;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * {@link AnnotationMetadata} created from a
 * {@link SimpleAnnotationMetadataReadingVisitor}.
 *
 * @author Phillip Webb
 * @author Sam Brannen
 * @author Juergen Hoeller
 * @since 5.2
 */
final class SimpleAnnotationMetadata implements AnnotationMetadata {

	private static final String[] PRIMITIVE_TYPE_NAMES = Stream.of(Boolean.TYPE, Character.TYPE, Byte.TYPE, Short.TYPE,
			Integer.TYPE, Long.TYPE, Float.TYPE, Double.TYPE, Void.TYPE).map(Class::getName).toArray(String[]::new);

	private final String className;

	private final int modifiers;

	@Nullable
	private final String enclosingClassName;

	@Nullable
	private final String superClassName;

	private final boolean independentInnerClass;

	private final Set<String> interfaceClassNames;

	private final Set<String> memberClassNames;

	private final Set<MethodMetadata> declaredMethods;

	private final Map<String, FieldMetadata> declaredFields;

	private final Set<ConstructorMetadata> declaredConstructors;

	private final MergedAnnotations annotations;

	private final MetadataReaderFactory metadataReaderFactory;

	@Nullable
	private volatile ClassMetadata superClassMetadata;

	@Nullable
	private volatile ClassMetadata enclosingClassMetadata;

	@Nullable
	private volatile Set<ClassMetadata> interfaceClassMetadata;

	@Nullable
	private volatile Set<ClassMetadata> memberClassMetadata;

	@Nullable
	private Set<String> annotationTypes;

	/**
	 * Construct an instance of {@link SimpleAnnotationMetadata}.
	 */
	SimpleAnnotationMetadata(String className, int access, @Nullable String enclosingClassName,
				@Nullable String superClassName, boolean independentInnerClass, Set<String> interfaceNames,
				Set<String> memberClassNames, Set<FieldMetadata> declaredFields,
				Set<ConstructorMetadata> declaredConstructors, Set<MethodMetadata> declaredMethods,
				MergedAnnotations annotations, MetadataReaderFactory metadataReaderFactory) {

		this.className = className;
		this.modifiers = access;

		this.enclosingClassName = enclosingClassName;
		this.superClassName = superClassName;
		this.interfaceClassNames = interfaceNames;

		this.memberClassNames = memberClassNames;
		this.independentInnerClass = independentInnerClass;

		this.declaredMethods = Collections.unmodifiableSet(declaredMethods);
		this.declaredFields = new LinkedHashMap<>();
		for (FieldMetadata declaredField : declaredFields) {
			this.declaredFields.put(declaredField.getFieldName(), declaredField);
		}
		this.declaredConstructors = Collections.unmodifiableSet(declaredConstructors);

		this.annotations = annotations;

		this.metadataReaderFactory = metadataReaderFactory;
	}

	@Override
	public String getClassName() {
		return this.className;
	}

	@Override
	public boolean isInterface() {
		return (this.modifiers & Opcodes.ACC_INTERFACE) != 0;
	}

	@Override
	public boolean isAbstract() {
		return (this.modifiers & Opcodes.ACC_ABSTRACT) != 0;
	}

	@Override
	public boolean isPrimitive() {
		for (String primitiveTypeName : PRIMITIVE_TYPE_NAMES) {
			if (this.className.equals(primitiveTypeName)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public boolean isFinal() {
		return (this.modifiers & Opcodes.ACC_FINAL) != 0;
	}

	@Override
	public int getModifiers() {
		return this.modifiers;
	}

	@Override
	public boolean isIndependent() {
		return (this.enclosingClassName == null || this.independentInnerClass);
	}

	@Override
	@Nullable
	public String getEnclosingClassName() {
		return this.enclosingClassName;
	}

	@Override
	@Nullable
	public String getSuperClassName() {
		return this.superClassName;
	}

	@Override
	public String[] getInterfaceNames() {
		return StringUtils.toStringArray(this.interfaceClassNames);
	}

	@Override
	public String[] getMemberClassNames() {
		return StringUtils.toStringArray(this.memberClassNames);
	}

	@Override
	public MergedAnnotations getAnnotations() {
		return this.annotations;
	}

	@Override
	public String getDeclaringClassName() {
		return this.getClassName();
	}

	@Override
	public TypeMetadata getAnnotatedType() {
		return new SimpleTypeMetadata(this);
	}

	@Override
	public Set<String> getAnnotationTypes() {
		Set<String> annotationTypes = this.annotationTypes;
		if (annotationTypes == null) {
			annotationTypes = Collections.unmodifiableSet(AnnotationMetadata.super.getAnnotationTypes());
			this.annotationTypes = annotationTypes;
		}
		return annotationTypes;
	}

	@Override
	public Set<MethodMetadata> getAnnotatedMethods(String annotationName) {
		return filterIsAnnotated(this.declaredMethods, annotationName);
	}

	@Override
	public Set<MethodMetadata> getDeclaredMethods() {
		return this.declaredMethods;
	}

	@Override
	public Set<FieldMetadata> getAnnotatedFields(String annotationName) {
		return filterIsAnnotated(this.declaredFields.values(), annotationName);
	}

	@Override
	public FieldMetadata getDeclaredField(String name) {
		return this.declaredFields.get(name);
	}

	@Override
	public Set<FieldMetadata> getDeclaredFields() {
		return Collections.unmodifiableSet(new LinkedHashSet<>(this.declaredFields.values()));
	}

	@Override
	public Set<ConstructorMetadata> getAnnotatedConstructors(String annotationName) {
		return filterIsAnnotated(this.declaredConstructors, annotationName);
	}

	@Override
	public Set<ConstructorMetadata> getDeclaredConstructors() {
		return this.declaredConstructors;
	}

	@Override
	public ClassMetadata getSuperClassMetadata() {
		if (this.superClassName != null && this.superClassMetadata == null) {
			this.superClassMetadata = readClassMetadata(this.superClassName);
		}
		return this.superClassMetadata;
	}

	@Override
	public Set<ClassMetadata> getInterfaceClassMetadata() {
		if (this.interfaceClassMetadata == null) {
			Set<ClassMetadata> interfaceClassMetadata = new LinkedHashSet<>();
			for (String interfaceClassName : this.interfaceClassNames) {
				interfaceClassMetadata.add(readClassMetadata(interfaceClassName));
			}
			this.interfaceClassMetadata = interfaceClassMetadata;
		}
		return Objects.requireNonNull(this.interfaceClassMetadata);
	}

	@Override
	public Set<ClassMetadata> getMemberClassMetadata() {
		if (this.memberClassMetadata == null) {
			Set<ClassMetadata> interfaceClassMetadata = new LinkedHashSet<>();
			for (String interfaceClassName : this.memberClassNames) {
				interfaceClassMetadata.add(readClassMetadata(interfaceClassName));
			}
			this.memberClassMetadata = interfaceClassMetadata;
		}
		return Objects.requireNonNull(this.memberClassMetadata);
	}

	@Override
	public ClassMetadata getEnclosingClassMetadata() {
		if (this.enclosingClassName != null && this.enclosingClassMetadata == null) {
			this.enclosingClassMetadata = readClassMetadata(this.enclosingClassName);
		}
		return this.enclosingClassMetadata;
	}

	@Override
	public boolean isType(Class<?> clazz) {
		Objects.requireNonNull(clazz);
		return isType(clazz.getName());
	}

	@Override
	public boolean isType(String className) {
		Objects.requireNonNull(className);
		return this.className.equals(className);
	}

	@Override
	public boolean isAssignableTo(Class<?> clazz) {
		Objects.requireNonNull(clazz);
		return isAssignableTo(clazz.getName());
	}

	@Override
	public boolean isAssignableTo(String className) {
		Objects.requireNonNull(className);
		if (className.equals(this.className)) {
			return true;
		}
		if (isPrimitive()) {
			return false;
		}
		if (className.equals(Object.class.getName())) {
			return true;
		}
		for (ClassMetadata interfaceType : getInterfaceClassMetadata()) {
			if (interfaceType.getClassName().equals(className)) {
				return true;
			}
		}
		ClassMetadata superClassMetadata = getSuperClassMetadata();
		return superClassMetadata != null && superClassMetadata.isAssignableTo(className);
	}

	@Override
	public boolean isAnnotation() {
		return (this.modifiers & Opcodes.ACC_ANNOTATION) != 0;
	}

	@Override
	public boolean isEnum() {
		return (this.modifiers & Opcodes.ACC_ENUM) != 0
				&& (this.superClassName == null || this.superClassName.equals(Enum.class.getName()));
	}

	@Override
	public boolean isSynthetic() {
		return (this.modifiers & Opcodes.ACC_SYNTHETIC) != 0;
	}

	private <T extends AnnotatedTypeMetadata> Set<T> filterIsAnnotated(Collection<T> annotationMetadata, String annotationName) {
		Set<T> result = new LinkedHashSet<>(annotationMetadata.size());
		for (T metadata : annotationMetadata) {
			if (metadata.isAnnotated(annotationName)) {
				result.add(metadata);
			}
		}
		return Collections.unmodifiableSet(result);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}
		if (o == null || getClass() != o.getClass()) {
			return false;
		}
		SimpleAnnotationMetadata metadata = (SimpleAnnotationMetadata) o;
		return Objects.equals(this.className, metadata.className);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.className);
	}

	@Override
	public String toString() {
		return this.className;
	}

	private ClassMetadata readClassMetadata(String className) {
		try {
			MetadataReader reader = this.metadataReaderFactory.getMetadataReader(className);
			return reader.getAnnotationMetadata();
		}
		catch (IOException ex) {
			throw new ClassMetadataNotFoundException("Could not load metadata for class " + className, ex);
		}
	}

	// BeanClassMetadata methods

	@Override
	public boolean isBeanFactory() {
		ClassMetadata classMetadata = this;
		while(classMetadata != null) {
			if (this.interfaceClassNames.contains("org.springframework.beans.factory.FactoryBean") ||
					this.interfaceClassNames.contains("org.springframework.beans.factory.ObjectFactory")) {
				return true;
			}
			classMetadata = classMetadata.getSuperClassMetadata();
		}
		return false;
	}

	@Override
	public TypeMetadata getBeanFactoryTypeMetadata() {
		TypeMetadata returnType = null;
		if (isBeanFactory()) {
			ClassMetadata classMetadata = this;
			while(classMetadata != null && returnType == null) {
				returnType = classMetadata.getDeclaredMethods().stream()
						.filter(method -> method.getMethodName().equals("getObject") && method.getParameters().isEmpty())
						.map(MethodMetadata::getReturnType)
						.filter(ParameterizedTypeMetadata.class::isInstance)
						.map(ParameterizedTypeMetadata.class::cast)
						.findFirst()
						.orElse(null);
				classMetadata = classMetadata.getSuperClassMetadata();
			}
			Objects.requireNonNull(classMetadata, "Could not find return type for bean factory method");
		}
		return returnType;
	}

}
