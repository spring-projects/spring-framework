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
 * Tests for {@link MockitoSpyBean @MockitoSpyBean}.
 *
 * @author Stephane Nicoll
 */
class MockitoSpyBeanConfigurationErrorTests {

	@Test
	void contextCustomizerCannotBeCreatedWithNoSuchBeanName() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("present", String.class, () -> "example");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByNameSingleLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to wrap bean: there is no bean with name 'beanToSpy' and \
						type java.lang.String (as required by field 'ByNameSingleLookup.example').""");
		}

	@Test
	void contextCustomizerCannotBeCreatedWithNoSuchBeanType() {
		GenericApplicationContext context = new GenericApplicationContext();
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByTypeSingleLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessageStartingWith("""
						Unable to select a bean to wrap: there are no beans of type java.lang.String \
						(as required by field 'ByTypeSingleLookup.example').""");
	}

	@Test
	void contextCustomizerCannotBeCreatedWithTooManyBeansOfThatType() {
		GenericApplicationContext context = new GenericApplicationContext();
		context.registerBean("bean1", String.class, () -> "example1");
		context.registerBean("bean2", String.class, () -> "example2");
		BeanOverrideContextCustomizerTestUtils.customizeApplicationContext(ByTypeSingleLookup.class, context);
		assertThatIllegalStateException()
				.isThrownBy(context::refresh)
				.withMessage("""
						Unable to select a bean to wrap: found 2 beans of type java.lang.String \
						(as required by field 'ByTypeSingleLookup.example'): %s""",
						List.of("bean1", "bean2"));
	}


	static class ByTypeSingleLookup {

		@MockitoSpyBean
		String example;

	}

	static class ByNameSingleLookup {

		@MockitoSpyBean("beanToSpy")
		String example;

	}

}
