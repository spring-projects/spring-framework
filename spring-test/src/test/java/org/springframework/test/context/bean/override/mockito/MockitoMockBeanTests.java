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

package org.springframework.test.context.bean.override.mockito;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.test.context.bean.override.BeanOverrideContextCustomizerTestUtils;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * Tests for {@link MockitoBean}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MockitoMockBeanTests {

	@Test
	void cannotOverrideBeanByNameWithNoSuchBeanName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("anotherBean", String.class, () -> "example");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: there is no bean \
						to replace with name [beanToOverride] and type [java.lang.String].""");
	}

	@Test
	void cannotOverrideBeanByNameWithBeanOfWrongType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", Integer.class, () -> 42);
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: there is no bean \
						to replace with name [beanToOverride] and type [java.lang.String].""");
	}

	@Test
	void cannotOverrideBeanByTypeWithNoSuchBeanType() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to override bean: no beans of \
						type %s (as required by annotated field '%s.example')""".formatted(
						String.class.getName(), FailureByTypeLookup.class.getSimpleName()));
	}

	@Test
	void cannotOverrideBeanByTypeWithTooManyBeansOfThatType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean1", String.class, () -> "example1");
		context.registerBean("bean2", String.class, () -> "example2");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to select a bean to override: found 2 beans \
						of type %s (as required by annotated field '%s.example'): %s""".formatted(
						String.class.getName(), FailureByTypeLookup.class.getSimpleName(), List.of("bean1", "bean2")));
	}


	static class FailureByTypeLookup {

		@MockitoBean(enforceOverride = true)
		String example;

	}

	static class FailureByNameLookup {

		@MockitoBean(name = "beanToOverride", enforceOverride = true)
		String example;

	}

}
