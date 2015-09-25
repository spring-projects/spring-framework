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

package org.springframework.test.context;

import java.util.List;

/**
 * {@code TestContextBootstrapper} defines the SPI for bootstrapping the
 * <em>Spring TestContext Framework</em>.
 *
 * <p>A {@code TestContextBootstrapper} is used by the {@link TestContextManager} to
 * {@linkplain #getTestExecutionListeners get the TestExecutionListeners} for the
 * current test and to {@linkplain #buildTestContext build the TestContext} that
 * it manages.
 *
 * <h3>Configuration</h3>
 *
 * <p>A custom bootstrapping strategy can be configured for a test class (or
 * test class hierarchy) via {@link BootstrapWith @BootstrapWith}, either
 * directly or as a meta-annotation. See
 * {@link org.springframework.test.context.web.WebAppConfiguration @WebAppConfiguration}
 * for an example.
 *
 * <p>If a bootstrapper is not explicitly configured via {@code @BootstrapWith}, the
 * {@link org.springframework.test.context.support.DefaultTestContextBootstrapper DefaultTestContextBootstrapper}
 * will be used.
 *
 * <h3>Implementation Notes</h3>
 *
 * <p>Concrete implementations must provide a {@code public} no-args constructor.
 *
 * <p><strong>WARNING</strong>: this SPI will likely change in the future in
 * order to accommodate new requirements. Implementers are therefore strongly encouraged
 * <strong>not</strong> to implement this interface directly but rather to <em>extend</em>
 * {@link org.springframework.test.context.support.AbstractTestContextBootstrapper
 * AbstractTestContextBootstrapper} or one of its concrete subclasses instead.
 *
 * @author Sam Brannen
 * @since 4.1
 * @see BootstrapWith
 * @see BootstrapContext
 */
public interface TestContextBootstrapper {

	/**
	 * Set the {@link BootstrapContext} to be used by this bootstrapper.
	 */
	void setBootstrapContext(BootstrapContext bootstrapContext);

	/**
	 * Get the {@link BootstrapContext} associated with this bootstrapper.
	 */
	BootstrapContext getBootstrapContext();

	/**
	 * Build the {@link TestContext} for the {@link BootstrapContext}
	 * associated with this bootstrapper.
	 * @return a new {@link TestContext}, never {@code null}
	 * @since 4.2
	 * @see #buildMergedContextConfiguration()
	 */
	TestContext buildTestContext();

	/**
	 * Build the {@linkplain MergedContextConfiguration merged context configuration}
	 * for the test class in the {@link BootstrapContext} associated with this
	 * bootstrapper.
	 * <p>Implementations must take the following into account when building the
	 * merged configuration:
	 * <ul>
	 * <li>Context hierarchies declared via {@link ContextHierarchy @ContextHierarchy}
	 * and {@link ContextConfiguration @ContextConfiguration}</li>
	 * <li>Active bean definition profiles declared via {@link ActiveProfiles @ActiveProfiles}</li>
	 * <li>{@linkplain org.springframework.context.ApplicationContextInitializer
	 * Context initializers} declared via {@link ContextConfiguration#initializers}</li>
	 * <li>Test property sources declared via {@link TestPropertySource @TestPropertySource}</li>
	 * </ul>
	 * <p>Consult the Javadoc for the aforementioned annotations for details on
	 * the required semantics.
	 * <p>Note that the implementation of {@link #buildTestContext()} should
	 * typically delegate to this method when constructing the {@code TestContext}.
	 * <p>When determining which {@link ContextLoader} to use for a given test
	 * class, the following algorithm should be used:
	 * <ol>
	 * <li>If a {@code ContextLoader} class has been explicitly declared via
	 * {@link ContextConfiguration#loader}, use it.</li>
	 * <li>Otherwise, concrete implementations are free to determine which
	 * {@code ContextLoader} class to use as a default.</li>
	 * </ol>
	 * @return the merged context configuration, never {@code null}
	 * @see #buildTestContext()
	 */
	MergedContextConfiguration buildMergedContextConfiguration();

	/**
	 * Get a list of newly instantiated {@link TestExecutionListener TestExecutionListeners}
	 * for the test class in the {@link BootstrapContext} associated with this bootstrapper.
	 * <p>If {@link TestExecutionListeners @TestExecutionListeners} is not
	 * <em>present</em> on the test class in the {@code BootstrapContext},
	 * <em>default</em> listeners should be returned. Furthermore, default
	 * listeners must be sorted using
	 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator
	 * AnnotationAwareOrderComparator}.
	 * <p>Concrete implementations are free to determine what comprises the
	 * set of default listeners. However, by default, the Spring TestContext
	 * Framework will use the
	 * {@link org.springframework.core.io.support.SpringFactoriesLoader SpringFactoriesLoader}
	 * mechanism to look up all {@code TestExecutionListener} class names
	 * configured in all {@code META-INF/spring.factories} files on the classpath.
	 * <p>The {@link TestExecutionListeners#inheritListeners() inheritListeners}
	 * flag of {@link TestExecutionListeners @TestExecutionListeners} must be
	 * taken into consideration. Specifically, if the {@code inheritListeners}
	 * flag is set to {@code true}, listeners declared for a given test class must
	 * be appended to the end of the list of listeners declared in superclasses.
	 * @return a list of {@code TestExecutionListener} instances
	 */
	List<TestExecutionListener> getTestExecutionListeners();

}
