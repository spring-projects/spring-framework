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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code TestExecutionListeners} defines class-level metadata for
 * configuring which {@link TestExecutionListener TestExecutionListeners} should
 * be registered with a {@link TestContextManager}.
 *
 * <p>Typically, {@code @TestExecutionListeners} will be used in conjunction with
 * {@link ContextConfiguration @ContextConfiguration}.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see TestExecutionListener
 * @see TestContextManager
 * @see ContextConfiguration
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface TestExecutionListeners {

	/**
	 * The {@link TestExecutionListener TestExecutionListeners} to register with
	 * a {@link TestContextManager}.
	 *
	 * @see org.springframework.test.context.web.ServletTestExecutionListener
	 * @see org.springframework.test.context.support.DependencyInjectionTestExecutionListener
	 * @see org.springframework.test.context.support.DirtiesContextTestExecutionListener
	 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
	 */
	Class<? extends TestExecutionListener>[] listeners() default {};

	/**
	 * Alias for {@link #listeners() listeners}.
	 */
	Class<? extends TestExecutionListener>[] value() default {};

	/**
	 * Whether or not {@link #value() TestExecutionListeners} from superclasses
	 * should be <em>inherited</em>.
	 * <p>
	 * The default value is {@code true}, which means that an annotated
	 * class will <em>inherit</em> the listeners defined by an annotated
	 * superclass. Specifically, the listeners for an annotated class will be
	 * appended to the list of listeners defined by an annotated superclass.
	 * Thus, subclasses have the option of <em>extending</em> the list of
	 * listeners. In the following example, {@code AbstractBaseTest} will
	 * be configured with {@code DependencyInjectionTestExecutionListener}
	 * and {@code DirtiesContextTestExecutionListener}; whereas,
	 * {@code TransactionalTest} will be configured with
	 * {@code DependencyInjectionTestExecutionListener},
	 * {@code DirtiesContextTestExecutionListener}, <strong>and</strong>
	 * {@code TransactionalTestExecutionListener}, in that order.
	 *
	 * <pre class="code">
	 * &#064;TestExecutionListeners({
	 *    DependencyInjectionTestExecutionListener.class,
	 *    DirtiesContextTestExecutionListener.class
	 * })
	 * public abstract class AbstractBaseTest {
	 * 	// ...
	 * }
	 *
	 * &#064;TestExecutionListeners(TransactionalTestExecutionListener.class)
	 * public class TransactionalTest extends AbstractBaseTest {
	 * 	// ...
	 * }</pre>
	 *
	 * <p>
	 * If {@code inheritListeners} is set to {@code false}, the listeners for the
	 * annotated class will <em>shadow</em> and effectively replace any listeners
	 * defined by a superclass.
	 */
	boolean inheritListeners() default true;

}
