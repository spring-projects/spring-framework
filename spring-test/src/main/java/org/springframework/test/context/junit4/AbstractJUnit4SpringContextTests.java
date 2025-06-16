/*
 * Copyright 2002-2025 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jspecify.annotations.Nullable;
import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;

/**
 * Abstract base test class which integrates the <em>Spring TestContext
 * Framework</em> with explicit {@link ApplicationContext} testing support
 * in a <strong>JUnit 4</strong> environment.
 *
 * <p>Concrete subclasses should typically declare a class-level
 * {@link ContextConfiguration @ContextConfiguration} annotation to
 * configure the {@linkplain ApplicationContext application context} {@linkplain
 * ContextConfiguration#locations() resource locations} or {@linkplain
 * ContextConfiguration#classes() component classes}.
 *
 * <p>This class serves only as a convenience for extension.
 * <ul>
 * <li>If you do not wish for your test classes to be tied to a Spring-specific
 * class hierarchy, you may configure your own custom test classes by using
 * {@link SpringRunner}, {@link ContextConfiguration @ContextConfiguration},
 * {@link TestExecutionListeners @TestExecutionListeners}, etc.</li>
 * <li>If you wish to extend this class and use a runner other than the
 * {@link SpringRunner}, you can use
 * {@link org.springframework.test.context.junit4.rules.SpringClassRule SpringClassRule} and
 * {@link org.springframework.test.context.junit4.rules.SpringMethodRule SpringMethodRule}
 * and specify your runner of choice via {@link RunWith @RunWith(...)}.</li>
 * </ul>
 *
 * <p><strong>NOTE:</strong> This class requires JUnit 4.12 or higher.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.testng.AbstractTestNGSpringContextTests
 * @deprecated since Spring Framework 7.0 in favor of the
 * {@link org.springframework.test.context.junit.jupiter.SpringExtension SpringExtension}
 * and JUnit Jupiter
 */
@RunWith(SpringRunner.class)
@Deprecated(since = "7.0")
public abstract class AbstractJUnit4SpringContextTests implements ApplicationContextAware {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The {@link ApplicationContext} that was injected into this test instance
	 * via {@link #setApplicationContext(ApplicationContext)}.
	 */
	protected @Nullable ApplicationContext applicationContext;


	/**
	 * Set the {@link ApplicationContext} to be used by this test instance,
	 * provided via {@link ApplicationContextAware} semantics.
	 * @param applicationContext the ApplicationContext that this test runs in
	 */
	@Override
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
