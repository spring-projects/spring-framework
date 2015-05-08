/*
 * Copyright 2002-2015 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.ClassRule;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.test.annotation.Repeat;
import org.springframework.test.annotation.Timed;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.statements.ProfileValueChecker;
import org.springframework.test.context.junit4.statements.RunAfterTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunBeforeTestMethodCallbacks;
import org.springframework.test.context.junit4.statements.RunPrepareTestInstanceCallbacks;
import org.springframework.test.context.junit4.statements.SpringFailOnTimeout;
import org.springframework.test.context.junit4.statements.SpringRepeat;
import org.springframework.util.Assert;
import org.springframework.util.ReflectionUtils;

/**
 * {@code SpringMethodRule} is a custom JUnit {@link MethodRule} that
 * provides instance-level and method-level functionality of the
 * <em>Spring TestContext Framework</em> to standard JUnit tests by means
 * of the {@link TestContextManager} and associated support classes and
 * annotations.
 *
 * <p>In contrast to the {@link org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 * SpringJUnit4ClassRunner}, Spring's rule-based JUnit support has the advantage
 * that it is independent of any {@link org.junit.runner.Runner Runner} and
 * can therefore be combined with existing alternative runners like JUnit's
 * {@code Parameterized} or third-party runners such as the {@code MockitoJUnitRunner}.
 *
 * <p>In order to achieve the same functionality as the {@code SpringJUnit4ClassRunner},
 * however, a {@code SpringMethodRule} must be combined with a {@link SpringClassRule},
 * since {@code SpringMethodRule} only provides the method-level features of the
 * {@code SpringJUnit4ClassRunner}.
 *
 * <h3>Example Usage</h3>
 * <pre><code> public class ExampleSpringIntegrationTest {
 *
 *    &#064;ClassRule
 *    public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
 *
 *    &#064;Rule
 *    public final SpringMethodRule springMethodRule = new SpringMethodRule(this);
 *
 *    // ...
 * }</code></pre>
 *
 * <p>The following list constitutes all annotations currently supported directly
 * or indirectly by {@code SpringMethodRule}. <em>(Note that additional annotations
 * may be supported by various
 * {@link org.springframework.test.context.TestExecutionListener TestExecutionListener} or
 * {@link org.springframework.test.context.TestContextBootstrapper TestContextBootstrapper}
 * implementations.)</em>
 *
 * <ul>
 * <li>{@link Timed @Timed}</li>
 * <li>{@link Repeat @Repeat}</li>
 * <li>{@link org.springframework.test.annotation.ProfileValueSourceConfiguration @ProfileValueSourceConfiguration}</li>
 * <li>{@link org.springframework.test.annotation.IfProfileValue @IfProfileValue}</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.9 or higher.
 *
 * @author Sam Brannen
 * @author Philippe Marschall
 * @since 4.2
 * @see #apply(Statement, FrameworkMethod, Object)
 * @see SpringClassRule
 * @see org.springframework.test.context.TestContextManager
 * @see org.springframework.test.context.junit4.SpringJUnit4ClassRunner
 */
public class SpringMethodRule implements MethodRule {

	private static final Log logger = LogFactory.getLog(SpringMethodRule.class);

	/**
	 * {@code SpringMethodRule} retains a reference to the {@code SpringClassRule}
	 * instead of the {@code TestContextManager}, since the class rule <em>owns</em>
	 * the {@code TestContextManager}.
	 */
	private final SpringClassRule springClassRule;


	/**
	 * Construct a new {@code SpringMethodRule} for the supplied test instance.
	 *
	 * <p>The test class must declare a {@code public static final SpringClassRule}
	 * field (i.e., a <em>constant</em>) that is annotated with JUnit's
	 * {@link ClassRule @ClassRule} &mdash; for example:
	 *
	 * <pre><code> &#064;ClassRule
	 * public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();</code></pre>
	 *
	 * @param testInstance the test instance, never {@code null}
	 * @throws IllegalStateException if the test class does not declare an
	 * appropriate {@code SpringClassRule} constant.
	 */
	public SpringMethodRule(Object testInstance) {
		Assert.notNull(testInstance, "testInstance must not be null");
		this.springClassRule = retrieveAndValidateSpringClassRule(testInstance.getClass());
	}

