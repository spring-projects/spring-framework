/*
 * Copyright 2002-2014 the original author or authors.
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
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;

import org.springframework.core.Ordered;
import org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener;
import org.springframework.test.context.support.AbstractTestExecutionListener;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.transaction.TransactionalTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

import static java.util.Arrays.*;
import static org.junit.Assert.*;
import static org.springframework.test.context.TestExecutionListeners.MergeMode.*;

/**
 * Unit tests for the {@link TestExecutionListeners @TestExecutionListeners}
 * annotation, which verify:
 * <ul>
 * <li>Proper registering of {@link TestExecutionListener listeners} in
 * conjunction with a {@link TestContextManager}</li>
 * <li><em>Inherited</em> functionality proposed in
 * <a href="https://jira.spring.io/browse/SPR-3896" target="_blank">SPR-3896</a></li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class TestExecutionListenersTests {

	private List<Class<?>> classes(TestContextManager testContextManager) {
		return testContextManager.getTestExecutionListeners().stream().map(listener -> listener.getClass()).collect(
			Collectors.toList());
	}

	private List<String> names(List<Class<?>> classes) {
		return classes.stream().map(clazz -> clazz.getSimpleName()).collect(Collectors.toList());
	}

	private void assertRegisteredListeners(Class<?> testClass, List<Class<?>> expected) {
		TestContextManager testContextManager = new TestContextManager(testClass);
		assertEquals("TELs registered for " + testClass.getSimpleName(), names(expected),
			names(classes(testContextManager)));
	}

	@Test
	public void defaultListeners() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
			DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
			TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class);
		assertRegisteredListeners(DefaultListenersTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	public void defaultListenersMergedWithCustomListenerPrepended() {
		List<Class<?>> expected = asList(QuuxTestExecutionListener.class, ServletTestExecutionListener.class,
			DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
			TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerPrependedTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	public void defaultListenersMergedWithCustomListenerAppended() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
			DependencyInjectionTestExecutionListener.class, DirtiesContextTestExecutionListener.class,
			TransactionalTestExecutionListener.class, SqlScriptsTestExecutionListener.class,
			BazTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerAppendedTestCase.class, expected);
	}

	/**
	 * @since 4.1
	 */
	@Test
	public void defaultListenersMergedWithCustomListenerInserted() {
		List<Class<?>> expected = asList(ServletTestExecutionListener.class,
			DependencyInjectionTestExecutionListener.class, BarTestExecutionListener.class,
			DirtiesContextTestExecutionListener.class, TransactionalTestExecutionListener.class,
			SqlScriptsTestExecutionListener.class);
		assertRegisteredListeners(MergedDefaultListenersWithCustomListenerInsertedTestCase.class, expected);
	}

	@Test
	public void nonInheritedDefaultListeners() {
		assertRegisteredListeners(NonInheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
	}

	@Test
	public void inheritedDefaultListeners() {
		assertRegisteredListeners(InheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
		assertRegisteredListeners(SubInheritedDefaultListenersTestCase.class, asList(QuuxTestExecutionListener.class));
		assertRegisteredListeners(SubSubInheritedDefaultListenersTestCase.class,
			asList(QuuxTestExecutionListener.class, EnigmaTestExecutionListener.class));
	}

	@Test
	public void customListeners() {
		TestContextManager testContextManager = new TestContextManager(ExplicitListenersTestCase.class);
		assertEquals("Num registered TELs for ExplicitListenersTestCase.", 3,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void nonInheritedListeners() {
		TestContextManager testContextManager = new TestContextManager(NonInheritedListenersTestCase.class);
		assertEquals("Num registered TELs for NonInheritedListenersTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void inheritedListeners() {
		TestContextManager testContextManager = new TestContextManager(InheritedListenersTestCase.class);
		assertEquals("Num registered TELs for InheritedListenersTestCase.", 4,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void customListenersRegisteredViaMetaAnnotation() {
		TestContextManager testContextManager = new TestContextManager(MetaTestCase.class);
		assertEquals("Num registered TELs for MetaTestCase.", 3, testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void nonInheritedListenersRegisteredViaMetaAnnotation() {
		TestContextManager testContextManager = new TestContextManager(MetaNonInheritedListenersTestCase.class);
		assertEquals("Num registered TELs for MetaNonInheritedListenersTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void inheritedListenersRegisteredViaMetaAnnotation() {
		TestContextManager testContextManager = new TestContextManager(MetaInheritedListenersTestCase.class);
		assertEquals("Num registered TELs for MetaInheritedListenersTestCase.", 4,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void customListenersRegisteredViaMetaAnnotationWithOverrides() {
		TestContextManager testContextManager = new TestContextManager(MetaWithOverridesTestCase.class);
		assertEquals("Num registered TELs for MetaWithOverridesTestCase.", 3,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void customsListenersRegisteredViaMetaAnnotationWithInheritedListenersWithOverrides() {
		TestContextManager testContextManager = new TestContextManager(
			MetaInheritedListenersWithOverridesTestCase.class);
		assertEquals("Num registered TELs for MetaInheritedListenersWithOverridesTestCase.", 5,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void customListenersRegisteredViaMetaAnnotationWithNonInheritedListenersWithOverrides() {
		TestContextManager testContextManager = new TestContextManager(
			MetaNonInheritedListenersWithOverridesTestCase.class);
		assertEquals("Num registered TELs for MetaNonInheritedListenersWithOverridesTestCase.", 8,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test(expected = IllegalStateException.class)
	public void listenersAndValueAttributesDeclared() {
		new TestContextManager(DuplicateListenersConfigTestCase.class);
	}


	// -------------------------------------------------------------------

	static class DefaultListenersTestCase {
	}

	@TestExecutionListeners(listeners = { QuuxTestExecutionListener.class,
		DependencyInjectionTestExecutionListener.class }, mergeMode = MERGE_WITH_DEFAULTS)
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

	@TestExecutionListeners(listeners = { QuuxTestExecutionListener.class }, inheritListeners = false)
	static class NonInheritedDefaultListenersTestCase extends InheritedDefaultListenersTestCase {
	}

	@TestExecutionListeners({ FooTestExecutionListener.class, BarTestExecutionListener.class,
		BazTestExecutionListener.class })
	static class ExplicitListenersTestCase {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	static class InheritedListenersTestCase extends ExplicitListenersTestCase {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	static class NonInheritedListenersTestCase extends InheritedListenersTestCase {
	}

	@TestExecutionListeners(listeners = FooTestExecutionListener.class, value = BarTestExecutionListener.class)
	static class DuplicateListenersConfigTestCase {
	}

	@TestExecutionListeners({//
	FooTestExecutionListener.class,//
		BarTestExecutionListener.class,//
		BazTestExecutionListener.class //
	})
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaListeners {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaInheritedListeners {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaNonInheritedListeners {
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaListenersWithOverrides {

		Class<? extends TestExecutionListener>[] listeners() default { FooTestExecutionListener.class,
			BarTestExecutionListener.class };
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaInheritedListenersWithOverrides {

		Class<? extends TestExecutionListener>[] listeners() default QuuxTestExecutionListener.class;

		boolean inheritListeners() default true;
	}

	@TestExecutionListeners
	@Retention(RetentionPolicy.RUNTIME)
	static @interface MetaNonInheritedListenersWithOverrides {

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

	@MetaListenersWithOverrides(listeners = {//
	FooTestExecutionListener.class,//
		BarTestExecutionListener.class,//
		BazTestExecutionListener.class //
	})
	static class MetaWithOverridesTestCase {
	}

	@MetaInheritedListenersWithOverrides(listeners = { FooTestExecutionListener.class, BarTestExecutionListener.class })
	static class MetaInheritedListenersWithOverridesTestCase extends MetaWithOverridesTestCase {
	}

	@MetaNonInheritedListenersWithOverrides(listeners = {//
	FooTestExecutionListener.class,//
		BarTestExecutionListener.class,//
		BazTestExecutionListener.class //
	},//
	inheritListeners = true)
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