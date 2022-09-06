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
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.TypeReference;
import org.springframework.core.annotation.AliasFor;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link RuntimeHintsUtils}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class RuntimeHintsUtilsTests {

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	@SuppressWarnings("deprecation")
	void registerSynthesizedAnnotation() {
		RuntimeHintsUtils.registerSynthesizedAnnotation(this.hints, SampleInvoker.class);
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(SampleInvoker.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerAnnotationIfNecessaryWithNonSynthesizedAnnotation() throws NoSuchFieldException {
		MergedAnnotation<SampleInvoker> annotation = MergedAnnotations
				.from(TestBean.class.getField("sampleInvoker")).get(SampleInvoker.class);
		RuntimeHintsUtils.registerAnnotationIfNecessary(this.hints, annotation);
		assertThat(this.hints.proxies().jdkProxies()).isEmpty();
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerAnnotationIfNecessaryWithLocalAliases() throws NoSuchFieldException {
		MergedAnnotation<LocalMapping> annotation = MergedAnnotations
				.from(TestBean.class.getField("localMapping")).get(LocalMapping.class);
		RuntimeHintsUtils.registerAnnotationIfNecessary(this.hints, annotation);
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(LocalMapping.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerAnnotationIfNecessaryWithMetaAttributeOverride() throws NoSuchFieldException {
		MergedAnnotation<SampleInvoker> annotation = MergedAnnotations
				.from(TestBean.class.getField("retryInvoker")).get(SampleInvoker.class);
		RuntimeHintsUtils.registerAnnotationIfNecessary(this.hints, annotation);
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(SampleInvoker.class));
	}

	@Test
	@SuppressWarnings("deprecation")
	void registerAnnotationIfNecessaryWithSynthesizedAttribute() throws NoSuchFieldException {
		MergedAnnotation<RetryContainer> annotation = MergedAnnotations
				.from(TestBean.class.getField("retryContainer")).get(RetryContainer.class);
		RuntimeHintsUtils.registerAnnotationIfNecessary(this.hints, annotation);
		assertThat(this.hints.proxies().jdkProxies()).singleElement()
				.satisfies(annotationProxy(RetryContainer.class));
	}

	@SuppressWarnings("deprecation")
	private Consumer<JdkProxyHint> annotationProxy(Class<?> type) {
		return jdkProxyHint -> assertThat(jdkProxyHint.getProxiedInterfaces())
				.containsExactly(TypeReference.of(type),
						TypeReference.of(org.springframework.core.annotation.SynthesizedAnnotation.class));
	}


	static class TestBean {

		@SampleInvoker
		public String sampleInvoker;

		@LocalMapping
		public String localMapping;

		@RetryInvoker
		public String retryInvoker;

		@RetryContainer(retry = @RetryInvoker(3))
		public String retryContainer;

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
	@interface RetryContainer {

		RetryInvoker retry();

	}

}