	/**
	 * Apply <em>instance-level</em> and <em>method-level</em> functionality
	 * of the <em>Spring TestContext Framework</em> to the supplied {@code base}
	 * statement.
	 *
	 * <p>Specifically, this method invokes the
	 * {@link TestContextManager#prepareTestInstance prepareTestInstance()},
	 * {@link TestContextManager#beforeTestMethod beforeTestMethod()}, and
	 * {@link TestContextManager#afterTestMethod afterTestMethod()} methods
	 * on the {@code TestContextManager}, potentially with Spring timeouts
	 * and repetitions.
	 *
	 * <p>In addition, this method checks whether the test is enabled in
	 * the current execution environment. This prevents methods with a
	 * non-matching {@code @IfProfileValue} annotation from running altogether,
	 * even skipping the execution of {@code prepareTestInstance()} methods
	 * in {@code TestExecutionListeners}.
	 *
	 * @param base the base {@code Statement} that this rule should be applied to
	 * @param frameworkMethod the method which is about to be invoked on the test instance
	 * @param testInstance the current test instance
	 * @return a statement that wraps the supplied {@code base} with instance-level
	 * and method-level functionality of the Spring TestContext Framework
	 * @see #withBeforeTestMethodCallbacks
	 * @see #withAfterTestMethodCallbacks
	 * @see #withPotentialRepeat
	 * @see #withPotentialTimeout
	 * @see #withTestInstancePreparation
	 * @see #withProfileValueCheck
	 */
	@Override
	public Statement apply(final Statement base, final FrameworkMethod frameworkMethod, final Object testInstance) {
		if (logger.isDebugEnabled()) {
			logger.debug("Applying SpringMethodRule to test method [" + frameworkMethod.getMethod() + "].");
		}

		Statement statement = base;
		statement = withBeforeTestMethodCallbacks(frameworkMethod, testInstance, statement);
		statement = withAfterTestMethodCallbacks(frameworkMethod, testInstance, statement);
		statement = withTestInstancePreparation(testInstance, statement);
		statement = withPotentialRepeat(frameworkMethod, testInstance, statement);
		statement = withPotentialTimeout(frameworkMethod, testInstance, statement);
		statement = withProfileValueCheck(frameworkMethod, testInstance, statement);
		return statement;
	}

