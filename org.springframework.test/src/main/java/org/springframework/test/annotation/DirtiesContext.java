/*
 * Copyright 2002-2009 the original author or authors.
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
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Test annotation which indicates that the
 * {@link org.springframework.context.ApplicationContext ApplicationContext}
 * associated with a test is <em>dirty</em> and should be closed:
 * <ul>
 * <li>after the current test, when declared at the method level, or</li>
 * <li>after the current test class, when declared at the class level.</li>
 * </ul>
 * <p>
 * Use this annotation if a test has modified the context (for example, by
 * replacing a bean definition). Subsequent tests will be supplied a new
 * context.
 * </p>
 * <p>
 * <code>&#064;DirtiesContext</code> may be used as a class-level and
 * method-level annotation within the same class. In such scenarios, the
 * <code>ApplicationContext</code> will be marked as <em>dirty</em> after any
 * such annotated method as well as after the entire class.
 * </p>
 * 
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 */
@Target( { ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface DirtiesContext {

}
