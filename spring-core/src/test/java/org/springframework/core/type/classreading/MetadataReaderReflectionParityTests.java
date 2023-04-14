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
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.ClassMetadata;
import org.springframework.core.type.ClassMetadataNotFoundException;
import org.springframework.core.type.FieldMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.ParameterizedTypeMetadata;
import org.springframework.core.type.TypeMetadata;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class MetadataReaderReflectionParityTests {

	private final SimpleMetadataReaderFactory factory = new SimpleMetadataReaderFactory();

	@Test
	public void readGenericField() throws IOException {
		MetadataReader metadataReader = factory.getMetadataReader(TestClass.class.getName());
		ClassMetadata classMetadata = metadataReader.getClassMetadata();

		FieldMetadata field = classMetadata.getDeclaredField("stringPredicate");
		TypeMetadata fieldType = field.getFieldType();

		assertThat(fieldType).isInstanceOf(ParameterizedTypeMetadata.class);
		assertThat(fieldType.getTypeName()).isEqualTo("java.util.function.Predicate");
		String typeArgumentClass = ((ParameterizedTypeMetadata) fieldType).getActualTypeArguments().get(0).getTypeName();
		assertThat(typeArgumentClass).isEqualTo("java.lang.String");
	}

	@Test
	public void readGenericTypedMethod() throws IOException {
		MetadataReader metadataReader = factory.getMetadataReader(TestClass.class.getName());
		ClassMetadata classMetadata = metadataReader.getClassMetadata();

		Set<MethodMetadata> declaredMethods = classMetadata.getDeclaredMethods();
		MethodMetadata genericTypedMethod = declaredMethods.stream()
				.filter(method -> method.getMethodName().equals("genericTyped")).findFirst().get();

		TypeMetadata returnType = genericTypedMethod.getReturnType();
		assertThat(returnType.getTypeName()).isEqualTo(Object.class.getName());
	}

	@Test
	public void isType() throws IOException {
		MetadataReader metadataReader = factory.getMetadataReader(StringBuilder.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();

		assertThat(metadata.isType(StringBuilder.class)).isTrue();

		// Direct interfaces
		assertThat(metadata.isType(CharSequence.class)).isFalse();
		assertThat(metadata.isType(Serializable.class)).isFalse();

		// Super type
		assertThat(metadata.isType("java.lang.AbstractStringBuilder")).isFalse();
		assertThat(metadata.isType(Appendable.class)).isFalse();
	}

	@Test
	public void fullTypeInformation() throws IOException {
		MetadataReader metadataReader = factory.getMetadataReader(StringBuilder.class.getName());
		ClassMetadata metadata = metadataReader.getClassMetadata();

		assertThat(metadata.getSuperClassMetadata()).isNotNull();
		assertThat(metadata.getSuperClassMetadata().getClassName()).isEqualTo("java.lang.AbstractStringBuilder");
		Set<ClassMetadata> interfaceTypes = metadata.getInterfaceClassMetadata();
		assertThat(interfaceTypes).isNotNull();
		assertThat(interfaceTypes.size()).isEqualTo(3);
		Iterator<ClassMetadata> iterator = interfaceTypes.iterator();
		assertThat(iterator.next().getClassName()).isEqualTo("java.io.Serializable");
		assertThat(iterator.next().getClassName()).isEqualTo("java.lang.Comparable");
		assertThat(iterator.next().getClassName()).isEqualTo("java.lang.CharSequence");
	}

	@Test
	public void isAssignableTo() throws IOException {
		MetadataReader metadataReader = factory.getMetadataReader(StringBuilder.class.getName());
		AnnotationMetadata metadata = metadataReader.getAnnotationMetadata();

		// Direct interfaces
		assertThat(metadata.isAssignableTo(CharSequence.class)).isTrue();
		assertThat(metadata.isAssignableTo(Serializable.class)).isTrue();

		// Super type
		assertThat(metadata.isAssignableTo("java.lang.AbstractStringBuilder")).isTrue();
		assertThat(metadata.isAssignableTo(Appendable.class)).isTrue();

		// Object
		assertThat(metadata.isAssignableTo(Object.class)).isTrue();
	}

	@Test
	public void isAssignableToPrimitive() {
		SimpleAnnotationMetadata metadata = forClass(Boolean.TYPE.getName());

		assertThat(metadata.isAssignableTo(Boolean.TYPE)).isTrue();
	}

	@Test
	public void isAssignableToPrimitiveNotObject() {
		SimpleAnnotationMetadata metadata = forClass(Boolean.TYPE.getName());

		assertThat(metadata.isAssignableTo(Object.class)).isFalse();
	}

	@Test
	public void isAssignableToNullClassThrows() {
		SimpleAnnotationMetadata metadata = forClass("java.lang.String");

		assertThatThrownBy(() -> metadata.isAssignableTo((Class<?>) null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void isAssignableToNullStringThrows() {
		SimpleAnnotationMetadata metadata = forClass("java.lang.String");

		assertThatThrownBy(() -> metadata.isAssignableTo((String) null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void isAssignableToNonExistentMetadataThrows() {
		SimpleAnnotationMetadata metadata = forClass(
				StringBuilder.class.getName(), "java.lang.NonExistent");

		assertThatThrownBy(() -> metadata.isAssignableTo(CharSequence.class))
				.isInstanceOf(ClassMetadataNotFoundException.class)
				.hasMessage("Could not load metadata for class java.lang.NonExistent");
	}

	@Test
	public void isTypeNullClassThrows() {
		SimpleAnnotationMetadata metadata = forClass("java.lang.String");

		assertThatThrownBy(() -> metadata.isType((Class) null))
				.isInstanceOf(NullPointerException.class);
	}

	@Test
	public void isTypeNullStringThrows() {
		SimpleAnnotationMetadata metadata = forClass("java.lang.String");

		assertThatThrownBy(() -> metadata.isAssignableTo((String) null))
				.isInstanceOf(NullPointerException.class);
	}

	private SimpleAnnotationMetadata forClass(String className) {
		return new SimpleAnnotationMetadata(className, 0, null, null,
				false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
				Collections.emptySet(), Collections.emptySet(), MergedAnnotations.of(Collections.emptyList()), factory);
	}

	private SimpleAnnotationMetadata forClass(String className, String superClassName) {
		return new SimpleAnnotationMetadata(className, 0, null, superClassName,
				false, Collections.emptySet(), Collections.emptySet(), Collections.emptySet(),
				Collections.emptySet(), Collections.emptySet(), MergedAnnotations.of(Collections.emptyList()), factory);
	}

	private static class TestClass<T> {

		Predicate<String> stringPredicate;

		T genericField;

		String[] stringArray;

		void voidReturn() {
		}

		String stringReturn() {
			return "";
		}

		T genericReturn() {
			return null;
		}

		<V extends CharSequence> V genericTyped(V charSequence) {
			return null;
		}

	}

}
