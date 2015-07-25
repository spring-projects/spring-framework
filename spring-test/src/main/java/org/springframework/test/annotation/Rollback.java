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

package org.springframework.test.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

/**
 * Test annotation used to indicate whether a <em>test-managed transaction</em>
 * should be <em>rolled back</em> after the test method has completed.
 *
 * <p>Consult the class-level Javadoc for
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * for an explanation of <em>test-managed transactions</em>.
 *
 * <p>When declared as a class-level annotation, {@code @Rollback} defines
 * the default rollback semantics for all test methods within the test class
 * hierarchy. When declared as a method-level annotation, {@code @Rollback}
 * defines rollback semantics for the specific test method, potentially
 * overriding class-level default rollback semantics.
 *
 * <p>As of Spring Framework 4.0, this annotation may be used as a
 * <em>meta-annotation</em> to create custom <em>composed annotations</em>.
 *
 * @author Sam Brannen
 * @since 2.5
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
@Documented
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Rollback {

	/**
	 * Whether the <em>test-managed transaction</em> should be rolled back
	 * after the test method has completed.
	 * <p>If {@code true}, the transaction will be rolled back; otherwise,
	 * the transaction will be committed.
	 */
	boolean value() default true;

}
