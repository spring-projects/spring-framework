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
 * associated with a test is <em>dirty</em> and should be closed:
 *
 * <ul>
 *   <li>after the current test, when declared at the method level</li>
 *   <li>after each test method in the current test class, when declared at the
 *   class level with class mode set to {@link ClassMode#AFTER_EACH_TEST_METHOD
 *   AFTER_EACH_TEST_METHOD}</li>
 *   <li>after the current test class, when declared at the class level with class
 *   mode set to {@link ClassMode#AFTER_CLASS AFTER_CLASS}</li>
 * </ul>
 *
 * <p>Use this annotation if a test has modified the context &mdash; for example,
 * by replacing a bean definition or changing the state of a singleton bean.
 * Subsequent tests will be supplied a new context.
 *
 * <p>{@code @DirtiesContext} may be used as a class-level and method-level
 * annotation within the same class. In such scenarios, the
 * {@code ApplicationContext} will be marked as <em>dirty</em> after any
 * such annotated method as well as after the entire class. If the
 * {@link ClassMode} is set to {@link ClassMode#AFTER_EACH_TEST_METHOD
 * AFTER_EACH_TEST_METHOD}, the context will be marked dirty after each test
 * method in the class.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @author Rod Johnson
 * @since 2.0
 * @see org.springframework.test.context.ContextConfiguration
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface DirtiesContext {

	/**
	 * Defines <i>modes</i> which determine how {@code @DirtiesContext} is
	 * interpreted when used to annotate a test class.
	 *
	 * @since 3.0
	 */
	static enum ClassMode {

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> after the test class.
		 */
		AFTER_CLASS,

		/**
		 * The associated {@code ApplicationContext} will be marked as
		 * <em>dirty</em> after each test method in the class.
		 */
		AFTER_EACH_TEST_METHOD;
	}

	/**
	 * Defines <i>modes</i> which determine how the context cache is cleared
	 * when {@code @DirtiesContext} is used in a test whose context is
	 * configured as part of a hierarchy via
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}.
	 *
	 * @since 3.2.2
	 */
	static enum HierarchyMode {

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
		CURRENT_LEVEL;
	}


	/**
	 * The <i>mode</i> to use when a test class is annotated with
	 * {@code @DirtiesContext}.
	 * <p>Defaults to {@link ClassMode#AFTER_CLASS AFTER_CLASS}.
	 * <p>Note: Setting the class mode on an annotated test method has no meaning,
	 * since the mere presence of the {@code @DirtiesContext} annotation on a
	 * test method is sufficient.
	 *
	 * @since 3.0
	 */
	ClassMode classMode() default ClassMode.AFTER_CLASS;

	/**
	 * The context cache clearing <em>mode</em> to use when a context is
	 * configured as part of a hierarchy via
	 * {@link org.springframework.test.context.ContextHierarchy @ContextHierarchy}.
	 * <p>Defaults to {@link HierarchyMode#EXHAUSTIVE EXHAUSTIVE}.
	 *
	 * @since 3.2.2
	 */
	HierarchyMode hierarchyMode() default HierarchyMode.EXHAUSTIVE;

}
