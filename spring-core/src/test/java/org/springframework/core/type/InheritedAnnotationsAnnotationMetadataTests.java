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

package org.springframework.core.type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;
import org.springframework.core.type.classreading.SimpleMetadataReaderFactory;
import org.springframework.util.MultiValueMap;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests demonstrating that the reflection-based {@link StandardAnnotationMetadata}
 * supports {@link Inherited @Inherited} annotations; whereas, the ASM-based
 * {@code SimpleAnnotationMetadata} does not.
 *
 * @author Sam Brannen
 * @since 5.2.3
 * @see AnnotationMetadataTests
 */
class InheritedAnnotationsAnnotationMetadataTests {

	private final AnnotationMetadata standardMetadata = AnnotationMetadata.introspect(AnnotatedSubclass.class);

	private final AnnotationMetadata asmMetadata;


	InheritedAnnotationsAnnotationMetadataTests() throws Exception {
		MetadataReaderFactory metadataReaderFactory = new SimpleMetadataReaderFactory();
		MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(AnnotatedSubclass.class.getName());
		this.asmMetadata = metadataReader.getAnnotationMetadata();
	}

	@Test
	void getAnnotationTypes() {
		assertThat(standardMetadata.getAnnotationTypes()).containsExactlyInAnyOrder(
			NamedAnnotation3.class.getName(),
			InheritedComposedAnnotation.class.getName());

		assertThat(asmMetadata.getAnnotationTypes()).containsExactly(
			NamedAnnotation3.class.getName());
	}

	@Test
	void hasAnnotation() {
		assertThat(standardMetadata.hasAnnotation(InheritedComposedAnnotation.class.getName())).isTrue();
		assertThat(standardMetadata.hasAnnotation(NamedAnnotation3.class.getName())).isTrue();

		// true because @NamedAnnotation3 is also directly present
		assertThat(asmMetadata.hasAnnotation(NamedAnnotation3.class.getName())).isTrue();

		assertThat(asmMetadata.hasAnnotation(InheritedComposedAnnotation.class.getName())).isFalse();
	}

	@Test
	void getMetaAnnotationTypes() {
		Set<String> metaAnnotationTypes;

		metaAnnotationTypes = standardMetadata.getMetaAnnotationTypes(InheritedComposedAnnotation.class.getName());
		assertThat(metaAnnotationTypes).containsExactlyInAnyOrder(
			MetaAnnotation.class.getName(),
			NamedAnnotation1.class.getName(),
			NamedAnnotation2.class.getName(),
			NamedAnnotation3.class.getName());

		metaAnnotationTypes = asmMetadata.getMetaAnnotationTypes(InheritedComposedAnnotation.class.getName());
		assertThat(metaAnnotationTypes).isEmpty();
	}

	@Test
	void hasMetaAnnotation() {
		assertThat(standardMetadata.hasMetaAnnotation(NamedAnnotation1.class.getName())).isTrue();
		assertThat(standardMetadata.hasMetaAnnotation(NamedAnnotation2.class.getName())).isTrue();
		assertThat(standardMetadata.hasMetaAnnotation(NamedAnnotation3.class.getName())).isTrue();
		assertThat(standardMetadata.hasMetaAnnotation(MetaAnnotation.class.getName())).isTrue();

		assertThat(asmMetadata.hasMetaAnnotation(NamedAnnotation1.class.getName())).isFalse();
		assertThat(asmMetadata.hasMetaAnnotation(NamedAnnotation2.class.getName())).isFalse();
		assertThat(asmMetadata.hasMetaAnnotation(NamedAnnotation3.class.getName())).isFalse();
		assertThat(asmMetadata.hasMetaAnnotation(MetaAnnotation.class.getName())).isFalse();
	}

	@Test
	void isAnnotated() {
		assertThat(standardMetadata.isAnnotated(InheritedComposedAnnotation.class.getName())).isTrue();
		assertThat(standardMetadata.isAnnotated(NamedAnnotation1.class.getName())).isTrue();
		assertThat(standardMetadata.isAnnotated(NamedAnnotation2.class.getName())).isTrue();
		assertThat(standardMetadata.isAnnotated(NamedAnnotation3.class.getName())).isTrue();
		assertThat(standardMetadata.isAnnotated(MetaAnnotation.class.getName())).isTrue();

		// true because @NamedAnnotation3 is also directly present
		assertThat(asmMetadata.isAnnotated(NamedAnnotation3.class.getName())).isTrue();

		assertThat(asmMetadata.isAnnotated(InheritedComposedAnnotation.class.getName())).isFalse();
		assertThat(asmMetadata.isAnnotated(NamedAnnotation1.class.getName())).isFalse();
		assertThat(asmMetadata.isAnnotated(NamedAnnotation2.class.getName())).isFalse();
		assertThat(asmMetadata.isAnnotated(MetaAnnotation.class.getName())).isFalse();
	}

	@Test
	void getAnnotationAttributes() {
		Map<String, Object> annotationAttributes;

		annotationAttributes = standardMetadata.getAnnotationAttributes(NamedAnnotation1.class.getName());
		assertThat(annotationAttributes.get("name")).isEqualTo("name 1");

		annotationAttributes = asmMetadata.getAnnotationAttributes(NamedAnnotation1.class.getName());
		assertThat(annotationAttributes).isNull();
	}

	@Test
	void getAllAnnotationAttributes() {
		MultiValueMap<String, Object> annotationAttributes;

		annotationAttributes = standardMetadata.getAllAnnotationAttributes(NamedAnnotation3.class.getName());
		assertThat(annotationAttributes).containsKey("name");
		assertThat(annotationAttributes.get("name")).containsExactlyInAnyOrder("name 3", "local");

		annotationAttributes = asmMetadata.getAllAnnotationAttributes(NamedAnnotation1.class.getName());
		assertThat(annotationAttributes).isNull();
	}


	@Retention(RetentionPolicy.RUNTIME)
	@Target(ElementType.ANNOTATION_TYPE)
	@interface MetaAnnotation {
	}

	@MetaAnnotation
	@Retention(RetentionPolicy.RUNTIME)
	@interface NamedAnnotation1 {

		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NamedAnnotation2 {

		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface NamedAnnotation3 {

		String name() default "";
	}

	@Retention(RetentionPolicy.RUNTIME)
	@NamedAnnotation1(name = "name 1")
	@NamedAnnotation2(name = "name 2")
	@NamedAnnotation3(name = "name 3")
	@Inherited
	@interface InheritedComposedAnnotation {
	}

	@InheritedComposedAnnotation
	private static class AnnotatedClass {
	}

	@NamedAnnotation3(name = "local")
	private static class AnnotatedSubclass extends AnnotatedClass {
	}

}
