/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import org.springframework.test.context.support.DefaultTestContextBootstrapper;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.context.web.WebTestContextBootstrapper;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.context.BootstrapUtils.*;

/**
 * Unit tests for {@link BootstrapUtils}.
 *
 * @author Sam Brannen
 * @since 4.2
 */
public class BootstrapUtilsTests {

	private final CacheAwareContextLoaderDelegate delegate = mock(CacheAwareContextLoaderDelegate.class);

	@Rule
	public ExpectedException exception = ExpectedException.none();

	@Test
	public void resolveTestContextBootstrapperForNonAnnotatedClass() {
		assertBootstrapper(NonAnnotatedClass.class, DefaultTestContextBootstrapper.class);
	}

	@Test
	public void resolveTestContextBootstrapperForWithWebAppConfigurationAnnotatedClass() throws Exception {
		assertBootstrapper(WithWebAppConfiguration.class, WebTestContextBootstrapper.class);
	}

	@Test
	public void resolveTestContextBootstrapperWithEmptyBootstrapWithAnnotation() {
		BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(EmptyBootstrapWithAnnotationClass.class, delegate);

		exception.expect(IllegalStateException.class);
		exception.expectMessage(containsString("Specify @BootstrapWith's 'value' attribute"));

		resolveTestContextBootstrapper(bootstrapContext);
	}

	@Test
	public void resolveTestContextBootstrapperWithDirectBootstrapWithAnnotation() {
		assertBootstrapper(DirectBootstrapWithAnnotationClass.class, FooBootstrapper.class);
	}

	@Test
	public void resolveTestContextBootstrapperWithInheritedBootstrapWithAnnotation() {
		assertBootstrapper(InheritedBootstrapWithAnnotationClass.class, FooBootstrapper.class);
	}

	@Test
	public void resolveTestContextBootstrapperWithMetaBootstrapWithAnnotation() {
		assertBootstrapper(MetaAnnotatedBootstrapWithAnnotationClass.class, BarBootstrapper.class);
	}

	@Test
	public void resolveTestContextBootstrapperWithDoubleMetaBootstrapWithAnnotation() {
		BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(
			DoubleMetaAnnotatedBootstrapWithAnnotationClass.class, delegate);

		exception.expect(IllegalStateException.class);
		exception.expectMessage(containsString("found multiple declarations of @BootstrapWith"));
		exception.expectMessage(containsString(FooBootstrapper.class.getName()));
		exception.expectMessage(containsString(BarBootstrapper.class.getName()));

		resolveTestContextBootstrapper(bootstrapContext);
	}

	private void assertBootstrapper(Class<?> testClass, Class<?> expectedBootstrapper) {
		BootstrapContext bootstrapContext = BootstrapTestUtils.buildBootstrapContext(testClass, delegate);
		TestContextBootstrapper bootstrapper = resolveTestContextBootstrapper(bootstrapContext);
		assertNotNull(bootstrapper);
		assertEquals(expectedBootstrapper, bootstrapper.getClass());
	}

	// -------------------------------------------------------------------

	static class FooBootstrapper extends DefaultTestContextBootstrapper {}

	static class BarBootstrapper extends DefaultTestContextBootstrapper {}

	@BootstrapWith(FooBootstrapper.class)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface BootWithFoo {}

	@BootstrapWith(BarBootstrapper.class)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface BootWithBar {}

	static class NonAnnotatedClass {}

	@BootstrapWith
	static class EmptyBootstrapWithAnnotationClass {}

	@BootstrapWith(FooBootstrapper.class)
	static class DirectBootstrapWithAnnotationClass {}

	static class InheritedBootstrapWithAnnotationClass extends DirectBootstrapWithAnnotationClass {}

	@BootWithBar
	static class MetaAnnotatedBootstrapWithAnnotationClass {}

	@BootWithBar
	@BootWithFoo
	static class DoubleMetaAnnotatedBootstrapWithAnnotationClass {}

	@WebAppConfiguration
	static class WithWebAppConfiguration {}

}
