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

package org.springframework.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.AnnotationConfigurationException;
import org.springframework.test.context.event.EventPublishingTestExecutionListener;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.MERGE_WITH_DEFAULTS;

/**
 * Unit tests for the {@link TestExecutionListeners @TestExecutionListeners}
 * annotation, which verify:
 * <ul>
 * <li>Proper registering of {@linkplain TestExecutionListener listeners} in
 * conjunction with a {@link TestContextManager}</li>
 * <li><em>Inherited</em> functionality proposed in
 * <a href="https://jira.spring.io/browse/SPR-3896" target="_blank">SPR-3896</a></li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 2.5
 */
class TestExecutionListenersTests {

	@Test
	void defaultListeners() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
				DirtiesContextBeforeModesTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
				DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
				SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class);
		assertRegisteredListeners(DefaultListenersTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	void defaultListenersMergedWithCustomListenerPrepended() {
		List<Class<?>> expected = asList(QuuxTestExecutionListener.class, ServletTestExecutionListener.class,
				DirtiesContextBeforeModesTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
				DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
				SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerPrependedTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	void defaultListenersMergedWithCustomListenerAppended() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
				DirtiesContextBeforeModesTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
				DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
				SqlScriptsTestExecutionListener.class, EventPublishingTestExecutionListener.class,
				BazTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerAppendedTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	void defaultListenersMergedWithCustomListenerInserted() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
				DirtiesContextBeforeModesTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
				BarTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
				TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class,
				EventPublishingTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerInsertedTestCase.class, expected);
	}

	@Test
	void nonInheritedDefaultListeners() {
		assertRegisteredListeners(NonInheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
	}

	@Test
	void inheritedDefaultListeners() {
		assertRegisteredListeners(InheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
		assertRegisteredListeners(SubInheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
		assertRegisteredListeners(SubSubInheritedDefaultListenersTestCase.class,
				asList(QuuxTestExecutionListener.class, EnigmaTestExecutionListener.class));
	}

	@Test
	void customListeners() {
		assertNumRegisteredListeners(ExplicitListenersTestCase.class, 3);
	}

	@Test
	void customListenersDeclaredOnInterface() {
		assertRegisteredListeners(ExplicitListenersOnTestInterfaceTestCase.class,
			asList(FooTestExecutionListener.class, BarTestExecutionListener.class));
	}

	@Test
	void nonInheritedListeners() {
		assertNumRegisteredListeners(NonInheritedListenersTestCase.class, 1);
	}

	@Test
	void inheritedListeners() {
		assertNumRegisteredListeners(InheritedListenersTestCase.class, 4);
	}

	@Test
	void customListenersRegisteredViaMetaAnnotation() {
		assertNumRegisteredListeners(MetaTestCase.class, 3);
	}

	@Test
	void nonInheritedListenersRegisteredViaMetaAnnotation() {
		assertNumRegisteredListeners(MetaNonInheritedListenersTestCase.class, 1);
	}

	@Test
	void inheritedListenersRegisteredViaMetaAnnotation() {
		assertNumRegisteredListeners(MetaInheritedListenersTestCase.class, 4);
	}

	@Test
	void customListenersRegisteredViaMetaAnnotationWithOverrides() {
		assertNumRegisteredListeners(MetaWithOverridesTestCase.class, 3);
	}

	@Test
	void customsListenersRegisteredViaMetaAnnotationWithInheritedListenersWithOverrides() {
		assertNumRegisteredListeners(MetaInheritedListenersWithOverridesTestCase.class, 5);
	}

	@Test
	void customListenersRegisteredViaMetaAnnotationWithNonInheritedListenersWithOverrides() {
		assertNumRegisteredListeners(MetaNonInheritedListenersWithOverridesTestCase.class, 8);
	}

	@Test
	void listenersAndValueAttributesDeclared() {
		assertThatExceptionOfType(AnnotationConfigurationException.class).isThrownBy(() ->
				new TestContextManager(DuplicateListenersConfigTestCase.class));
	}


	private List<Class<?>> classes(TestContextManager testContextManager) {
		return testContextManager.getTestExecutionListeners().stream().map(Object::getClass).collect(toList());
	}

	private List<String> names(List<Class<?>> classes) {
		return classes.stream().map(Class::getSimpleName).collect(toList());
	}

	private void assertRegisteredListeners(Class<?> testClass, List<Class<?>> expected) {
		TestContextManager testContextManager = new TestContextManager(testClass);
		assertThat(names(classes(testContextManager))).as("TELs registered for " + testClass.getSimpleName()).isEqualTo(names(expected));
	}

	private void assertNumRegisteredListeners(Class<?> testClass, int expected) {
		TestContextManager testContextManager = new TestContextManager(testClass);
		assertThat(testContextManager.getTestExecutionListeners().size()).as("Num registered TELs for " + testClass).isEqualTo(expected);
	}


	// -------------------------------------------------------------------

	static class DefaultListenersTestCase {
	}

	@TestExecutionListeners(
			listeners = {QuuxTestExecutionListener.class, DependencyInjectionTestExecutionListener.class},
			mergeMode = MERGE_WITH_DEFAULTS)
	static class MergedDefaultListenersWithCustomListenerPrependedTestCase {
	}

	@TestExecutionListeners(listeners = BazTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
	static class MergedDefaultListenersWithCustomListenerAppendedTestCase {
	}

	@TestExecutionListeners(listeners = BarTestExecutionListener.class, mergeMode = MERGE_WITH_DEFAULTS)
	static class MergedDefaultListenersWithCustomListenerInsertedTestCase {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	static class InheritedDefaultListenersTestCase extends DefaultListenersTestCase {
	}

	static class SubInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
	}

	@TestExecutionListeners(EnigmaTestExecutionListener.class)
	static class SubSubInheritedDefaultListenersTestCase extends SubInheritedDefaultListenersTestCase {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	static class NonInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
	}

	@TestExecutionListeners(
			{FooTestExecutionListener.class, BarTestExecutionListener.class, BazTestExecutionListener.class})
	static class ExplicitListenersTestCase {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	static class InheritedListenersTestCase extends ExplicitListenersTestCase {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	static class NonInheritedListenersTestCase extends InheritedListenersTestCase {
	}

	@TestExecutionListeners({ FooTestExecutionListener.class, BarTestExecutionListener.class })
	interface ExplicitListenersTestInterface {
	}

	static class ExplicitListenersOnTestInterfaceTestCase implements ExplicitListenersTestInterface {
	}

	@TestExecutionListeners(listeners = FooTestExecutionListener.class, value = BarTestExecutionListener.class)
	static class DuplicateListenersConfigTestCase {
	}

	@TestExecutionListeners({
			FooTestExecutionListener.class,
			BarTestExecutionListener.class,
			BazTestExecutionListener.class
	})
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaListeners {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaInheritedListeners {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaNonInheritedListeners {
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaListenersWithOverrides {

		Class<? extends TestExecutionListener>[] listeners() default
				{FooTestExecutionListener.class, BarTestExecutionListener.class};
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaInheritedListenersWithOverrides {

		Class<? extends TestExecutionListener>[] listeners() default QuuxTestExecutionListener.class;

		boolean inheritListeners() default true;
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	@interface MetaNonInheritedListenersWithOverrides {

		Class<? extends TestExecutionListener>[] listeners() default QuuxTestExecutionListener.class;

		boolean inheritListeners() default false;
	}

	@MetaListeners
	static class MetaTestCase {
	}

	@MetaInheritedListeners
	static class MetaInheritedListenersTestCase extends MetaTestCase {
	}

	@MetaNonInheritedListeners
	static class MetaNonInheritedListenersTestCase extends MetaInheritedListenersTestCase {
	}

	@MetaListenersWithOverrides(listeners = {
			FooTestExecutionListener.class,
			BarTestExecutionListener.class,
			BazTestExecutionListener.class
	})
	static class MetaWithOverridesTestCase {
	}

	@MetaInheritedListenersWithOverrides(listeners = {FooTestExecutionListener.class, BarTestExecutionListener.class})
	static class MetaInheritedListenersWithOverridesTestCase extends MetaWithOverridesTestCase {
	}

	@MetaNonInheritedListenersWithOverrides(listeners = {
			FooTestExecutionListener.class,
			BarTestExecutionListener.class,
			BazTestExecutionListener.class
	}, inheritListeners = true)
	static class MetaNonInheritedListenersWithOverridesTestCase extends MetaInheritedListenersWithOverridesTestCase {
	}

	static class FooTestExecutionListener extends AbstractTestExecutionListener {
	}

	static class BarTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public int getOrder() {
			// 2500 is between DependencyInjectionTestExecutionListener (2000) and
			// DirtiesContextTestExecutionListener (3000)
			return 2500;
		}
	}

	static class BazTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public int getOrder() {
			return Ordered.LOWEST_PRECEDENCE;
		}
	}

	static class QuuxTestExecutionListener extends AbstractTestExecutionListener {

		@Override
		public int getOrder() {
			return Ordered.HIGHEST_PRECEDENCE;
		}
	}

	static class EnigmaTestExecutionListener extends AbstractTestExecutionListener {
	}

}
