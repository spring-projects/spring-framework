/*
 * Copyright 2002-2007 the original author or authors.
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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.test.context.support.AbstractTestExecutionListener;

/**
 * <p>
 * JUnit 4 based unit test for the
 * {@link TestExecutionListeners @TestExecutionListeners} annotation, which
 * verifies:
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
		TestContextManager testContextManager = new TestContextManager(DefaultListenersExampleTest.class);
		assertEquals("Verifying the number of registered TestExecutionListeners for DefaultListenersExampleTest.", 3,
				testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumNonInheritedDefaultListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(NonInheritedDefaultListenersExampleTest.class);
		assertEquals(
				"Verifying the number of registered TestExecutionListeners for NonInheritedDefaultListenersExampleTest.",
				1, testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumInheritedDefaultListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(InheritedDefaultListenersExampleTest.class);
		assertEquals(
				"Verifying the number of registered TestExecutionListeners for InheritedDefaultListenersExampleTest.",
				1, testContextManager.getTestExecutionListeners().size());

		testContextManager = new TestContextManager(SubInheritedDefaultListenersExampleTest.class);
		assertEquals(
				"Verifying the number of registered TestExecutionListeners for SubInheritedDefaultListenersExampleTest.",
				1, testContextManager.getTestExecutionListeners().size());

		testContextManager = new TestContextManager(SubSubInheritedDefaultListenersExampleTest.class);
		assertEquals(
				"Verifying the number of registered TestExecutionListeners for SubSubInheritedDefaultListenersExampleTest.",
				2, testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(ExampleTest.class);
		assertEquals("Verifying the number of registered TestExecutionListeners for ExampleTest.", 3,
				testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumNonInheritedListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(NonInheritedListenersExampleTest.class);
		assertEquals("Verifying the number of registered TestExecutionListeners for NonInheritedListenersExampleTest.",
				1, testContextManager.getTestExecutionListeners().size());
	}

	@Test
	public void verifyNumInheritedListenersRegistered() throws Exception {
		TestContextManager testContextManager = new TestContextManager(InheritedListenersExampleTest.class);
		assertEquals("Verifying the number of registered TestExecutionListeners for InheritedListenersExampleTest.", 4,
				testContextManager.getTestExecutionListeners().size());
	}


	static class DefaultListenersExampleTest {
	}

	@TestExecutionListeners( { QuuxTestExecutionListener.class })
	static class InheritedDefaultListenersExampleTest extends DefaultListenersExampleTest {
	}

	static class SubInheritedDefaultListenersExampleTest extends InheritedDefaultListenersExampleTest {
	}

	@TestExecutionListeners( { EnigmaTestExecutionListener.class })
	static class SubSubInheritedDefaultListenersExampleTest extends SubInheritedDefaultListenersExampleTest {
	}

	@TestExecutionListeners(value = { QuuxTestExecutionListener.class }, inheritListeners = false)
	static class NonInheritedDefaultListenersExampleTest extends InheritedDefaultListenersExampleTest {
	}

	@TestExecutionListeners( { FooTestExecutionListener.class, BarTestExecutionListener.class,
		BazTestExecutionListener.class })
	static class ExampleTest {
	}

	@TestExecutionListeners( { QuuxTestExecutionListener.class })
	static class InheritedListenersExampleTest extends ExampleTest {
	}

	@TestExecutionListeners(value = { QuuxTestExecutionListener.class }, inheritListeners = false)
	static class NonInheritedListenersExampleTest extends InheritedListenersExampleTest {
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
