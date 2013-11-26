/*
 * Copyright 2002-2013 the original author or authors.
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

import org.junit.Test;
import org.springframework.test.context.support.AbstractTestExecutionListener;

import static org.junit.Assert.*;

/**
 * <p>
 * JUnit 4 based unit test for the {@link TestExecutionListeners
 * &#064;TestExecutionListeners} annotation, which verifies:
 * </p>
 * <ul>
 * <li>Proper registering of {@link TestExecutionListener listeners} in
 * conjunction with a {@link TestContextManager}</li>
 * <li><em>Inherited</em> functionality proposed in <a
 * href="http://opensource.atlassian.com/projects/spring/browse/SPR-3896"
 * target="_blank">SPR-3896</a></li>
 * </ul>
 *
 * @author Sam Brannen
 * @since 2.5
 */
public class TestExecutionListenersTests {

	@Test
	public void verifyNumDefaultListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(DefaultListenersExampleTestCase.class);
		assertEquals("Num registered TELs for DefaultListenersExampleTestCase.", 4,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumNonInheritedDefaultListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(
			NonInheritedDefaultListenersExampleTestCase.class);
		assertEquals("Num registered TELs for NonInheritedDefaultListenersExampleTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumInheritedDefaultListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(InheritedDefaultListenersExampleTestCase.class);
		assertEquals("Num registered TELs for InheritedDefaultListenersExampleTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());

		testContextManager = new TestContextManager(SubInheritedDefaultListenersExampleTestCase.class);
		assertEquals("Num registered TELs for SubInheritedDefaultListenersExampleTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());

		testContextManager = new TestContextManager(SubSubInheritedDefaultListenersExampleTestCase.class);
		assertEquals("Num registered TELs for SubSubInheritedDefaultListenersExampleTestCase.", 2,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(ExampleTestCase.class);
		assertEquals("Num registered TELs for ExampleTestCase.", 3,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumNonInheritedListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(NonInheritedListenersExampleTestCase.class);
		assertEquals("Num registered TELs for NonInheritedListenersExampleTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumInheritedListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(InheritedListenersExampleTestCase.class);
		assertEquals("Num registered TELs for InheritedListenersExampleTestCase.", 4,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegisteredViaMetaAnnotation() throws Exception {
		TestContextManager testContextManager = new TestContextManager(MetaExampleTestCase.class);
		assertEquals("Num registered TELs for MetaExampleTestCase.", 3,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumNonInheritedListenersRegisteredViaMetaAnnotation() throws Exception {
		TestContextManager testContextManager = new TestContextManager(MetaNonInheritedListenersExampleTestCase.class);
		assertEquals("Num registered TELs for MetaNonInheritedListenersExampleTestCase.", 1,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumInheritedListenersRegisteredViaMetaAnnotation() throws Exception {
		TestContextManager testContextManager = new TestContextManager(MetaInheritedListenersExampleTestCase.class);
		assertEquals("Num registered TELs for MetaInheritedListenersExampleTestCase.", 4,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegisteredViaMetaAnnotationWithOverrides() throws Exception {
		TestContextManager testContextManager = new TestContextManager(MetaWithOverridesExampleTestCase.class);
		assertEquals("Num registered TELs for MetaWithOverridesExampleTestCase.", 3,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegisteredViaMetaAnnotationWithInheritedListenersWithOverrides() throws Exception {
		TestContextManager testContextManager = new TestContextManager(
			MetaInheritedListenersWithOverridesExampleTestCase.class);
		assertEquals("Num registered TELs for MetaInheritedListenersWithOverridesExampleTestCase.", 5,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegisteredViaMetaAnnotationWithNonInheritedListenersWithOverrides() throws Exception {
		TestContextManager testContextManager = new TestContextManager(
			MetaNonInheritedListenersWithOverridesExampleTestCase.class);
		assertEquals("Num registered TELs for MetaNonInheritedListenersWithOverridesExampleTestCase.", 8,
			testContextManager.getTestExecutionListeners().size());
	}

	@Test(expected = IllegalStateException.class)
	public void verifyDuplicateListenersConfigThrowsException() throws Exception {
		new TestContextManager(DuplicateListenersConfigExampleTestCase.class);
	}


	static class DefaultListenersExampleTestCase {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	static class InheritedDefaultListenersExampleTestCase extends DefaultListenersExampleTestCase {
	}

	static class SubInheritedDefaultListenersExampleTestCase extends InheritedDefaultListenersExampleTestCase {
	}

	@TestExecutionListeners(EnigmaTestExecutionListener.class)
	static class SubSubInheritedDefaultListenersExampleTestCase extends SubInheritedDefaultListenersExampleTestCase {
	}

	@TestExecutionListeners(listeners = { QuuxTestExecutionListener.class }, inheritListeners = false)
	static class NonInheritedDefaultListenersExampleTestCase extends InheritedDefaultListenersExampleTestCase {
	}

	@TestExecutionListeners({ FooTestExecutionListener.class, BarTestExecutionListener.class,
		BazTestExecutionListener.class })
	static class ExampleTestCase {
	}

	@TestExecutionListeners(QuuxTestExecutionListener.class)
	static class InheritedListenersExampleTestCase extends ExampleTestCase {
	}

	@TestExecutionListeners(listeners = QuuxTestExecutionListener.class, inheritListeners = false)
	static class NonInheritedListenersExampleTestCase extends InheritedListenersExampleTestCase {
	}

	@TestExecutionListeners(listeners = FooTestExecutionListener.class, value = BarTestExecutionListener.class)
	static class DuplicateListenersConfigExampleTestCase {
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
	static class MetaExampleTestCase {
	}

	@MetaInheritedListeners
	static class MetaInheritedListenersExampleTestCase extends MetaExampleTestCase {
	}

	@MetaNonInheritedListeners
	static class MetaNonInheritedListenersExampleTestCase extends MetaInheritedListenersExampleTestCase {
	}

	@MetaListenersWithOverrides(listeners = {//
	FooTestExecutionListener.class,//
		BarTestExecutionListener.class,//
		BazTestExecutionListener.class //
	})
	static class MetaWithOverridesExampleTestCase {
	}

	@MetaInheritedListenersWithOverrides(listeners = { FooTestExecutionListener.class, BarTestExecutionListener.class })
	static class MetaInheritedListenersWithOverridesExampleTestCase extends MetaWithOverridesExampleTestCase {
	}

	@MetaNonInheritedListenersWithOverrides(listeners = {//
	FooTestExecutionListener.class,//
		BarTestExecutionListener.class,//
		BazTestExecutionListener.class //
	},//
	inheritListeners = true)
	static class MetaNonInheritedListenersWithOverridesExampleTestCase extends
			MetaInheritedListenersWithOverridesExampleTestCase {
	}

	static class FooTestExecutionListener extends AbstractTestExecutionListener {
	}

	static class BarTestExecutionListener extends AbstractTestExecutionListener {
	}

	static class BazTestExecutionListener extends AbstractTestExecutionListener {
	}

	static class QuuxTestExecutionListener extends AbstractTestExecutionListener {
	}

	static class EnigmaTestExecutionListener extends AbstractTestExecutionListener {
	}

}