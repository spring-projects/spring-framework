/*
 * Copyright 2004-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.test.context.junit4;

import static org.junit.Assume.assumeTrue;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;


/**
 * <p>
 * {@code SpringJUnitClassRule} is a custom {@link TestRule} which provides
 * functionality of the <em>Spring TestContext Framework</em> to standard
 * JUnit 4.9+ tests by means of the {@link TestContextManager} and associated
 * support classes and annotations.
 * </p>
 * <p>
 * Compared to {@link SpringJUnit4ClassRunner} the rule based JUnit support
 * has the advantage that it is independent of the runner and can therefore
 * be combined with existing 3rd party runners like {@code Parameterized}.
 * </p>
 * <p>
 * However to achieve the same functionality as {@link SpringJUnit4ClassRunner}
 * {@code SpringJUnitClassRule} has to be combined with
 * {@link SpringJUnitMethodRule} as {@code SpringJUnitClassRule} only provides
 * the class level features of {@link SpringJUnit4ClassRunner}.
 * </p>
 * 
 * <p>
 * The following example shows you how to use {@code SpringJUnitClassRule}.
 * </p>
 * <pre><code>
 * public class ExampleTest {
 *   
 *   @ClassRule
 *   public static final SpringJUnitClassRule CLASS_RULE = new SpringJUnitClassRule();
 *   
 *   @Rule
 *   public MethodRule methodRule = new SpringJUnitMethodRule(CLASS_RULE);
 * }
 * </code></pre>
 * 
 * @author Philippe Marschall
 * @since 3.2.2
 * @see SpringJUnit4ClassRunner
 * @see TestContextManager
 * @see SpringJUnitMethodRule
 */
public class SpringJUnitClassRule implements TestRule {
	
	// volatile since SpringJUnitMethodRule can potentially access it from a
	// different thread depending on the runner.
	private volatile TestContextManager testContextManager;
	
	/**
	 * Get the {@link TestContextManager} associated with this runner.
	 * Will be {@code null} until this class is called by the JUnit framework.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.testContextManager;
	}
	

	/**
	 * Creates a new {@link TestContextManager} for the supplied test class and
	 * the configured <em>default {@code ContextLoader} class name</em>.
	 * Can be overridden by subclasses.
	 * @param clazz the test class to be managed
	 * @see #getDefaultContextLoaderClassName(Class)
	 */
	protected TestContextManager createTestContextManager(Class<?> clazz) {
		return new TestContextManager(clazz, getDefaultContextLoaderClassName(clazz));
	}
	

	/**
	 * Get the name of the default {@code ContextLoader} class to use for
	 * the supplied test class. The named class will be used if the test class
	 * does not explicitly declare a {@code ContextLoader} class via the
	 * {@code &#064;ContextConfiguration} annotation.
	 * <p>The default implementation returns {@code null}, thus implying use
	 * of the <em>standard</em> default {@code ContextLoader} class name.
	 * Can be overridden by subclasses.
	 * @param clazz the test class
	 * @return {@code null}
	 */
	protected String getDefaultContextLoaderClassName(Class<?> clazz) {
		return null;
	}
	

	/**
	 * Check whether the test is enabled in the first place. This prevents
	 * classes with a non-matching {@code &#064;IfProfileValue} annotation
	 * from running altogether, even skipping the execution of
	 * {@code prepareTestInstance()} {@code TestExecutionListener}
	 * methods.
	 * Creates the {@link TestContextManager} and runs it's before and after
	 * class methods. 
	 * 
	 * @see ProfileValueUtils#isTestEnabledInThisEnvironment(Class)
	 * @see org.springframework.test.annotation.IfProfileValue
	 * @see org.springframework.test.context.TestExecutionListener
	 * @see #createTestContextManager(Class)
	 * @see TestContextManager#beforeTestClass()
	 * @see TestContextManager#afterTestClass()
	 */
	@Override
	public Statement apply(final Statement base, final Description description) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Class<?> testClass = description.getTestClass();
				boolean profileIsActive = ProfileValueUtils.isTestEnabledInThisEnvironment(testClass);
				assumeTrue("required profile not active", profileIsActive);
				testContextManager = createTestContextManager(testClass);
				testContextManager.beforeTestClass();
				try {
					base.evaluate();
				} finally {
					testContextManager.afterTestClass();
					// make test context manager eligible for garbage collection
					testContextManager = null;
				}

			}
		};
	}

}
