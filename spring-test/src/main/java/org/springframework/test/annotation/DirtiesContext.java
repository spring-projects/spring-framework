/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation which indicates that the
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * associated with a test is <em>dirty</em> and should therefore be closed
 * and removed from the context cache.
 *
 * <p>Use this annotation if a test has modified the context &mdash; for
 * example, by modifying the state of a singleton bean, modifying the state
 * of an embedded database, etc. Subsequent tests that request the same context
 * will be supplied a new context.
 *
 * <p>{@code @DirtiesContext} may be used as a class-level and method-level
 * annotation within the same test class or test class hierarchy. In such scenarios,
 * the {@code ApplicationContext} will be marked as <em>dirty</em> before or
 * after any such annotated method as well as before or after the current test
 * class, depending on the configured {@link #methodMode} and {@link #classMode}.
 * When {@code @DirtiesContext} is declared at both the class level and the
 * method level, the configured test phases from both annotations will be honored.
 * For example, if the class mode is set to {@link ClassMode#BEFORE_EACH_TEST_METHOD
 * BEFORE_EACH_TEST_METHOD} and the method mode is set to
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD}, the context will be marked as
 * dirty both before and after the given test method.
 *
 * <h3>Supported Test Phases</h3>
 * <ul>
 * <li><strong>Before current test class</strong>: when declared at the class
 * level with class mode set to {@link ClassMode#BEFORE_CLASS BEFORE_CLASS}</li>
 * <li><strong>Before each test method in current test class</strong>: when
 * declared at the class level with class mode set to
 * {@link ClassMode#BEFORE_EACH_TEST_METHOD BEFORE_EACH_TEST_METHOD}</li>
 * <li><strong>Before current test method</strong>: when declared at the
 * method level with method mode set to
 * {@link MethodMode#BEFORE_METHOD BEFORE_METHOD}</li>
 * <li><strong>After current test method</strong>: when declared at the
 * method level with method mode set to
 * {@link MethodMode#AFTER_METHOD AFTER_METHOD}</li>
 * <li><strong>After each test method in current test class</strong>: when
 * declared at the class level with class mode set to
 * {@link ClassMode#AFTER_EACH_TEST_METHOD AFTER_EACH_TEST_METHOD}</li>
 * <li><strong>After current test class</strong>: when declared at the
 * class level with class mode set to
 * {@link ClassMode#AFTER_CLASS AFTER_CLASS}</li>
 * </ul>
 *
 * <p>{@code BEFORE_*} modes are supported by the
 * {@link org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
 * DirtiesContextBeforeModesTestExecutionListener}; {@code AFTER_*} modes are supported by the
 * {@link org.springframework.test.context.support.DirtiesContextTestExecutionListener
 * DirtiesContextTestExecutionListener}.
 *
 * <p>This annotation may be used as a <em>meta-annotation</em> to create custom
 * <em>composed annotations</em>.
 *
 * <p>As of Spring Framework 5.3, this annotation will be inherited from an
 * enclosing test class by default. See
 * {@link org.springframework.test.context.NestedTestConfiguration @NestedTestConfiguration}
 * for details.
 *
 * @author Sam Brannen
 * @author Rod Johnson
 * @since 2.0
 * @see org.springframework.test.context.ContextConfiguration
 * @see org.springframework.test.context.support.DirtiesContextBeforeModesTestExecutionListener
 * @see org.springframework.test.context.support.DirtiesContextTestExecutionListener
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface DirtiesContext {

	/**
	 * The <i>mode</i> to use when a test method is annotated with
	 * {@code @DirtiesContext}.
	 * <p>Defaults to {@link MethodMode#AFTER_METHOD AFTER_METHOD}.
	 * <p>Setting the method mode on an annotated test class has no meaning.
	 * For class-level control, use {@link #classMode} instead.
	 * @since 4.2
	 */
	MethodMode methodMode() default MethodMode.AFTER_METHOD;

	/**
	 * The <i>mode</i> to use when a test class is annotated with
	 * {@code @DirtiesContext}.
	 * <p>Defaults to {@link ClassMode#AFTER_CLASS AFTER_CLASS}.
	 * <p>Setting the class mode on an annotated test method has no meaning.
	 * For method-level control, use {@link #methodMode} instead.
	 * @since 3.0
	 */
	ClassMode classMode() default ClassMode.AFTER_CLASS;

	/**
	 * The context cache clearing <em>mode</em> to use when a context is
	 * configured as part of a hierarchy via
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}.
	 * <p>Defaults to {@link HierarchyMode#EXHAUSTIVE EXHAUSTIVE}.
	 * @since 3.2.2
	 */
	HierarchyMode hierarchyMode() default HierarchyMode.EXHAUSTIVE;


	/**
	 * Defines <i>modes</i> which determine how {@code @DirtiesContext} is
	 * interpreted when used to annotate a test method.
	 * @since 4.2
	 */
	enum MethodMode {

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> before the corresponding test method.
		 */
		BEFORE_METHOD,

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> after the corresponding test method.
		 */
		AFTER_METHOD
	}


	/**
	 * Defines <i>modes</i> which determine how {@code @DirtiesContext} is
	 * interpreted when used to annotate a test class.
	 * @since 3.0
	 */
	enum ClassMode {

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> before the test class.
		 *
		 * @since 4.2
		 */
		BEFORE_CLASS,

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> before each test method in the class.
		 *
		 * @since 4.2
		 */
		BEFORE_EACH_TEST_METHOD,

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> after each test method in the class.
		 */
		AFTER_EACH_TEST_METHOD,

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> after the test class.
		 */
		AFTER_CLASS
	}


	/**
	 * Defines <i>modes</i> which determine how the context cache is cleared
	 * when {@code @DirtiesContext} is used in a test whose context is
	 * configured as part of a hierarchy via
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}.
	 * @since 3.2.2
	 */
	enum HierarchyMode {

		/**
		 * The context cache will be cleared using an <em>exhaustive</em> algorithm
		 * that includes not only the {@linkplain HierarchyMode#CURRENT_LEVEL current level}
		 * but also all other context hierarchies that share an ancestor context
		 * common to the current test.
		 *
		 * <p>All {@code ApplicationContexts} that reside in a subhierarchy of
		 * the common ancestor context will be removed from the context cache and
		 * closed.
		 */
		EXHAUSTIVE,

		/**
		 * The {@code ApplicationContext} for the <em>current level</em> in the
		 * context hierarchy and all contexts in subhierarchies of the current
		 * level will be removed from the context cache and closed.
		 *
		 * <p>The <em>current level</em> refers to the {@code ApplicationContext}
		 * at the lowest level in the context hierarchy that is visible from the
		 * current test.
		 */
		CURRENT_LEVEL
	}

}
