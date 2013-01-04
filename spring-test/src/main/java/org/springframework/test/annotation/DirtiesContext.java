/*
 * Copyright 2002-2012 the original author or authors.
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
 * <ul>
 * <li>after the current test, when declared at the method level</li>
 * <li>after each test method in the current test class, when declared at the
 * class level with class mode set to {@link ClassMode#AFTER_EACH_TEST_METHOD
 * AFTER_EACH_TEST_METHOD}</li>
 * <li>after the current test class, when declared at the class level with class
 * mode set to {@link ClassMode#AFTER_CLASS AFTER_CLASS}</li>
 * </ul>
 * <p>
 * Use this annotation if a test has modified the context (for example, by
 * replacing a bean definition). Subsequent tests will be supplied a new
 * context.
 * </p>
 * <p>
 * {@code &#064;DirtiesContext} may be used as a class-level and
 * method-level annotation within the same class. In such scenarios, the
 * {@code ApplicationContext} will be marked as <em>dirty</em> after any
 * such annotated method as well as after the entire class. If the
 * {@link ClassMode} is set to {@link ClassMode#AFTER_EACH_TEST_METHOD
 * AFTER_EACH_TEST_METHOD}, the context will be marked dirty after each test
 * method in the class.
 * </p>
 *
 * @author Sam Brannen
 * @author Rod Johnson
 * @since 2.0
 */
@Documented
@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface DirtiesContext {

	/**
	 * Defines <i>modes</i> which determine how {@code &#064;DirtiesContext}
	 * is interpreted when used to annotate a test class.
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
		AFTER_EACH_TEST_METHOD
	}


	/**
	 * The <i>mode</i> to use when a test class is annotated with
	 * {@code &#064;DirtiesContext}.
	 * <p>Defaults to {@link ClassMode#AFTER_CLASS AFTER_CLASS}.
	 * <p>Note: Setting the class mode on an annotated test method has no meaning,
	 * since the mere presence of the {@code &#064;DirtiesContext}
	 * annotation on a test method is sufficient.
	 */
	ClassMode classMode() default ClassMode.AFTER_CLASS;

}
