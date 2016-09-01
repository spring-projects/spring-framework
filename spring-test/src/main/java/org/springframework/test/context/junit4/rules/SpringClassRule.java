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

package org.springframework.test.context.junit4.rules;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.Rule;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestClassCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestClassCallbacks;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * {@code SpringClassRule} is a custom JUnit {@link TestRule} that supports
 * <em>class-level</em> features of the <em>Spring TestContext Framework</em>
 * in standard JUnit tests by means of the {@link TestContextManager} and
 * associated support classes and annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, a {@code SpringClassRule} must be combined with a {@link SpringMethodRule},
 * since {@code SpringClassRule} only supports the class-level features of the
 * {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule();
 *
 *    // ...
 * }</code></pre>
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringClassRule}. <em>(Note that additional annotations
 * may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} or
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> As of Spring Framework 4.3, this class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #apply(Statement, Description)
 * @see SpringMethodRule
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
public class SpringClassRule implements TestRule {

	private static final Log logger = LogFactory.getLog(SpringClassRule.class);

	/**
	 * Cache of {@code TestContextManagers} keyed by test class.
	 */
	private static final Map<Class<?>, TestContextManager> testContextManagerCache =
			new ConcurrentHashMap<>(64);

	static {
		Assert.state(ClassUtils.isPresent("org.junit.internal.Throwables", SpringClassRule.class.getClassLoader()),
				"SpringClassRule requires JUnit 4.12 or higher.");
	}


	/**
	 * Apply <em>class-level</em> features of the <em>Spring TestContext
	 * Framework</em> to the supplied {@code base} statement.
	 * <p>Specifically, this method retrieves the {@link TestContextManager}
	 * used by this rule and its associated {@link SpringMethodRule} and
	 * invokes the {@link TestContextManager#beforeTestClass() beforeTestClass()}
	 * and {@link TestContextManager#afterTestClass() afterTestClass()} methods
	 * on the {@code TestContextManager}.
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents classes with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code beforeTestClass()} methods
	 * in {@code TestExecutionListeners}.
	 * @param base the base {@code Statement} that this rule should be applied to
	 * @param description a {@code Description} of the current test execution
	 * @return a statement that wraps the supplied {@code base} with class-level
	 * features of the Spring TestContext Framework
	 * @see #getTestContextManager
	 * @see #withBeforeTestClassCallbacks
	 * @see #withAfterTestClassCallbacks
	 * @see #withProfileValueCheck
	 * @see #withTestContextManagerCacheEviction
	 */
	@Override
	public Statement apply(Statement base, Description description) {
		Class<?> testClass = description.getTestClass();
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringClassRule to test class [" + testClass.getName() + "]");
		}
		validateSpringMethodRuleConfiguration(testClass);
		TestContextManager testContextManager = getTestContextManager(testClass);

		Statement statement = base;
		statement = withBeforeTestClassCallbacks(statement, testContextManager);
		statement = withAfterTestClassCallbacks(statement, testContextManager);
		statement = withProfileValueCheck(statement, testClass);
		statement = withTestContextManagerCacheEviction(statement, testClass);
		return statement;
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code RunBeforeTestClassCallbacks} statement.
	 * @see RunBeforeTestClassCallbacks
	 */
	private Statement withBeforeTestClassCallbacks(Statement statement, TestContextManager testContextManager) {
		return new RunBeforeTestClassCallbacks(statement, testContextManager);
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code RunAfterTestClassCallbacks} statement.
	 * @see RunAfterTestClassCallbacks
	 */
	private Statement withAfterTestClassCallbacks(Statement statement, TestContextManager testContextManager) {
		return new RunAfterTestClassCallbacks(statement, testContextManager);
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code ProfileValueChecker} statement.
	 * @see ProfileValueChecker
	 */
	private Statement withProfileValueCheck(Statement statement, Class<?> testClass) {
		return new ProfileValueChecker(statement, testClass, null);
	}

	/**
	 * Wrap the supplied {@code statement} with a {@code TestContextManagerCacheEvictor} statement.
	 * @see TestContextManagerCacheEvictor
	 */
	private Statement withTestContextManagerCacheEviction(Statement statement, Class<?> testClass) {
		return new TestContextManagerCacheEvictor(statement, testClass);
	}


	/**
	 * Throw an {@link IllegalStateException} if the supplied {@code testClass}
	 * does not declare a {@code public SpringMethodRule} field that is
	 * annotated with {@code @Rule}.
	 */
	private static void validateSpringMethodRuleConfiguration(Class<?> testClass) {
		Field ruleField = findSpringMethodRuleField(testClass);

		Assert.state(ruleField != null, () -> String.format(
					"Failed to find 'public SpringMethodRule' field in test class [%s]. " +
					"Consult the javadoc for SpringClassRule for details.", testClass.getName()));

		Assert.state(ruleField.isAnnotationPresent(Rule.class), () -> String.format(
					"SpringMethodRule field [%s] must be annotated with JUnit's @Rule annotation. " +
					"Consult the javadoc for SpringClassRule for details.", ruleField));
	}

	private static Field findSpringMethodRuleField(Class<?> testClass) {
		for (Field field : testClass.getFields()) {
			int modifiers = field.getModifiers();
			if (!Modifier.isStatic(modifiers) && Modifier.isPublic(modifiers) &&
					SpringMethodRule.class.isAssignableFrom(field.getType())) {
				return field;
			}
		}
		return null;
	}

	/**
	 * Get the {@link TestContextManager} associated with the supplied test class.
	 * @param testClass the test class to be managed; never {@code null}
	 */
	static TestContextManager getTestContextManager(Class<?> testClass) {
		Assert.notNull(testClass, "testClass must not be null");
		return testContextManagerCache.computeIfAbsent(testClass, TestContextManager::new);
	}


	private static class TestContextManagerCacheEvictor extends Statement {

		private final Statement next;

		private final Class<?> testClass;


		TestContextManagerCacheEvictor(Statement next, Class<?> testClass) {
			this.next = next;
			this.testClass = testClass;
		}

		@Override
		public void evaluate() throws Throwable {
			try {
				next.evaluate();
			}
			finally {
				testContextManagerCache.remove(testClass);
			}
		}
	}

}
