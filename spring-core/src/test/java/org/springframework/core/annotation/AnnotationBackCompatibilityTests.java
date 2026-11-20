/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.core.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests to ensure back-compatibility with Spring Framework 5.1.
 *
 * @author Phillip Webb
 * @since 5.2
 */
class AnnotationBackCompatibilityTests {

	@Test
	void multipleRoutesToMetaAnnotation() {
		Class<?> source = WithMetaMetaTestAnnotation1AndMetaTestAnnotation2.class;
		// Merged annotation chooses lowest depth
		MergedAnnotation<TestAnnotation> mergedAnnotation = MergedAnnotations.from(source).get(TestAnnotation.class);
		assertThat(mergedAnnotation.getString("value")).isEqualTo("testAndMetaTest");
		// AnnotatedElementUtils finds first
		TestAnnotation previousVersion = AnnotatedElementUtils.getMergedAnnotation(source, TestAnnotation.class);
		assertThat(previousVersion.value()).isEqualTo("metaTest");
	}

	@Test
	void defaultValue() {
		DefaultValueAnnotation synthesized = MergedAnnotations.from(WithDefaultValue.class).get(DefaultValueAnnotation.class).synthesize();
		assertThat(AnnotationUtils.isSynthesizedAnnotation(synthesized)).as("synthesized annotation").isTrue();
		Object defaultValue = AnnotationUtils.getDefaultValue(synthesized, "enumValue");
		assertThat(defaultValue).isEqualTo(TestEnum.ONE);
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface TestAnnotation {

		String value();

	}

	@Retention(RetentionPolicy.RUNTIME)
	@TestAnnotation("metaTest")
	@interface MetaTestAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@TestAnnotation("testAndMetaTest")
	@MetaTestAnnotation
	@interface TestAndMetaTestAnnotation {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@MetaTestAnnotation
	@interface MetaMetaTestAnnotation {
	}

	@MetaMetaTestAnnotation
	@TestAndMetaTestAnnotation
	static class WithMetaMetaTestAnnotation1AndMetaTestAnnotation2 {
	}

	@Retention(RetentionPolicy.RUNTIME)
	@interface DefaultValueAnnotation {

		@AliasFor("enumAlias")
		TestEnum enumValue() default TestEnum.ONE;

		@AliasFor("enumValue")
		TestEnum enumAlias() default TestEnum.ONE;

	}

	@DefaultValueAnnotation
	static class WithDefaultValue {
	}

	enum TestEnum {

		ONE,

		TWO

	}

}
