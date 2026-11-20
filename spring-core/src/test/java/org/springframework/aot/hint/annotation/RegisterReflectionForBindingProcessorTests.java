/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void registerReflectionForBindingOnMethod() throws NoSuchMethodException {
		processor.registerReflectionHints(hints.reflection(), MethodLevelAnnotatedBean.class.getMethod("method"));
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithGetter.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(SampleClassWithGetter.class, "getName")).accepts(hints);
	}

	@Test
	void registerReflectionForBindingOnClassItself() {
		processor.registerReflectionHints(hints.reflection(), SampleClassWithoutAnnotationAttribute.class);
		assertThat(RuntimeHintsPredicates.reflection().onType(SampleClassWithoutAnnotationAttribute.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onType(String.class)).accepts(hints);
		assertThat(RuntimeHintsPredicates.reflection().onMethodInvocation(SampleClassWithoutAnnotationAttribute.class, "getName")).accepts(hints);
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
