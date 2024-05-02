/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.aot.hint.annotation;

import org.junit.jupiter.api.Test;

import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.predicate.RuntimeHintsPredicates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests for {@link RegisterReflectionForBindingProcessor}.
 *
 * @author Sebastien Deleuze
 * @author Stephane Nicoll
 */
class RegisterReflectionForBindingProcessorTests {

	private final RegisterReflectionForBindingProcessor processor = new RegisterReflectionForBindingProcessor();

	private final RuntimeHints hints = new RuntimeHints();

	@Test
	void registerReflectionForBindingOnClass() {
		processor.registerReflectionHints(hints.reflection(), ClassLevelAnnotatedBean.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithGetter.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void registerReflectionForBindingOnMethod() throws NoSuchMethodException {
		processor.registerReflectionHints(hints.reflection(), MethodLevelAnnotatedBean.class.getMethod("method"));
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithGetter.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void registerReflectionForBindingOnClassItself() {
		processor.registerReflectionHints(hints.reflection(), SampleClassWithoutAnnotationAttribute.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithoutAnnotationAttribute.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethod(SampleClassWithoutAnnotationAttribute.class, "getName")).accepts(hints);
	}

	@Test
	void throwExceptionWithoutAnnotationAttributeOnMethod() {
		assertThatThrownBy(() -> processor.registerReflectionHints(hints.reflection(),
				SampleClassWithoutMethodLevelAnnotationAttribute.class.getMethod("method")))
				.isInstanceOf(IllegalStateException.class);
	}


	@RegisterReflectionForBinding(SampleClassWithGetter.class)
	static class ClassLevelAnnotatedBean {
	}

	static class MethodLevelAnnotatedBean {

		@RegisterReflectionForBinding(SampleClassWithGetter.class)
		public void method() {
		}
	}

	static class SampleClassWithGetter {

		public String getName() {
			return "test";
		}
	}

	@RegisterReflectionForBinding
	static class SampleClassWithoutAnnotationAttribute {

		public String getName() {
			return "test";
		}
	}

	static class SampleClassWithoutMethodLevelAnnotationAttribute {

		@RegisterReflectionForBinding
		public void method() {
		}
	}

}
