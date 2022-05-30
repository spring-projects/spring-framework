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

package org.springframework.test.context.junit4;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;

import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * Unit tests for {@link SpringJUnit4ClassRunner}.
 *
 * @author Sam Brannen
 * @author Rick Evans
 * @since 2.5
 */
public class SpringJUnit4ClassRunnerTests {

	@Test
	public void checkThatExceptionsAreNotSilentlySwallowed() throws Exception {
		SpringJUnit4ClassRunner runner = new SpringJUnit4ClassRunner(getClass()) {

			@Override
			protected TestContextManager createTestContextManager(Class<?> clazz) {
				return new TestContextManager(clazz) {

					@Override
					public void prepareTestInstance(Object testInstance) {
						throw new RuntimeException(
							"This RuntimeException should be caught and wrapped in an Exception.");
					}
				};
			}
		};
		assertThatExceptionOfType(Exception.class).isThrownBy(
				runner::createTest);
	}

	@Test
	public void getSpringTimeoutViaMetaAnnotation() throws Exception {
		SpringJUnit4ClassRunner runner = new SpringJUnit4ClassRunner(getClass());
		long timeout = runner.getSpringTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
			"springTimeoutWithMetaAnnotation")));
		assertThat(timeout).isEqualTo(10);
	}

	@Test
	public void getSpringTimeoutViaMetaAnnotationWithOverride() throws Exception {
		SpringJUnit4ClassRunner runner = new SpringJUnit4ClassRunner(getClass());
		long timeout = runner.getSpringTimeout(new FrameworkMethod(getClass().getDeclaredMethod(
			"springTimeoutWithMetaAnnotationAndOverride")));
		assertThat(timeout).isEqualTo(42);
	}

	// -------------------------------------------------------------------------

	@MetaTimed
	void springTimeoutWithMetaAnnotation() {
		/* no-op */
	}

	@MetaTimedWithOverride(millis = 42)
	void springTimeoutWithMetaAnnotationAndOverride() {
		/* no-op */
	}


	@Timed(millis = 10)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTimed {
	}

	@Timed(millis = 1000)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface MetaTimedWithOverride {

		long millis() default 1000;
	}

}
