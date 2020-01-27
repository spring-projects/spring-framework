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
class AnnotationBackCompatibiltyTests {

	@Test
	void multiplRoutesToMetaAnnotation() {
		Class<WithMetaMetaTestAnnotation1AndMetaTestAnnotation2> source = WithMetaMetaTestAnnotation1AndMetaTestAnnotation2.class;
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
		assertThat(synthesized).isInstanceOf(SynthesizedAnnotation.class);
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

		@AliasFor("enumAlais")
		TestEnum enumValue() default TestEnum.ONE;

		@AliasFor("enumValue")
		TestEnum enumAlais() default TestEnum.ONE;

	}

	@DefaultValueAnnotation
	static class WithDefaultValue {

	}

	static enum TestEnum {

		ONE,

		TWO

	}

}
