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

package org.springframework.test.context;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.core.annotation.AliasFor;

/**
 * {@code TestExecutionListeners} defines class-level metadata for configuring
 * which {@link TestExecutionListener TestExecutionListeners} should be
 * registered with a {@link TestContextManager}.
 *
 * <p>Typically, {@code @TestExecutionListeners} will be used in conjunction
 * with {@link ContextConfiguration @ContextConfiguration}.
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
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface TestExecutionListeners {

	/**
	 * Alias for {@link #listeners}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #listeners}, but it may be used instead of {@link #listeners}.
	 */
	@AliasFor("listeners")
	Class<? extends TestExecutionListener>[] value() default {};

	/**
	 * The {@link TestExecutionListener TestExecutionListeners} to register with
	 * the {@link TestContextManager}.
	 * <p>This attribute may <strong>not</strong> be used in conjunction with
	 * {@link #value}, but it may be used instead of {@link #value}.
	 * @see org.springframework.test.context.event.EventPublishingTestExecutionListener
	 * @see org.springframework.test.context.web.ServletTestExecutionListener
	 * @see org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
	 * @see org.springframework.test.context.support.DependencyInjectionTestExecutionListener
	 * @see org.springframework.test.context.support.DirtiesContextTestExecutionListener
	 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
	 * @see org.springframework.test.context.jdbc.SqlScriptsTestExecutionListener
	 */
	@AliasFor("value")
	Class<? extends TestExecutionListener>[] listeners() default {};

	/**
	 * Whether or not {@link #listeners TestExecutionListeners} from superclasses
	 * should be <em>inherited</em>.
	 * <p>The default value is {@code true}, which means that an annotated
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
	 * <pre class="code">
	 * &#064;TestExecutionListeners({
	 *     DependencyInjectionTestExecutionListener.class,
	 *     DirtiesContextTestExecutionListener.class
	 * })
	 * public abstract class AbstractBaseTest {
	 * 	 // ...
	 * }
	 *
	 * &#064;TestExecutionListeners(TransactionalTestExecutionListener.class)
	 * public class TransactionalTest extends AbstractBaseTest {
	 * 	 // ...
	 * }</pre>
	 * <p>If {@code inheritListeners} is set to {@code false}, the listeners for
	 * the annotated class will <em>shadow</em> and effectively replace any
	 * listeners defined by a superclass.
	 */
	boolean inheritListeners() default true;

	/**
	 * The <em>merge mode</em> to use when {@code @TestExecutionListeners} is
	 * declared on a class that does <strong>not</strong> inherit listeners
	 * from a superclass.
	 * <p>Can be set to {@link MergeMode#MERGE_WITH_DEFAULTS MERGE_WITH_DEFAULTS}
	 * to have locally declared listeners <em>merged</em> with the default
	 * listeners.
	 * <p>The mode is ignored if listeners are inherited from a superclass.
	 * <p>Defaults to {@link MergeMode#REPLACE_DEFAULTS REPLACE_DEFAULTS}
	 * for backwards compatibility.
	 * @see MergeMode
	 * @since 4.1
	 */
	MergeMode mergeMode() default MergeMode.REPLACE_DEFAULTS;


	/**
	 * Enumeration of <em>modes</em> that dictate whether or not explicitly
	 * declared listeners are merged with the default listeners when
	 * {@code @TestExecutionListeners} is declared on a class that does
	 * <strong>not</strong> inherit listeners from a superclass.
	 * @since 4.1
	 */
	enum MergeMode {

		/**
		 * Indicates that locally declared listeners should replace the default
		 * listeners.
		 */
		REPLACE_DEFAULTS,

		/**
		 * Indicates that locally declared listeners should be merged with the
		 * default listeners.
		 * <p>The merging algorithm ensures that duplicates are removed from
		 * the list and that the resulting set of merged listeners is sorted
		 * according to the semantics of
		 * {@link org.springframework.core.annotation.AnnotationAwareOrderComparator
		 * AnnotationAwareOrderComparator}. If a listener implements
		 * {@link org.springframework.core.Ordered Ordered} or is annotated
		 * with {@link org.springframework.core.annotation.Order @Order} it can
		 * influence the position in which it is merged with the defaults; otherwise,
		 * locally declared listeners will simply be appended to the list of default
		 * listeners when merged.
		 */
		MERGE_WITH_DEFAULTS
	}

}
