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
 * Tests for {@link MockitoBean @MockitoBean}.
 *
 * @author Stephane Nicoll
 * @author Sam Brannen
 */
class MockitoBeanConfigurationErrorTests {

	@Test
	void cannotOverrideBeanByNameWithNoSuchBeanName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("anotherBean", String.class, () -> "example");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessageStartingWith("""
						Unable to replace bean: there is no bean with name 'beanToOverride' \
						and type java.lang.String (as required by field 'FailureByNameLookup.example').""");
	}

	@Test
	void cannotOverrideBeanByNameWithBeanOfWrongType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("beanToOverride", Integer.class, () -> 42);
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByNameLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessageStartingWith("""
						Unable to replace bean: there is no bean with name 'beanToOverride' \
						and type java.lang.String (as required by field 'FailureByNameLookup.example').""");
	}

	@Test
	void cannotOverrideBeanByTypeWithNoSuchBeanType() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(FailureByTypeLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessageStartingWith("""
						Unable to override bean: there are no beans of \
						type java.lang.String (as required by field 'FailureByTypeLookup.example').""");
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
						Unable to select a bean to override: found 2 beans of type java.lang.String \
						(as required by field 'FailureByTypeLookup.example'): %s""",
						List.of("bean1", "bean2"));
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
