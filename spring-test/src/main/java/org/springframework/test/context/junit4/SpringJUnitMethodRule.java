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

import java.lang.reflect.Method;

import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.annotation.ProfileValueUtils;
import org.springframework.test.context.TestContextManager;


/**
 * <p>
 * {@code MethodRule} is a custom {@link MethodRule} which together with
 * {@link SpringJUnitClassRule} provides functionality of the <em>Spring
 * TestContext Framework</em> to standard JUnit 4.9+ tests by means of the
 * {@link TestContextManager} and associated support classes and annotations.
 * </p>
 * <p>
 * The only feature not supported of {@link SpringJUnit4ClassRunner} is
 * {@link ExpectedException}.
 * </p>
 * 
 * @author Philippe Marschall
 * @since 3.2.2
 * @see SpringJUnitClassRule
 */
public class SpringJUnitMethodRule implements MethodRule {
	
	// Hold on to the class rule instead of the TestContextManager because
	// the class rule "owns" the TestContextManager can releases it when no
	// longer needed.
	private final SpringJUnitClassRule classRule;

	/**
	 * Constructs a new {@code SpringJUnitMethodRule}.
	 * 
	 * @param classRule the class rule, not {@code null},
	 * 		the class rule has to be defined in the same test class
	 * 		where this {@link SpringJUnitMethodRule} instance is defined
	 */
	public SpringJUnitMethodRule(SpringJUnitClassRule classRule) {
		this.classRule = classRule;
	}

	/**
	 * Prepares the test instance (performs the injection) and runs the before
	 * and after test methods on the {@link TestContextManager}. 
	 * 
	 * @see TestContextManager#prepareTestInstance(Object)
	 * @see TestContextManager#beforeTestMethod(Object, Method)
	 * @see TestContextManager#afterTestMethod(Object, Method, Throwable)
	 */
	@Override
	public Statement apply(final Statement base, final FrameworkMethod method, final Object target) {
		return new Statement() {

			@Override
			public void evaluate() throws Throwable {
				Method testMethod = method.getMethod();
				boolean profileIsActive = ProfileValueUtils.isTestEnabledInThisEnvironment(testMethod, target.getClass());
				assumeTrue("required profile not active", profileIsActive);
				TestContextManager testContextManager = classRule.getTestContextManager();
				testContextManager.prepareTestInstance(target);
				testContextManager.beforeTestMethod(target, testMethod);
				try {
					base.evaluate();
				}
				catch (Throwable t) {
					testContextManager.afterTestMethod(target, testMethod, t);
					throw t;
				}
				testContextManager.afterTestMethod(target, testMethod, null);
			}
		};
	}

}