	/**
	 * Get the {@link TestContextManager} associated with this rule.
	 */
	protected final TestContextManager getTestContextManager() {
		return this.springClassRule.getTestContextManager();
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code ProfileValueChecker} statement.
	 * @see ProfileValueChecker
	 */
	protected Statement withProfileValueCheck(FrameworkMethod frameworkMethod, Object testInstance, Statement statement) {
		return new ProfileValueChecker(statement, testInstance.getClass(), frameworkMethod.getMethod());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunPrepareTestInstanceCallbacks} statement.
	 * @see RunPrepareTestInstanceCallbacks
	 */
	protected Statement withTestInstancePreparation(Object testInstance, Statement statement) {
		return new RunPrepareTestInstanceCallbacks(statement, testInstance, getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunBeforeTestMethodCallbacks} statement.
	 * @see RunBeforeTestMethodCallbacks
	 */
	protected Statement withBeforeTestMethodCallbacks(FrameworkMethod frameworkMethod, Object testInstance,
			Statement statement) {
		return new RunBeforeTestMethodCallbacks(statement, testInstance, frameworkMethod.getMethod(),
			getTestContextManager());
	}

	/**
	 * Wrap the supplied {@link Statement} with a {@code RunAfterTestMethodCallbacks} statement.
	 * @see RunAfterTestMethodCallbacks
	 */
	protected Statement withAfterTestMethodCallbacks(FrameworkMethod frameworkMethod, Object testInstance,
			Statement statement) {
		return new RunAfterTestMethodCallbacks(statement, testInstance, frameworkMethod.getMethod(),
			getTestContextManager());
	}

	/**
	 * Return a {@link Statement} that potentially repeats the execution of
	 * the {@code next} statement.
	 * <p>Supports Spring's {@link Repeat @Repeat} annotation by returning a
	 * {@link SpringRepeat} statement initialized with the configured repeat
	 * count (if greater than {@code 1}); otherwise, the supplied statement
	 * is returned unmodified.
	 * @return either a {@code SpringRepeat} or the supplied {@code Statement}
	 * @see SpringRepeat
	 */
	protected Statement withPotentialRepeat(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		Repeat repeatAnnotation = AnnotationUtils.getAnnotation(frameworkMethod.getMethod(), Repeat.class);
		int repeat = (repeatAnnotation != null ? repeatAnnotation.value() : 1);
		return (repeat > 1 ? new SpringRepeat(next, frameworkMethod.getMethod(), repeat) : next);
	}

	/**
	 * Return a {@link Statement} that potentially throws an exception if
	 * the {@code next} statement in the execution chain takes longer than
	 * a specified timeout.
	 * <p>Supports Spring's {@link Timed @Timed} annotation by returning a
	 * {@link SpringFailOnTimeout} statement initialized with the configured
	 * timeout (if greater than {@code 0}); otherwise, the supplied statement
	 * is returned unmodified.
	 * @return either a {@code SpringFailOnTimeout} or the supplied {@code Statement}
	 * @see #getSpringTimeout(FrameworkMethod)
	 * @see SpringFailOnTimeout
	 */
	protected Statement withPotentialTimeout(FrameworkMethod frameworkMethod, Object testInstance, Statement next) {
		long springTimeout = getSpringTimeout(frameworkMethod);
		return (springTimeout > 0 ? new SpringFailOnTimeout(next, springTimeout) : next);
	}

	/**
	 * Retrieve the configured Spring-specific {@code timeout} from the
	 * {@link Timed @Timed} annotation on the supplied
	 * {@linkplain FrameworkMethod test method}.
	 * @return the timeout, or {@code 0} if none was specified
	 */
	protected long getSpringTimeout(FrameworkMethod frameworkMethod) {
		AnnotationAttributes annAttrs = AnnotatedElementUtils.findAnnotationAttributes(frameworkMethod.getMethod(),
			Timed.class.getName());
		if (annAttrs == null) {
			return 0;
		}
		else {
			long millis = annAttrs.<Long> getNumber("millis").longValue();
			return millis > 0 ? millis : 0;
		}
	}

	private static SpringClassRule retrieveAndValidateSpringClassRule(Class<?> testClass) {
		Field springClassRuleField = null;

		for (Field field : testClass.getFields()) {
			if (ReflectionUtils.isPublicStaticFinal(field) && (SpringClassRule.class.isAssignableFrom(field.getType()))) {
				springClassRuleField = field;
				break;
			}
		}

		if (springClassRuleField == null) {
			throw new IllegalStateException(String.format(
				"Failed to find 'public static final SpringClassRule' field in test class [%s]. "
						+ "Consult the Javadoc for SpringClassRule for details.", testClass.getName()));
		}

		if (!springClassRuleField.isAnnotationPresent(ClassRule.class)) {
			throw new IllegalStateException(String.format(
				"SpringClassRule field [%s] must be annotated with JUnit's @ClassRule annotation. "
						+ "Consult the Javadoc for SpringClassRule for details.", springClassRuleField));
		}

		return (SpringClassRule) ReflectionUtils.getField(springClassRuleField, null);
	}

}
