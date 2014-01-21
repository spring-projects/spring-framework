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

package org.springframework.test.context.junit4;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.junit.runner.RunWith;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.support.DirtiesContextTestExecutionListener;
import org.springframework.test.context.web.ServletTestExecutionListener;

/**
 * Abstract base test class which integrates the <em>Spring TestContext
 * Framework</em> with explicit {@link ApplicationContext} testing support in a
 * <strong>JUnit 4.5+</strong> environment.
 *
 * <p>Concrete subclasses should typically declare a class-level
 * {@link ContextConfiguration &#064;ContextConfiguration} annotation to
 * configure the {@link ApplicationContext application context} {@link
 * ContextConfiguration#locations() resource locations} or {@link
 * ContextConfiguration#classes() annotated classes}. <em>If your test does not
 * need to load an application context, you may choose to omit the {@link
 * ContextConfiguration &#064;ContextConfiguration} declaration and to configure
 * the appropriate {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners} manually.</em>
 *
 * <p>The following {@link org.springframework.test.context.TestExecutionListener
 * TestExecutionListeners} are configured by default:
 *
 * <ul>
 *   <li>{@link org.springframework.test.context.web.ServletTestExecutionListener}
 *   <li>{@link org.springframework.test.context.support.DependencyInjectionTestExecutionListener}
 *   <li>{@link org.springframework.test.context.support.DirtiesContextTestExecutionListener}
 * </ul>
 *
 * <p>Note: this class serves only as a convenience for extension. If you do not
 * wish for your test classes to be tied to a Spring-specific class hierarchy,
 * you may configure your own custom test classes by using
 * {@link SpringJUnit4ClassRunner}, {@link ContextConfiguration
 * &#064;ContextConfiguration}, {@link TestExecutionListeners
 * &#064;TestExecutionListeners}, etc.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see ContextConfiguration
 * @see TestContext
 * @see TestContextManager
 * @see TestExecutionListeners
 * @see ServletTestExecutionListener
 * @see DependencyInjectionTestExecutionListener
 * @see DirtiesContextTestExecutionListener
 * @see AbstractTransactionalJUnit4SpringContextTests
 * @see org.springframework.test.context.testng.AbstractTestNGSpringContextTests
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestExecutionListeners({ ServletTestExecutionListener.class, DependencyInjectionTestExecutionListener.class,
	DirtiesContextTestExecutionListener.class })
public abstract class AbstractJUnit4SpringContextTests implements ApplicationContextAware {

	/**
	 * Logger available to subclasses.
	 */
	protected final Log logger = LogFactory.getLog(getClass());

	/**
	 * The {@link ApplicationContext} that was injected into this test instance
	 * via {@link #setApplicationContext(ApplicationContext)}.
	 */
	protected ApplicationContext applicationContext;


	/**
	 * Set the {@link ApplicationContext} to be used by this test instance,
	 * provided via {@link ApplicationContextAware} semantics.
	 */
	@Override
	public final void setApplicationContext(final ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}

}
