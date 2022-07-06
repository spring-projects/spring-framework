/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.aot.hint.support;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.JdkProxyHint;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.ReflectionHints;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeHint;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.SynthesizedAnnotation;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHintsUtils}.
 *
 * @author Stephane Nicoll
 */
class RuntimeHintsUtilsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerAnnotationType() {
		RuntimeHintsUtils.registerAnnotation(this.hints, SampleInvoker.class);
		assertThat(this.hints.reflection().typeHints()).singleElement()
				.satisfies(annotationHint(SampleInvoker.class));
		assertThat(this.hints.proxies().jdkProxies()).isEmpty();
	}

	@Test
	void registerAnnotationTypeWithLocalUseOfAliasForRegistersProxy() {
		RuntimeHintsUtils.registerAnnotation(this.hints, LocalMapping.class);
		assertThat(this.hints.reflection().typeHints()).singleElement()
				.satisfies(annotationHint(LocalMapping.class));
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(LocalMapping.class));
	}

	@Test
	void registerAnnotationTypeProxyRegistersJdkProxy() {
		RuntimeHintsUtils.registerAnnotation(this.hints, RetryInvoker.class);
		assertThat(this.hints.reflection().typeHints())
				.anySatisfy(annotationHint(RetryInvoker.class))
				.anySatisfy(annotationHint(SampleInvoker.class));
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(RetryInvoker.class));
	}

	@Test
	void registerAnnotationTypeWhereUsedAsAMetaAnnotationRegistersHierarchy() {
		RuntimeHintsUtils.registerAnnotation(this.hints, RetryWithEnabledFlagInvoker.class);
		ReflectionHints reflection = this.hints.reflection();
		assertThat(reflection.typeHints())
				.anySatisfy(annotationHint(RetryWithEnabledFlagInvoker.class))
				.anySatisfy(annotationHint(RetryInvoker.class))
				.anySatisfy(annotationHint(SampleInvoker.class))
				.hasSize(3);
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(RetryWithEnabledFlagInvoker.class));
	}

	private Consumer<TypeHint> annotationHint(Class<?> type) {
		return typeHint -> {
			assertThat(typeHint.getType()).isEqualTo(TypeReference.of(type));
			assertThat(typeHint.constructors()).isEmpty();
			assertThat(typeHint.fields()).isEmpty();
			assertThat(typeHint.methods()).isEmpty();
			assertThat(typeHint.getMemberCategories()).containsOnly(MemberCategory.INVOKE_DECLARED_METHODS);
		};
	}

	private Consumer<JdkProxyHint> annotationProxy(Class<?> type) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(TypeReference.of(type),
						TypeReference.of(SynthesizedAnnotation.class));
	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface LocalMapping {

		@AliasFor("retries")
		int value() default 0;

		@AliasFor("value")
		int retries() default 0;

	}


	@Retention(RetentionPolicy.RUNTIME)
	@interface SampleInvoker {

		int retries() default 0;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@SampleInvoker
	@interface RetryInvoker {

		@AliasFor(attribute = "retries", annotation = SampleInvoker.class)
		int value() default 1;

	}

	@Retention(RetentionPolicy.RUNTIME)
	@RetryInvoker
	@interface RetryWithEnabledFlagInvoker {

		@AliasFor(attribute = "value", annotation = RetryInvoker.class)
		int value() default 5;

		boolean enabled() default true;

	}

}
