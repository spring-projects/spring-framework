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
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code @Commit} is a test annotation that is used to indicate that a
 * <em>test-managed transaction</em> should be <em>committed</em> after
 * the test method has completed.
 *
 * <p>Consult the class-level Javadoc for
 * {@link org.springframework.test.context.transaction.TransactionalTestExecutionListener}
 * for an explanation of <em>test-managed transactions</em>.
 *
 * <p>When declared as a class-level annotation, {@code @Commit} defines
 * the default commit semantics for all test methods within the test class
 * hierarchy. When declared as a method-level annotation, {@code @Commit}
 * defines commit semantics for the specific test method, potentially
 * overriding class-level default commit or rollback semantics.
 *
 * <p><strong>Warning</strong>: {@code @Commit} can be used as direct
 * replacement for {@code @Rollback(false)}; however, it should
 * <strong>not</strong> be declared alongside {@code @Rollback}. Declaring
 * {@code @Commit} and {@code @Rollback} on the same test method or on the
 * same test class is unsupported and may lead to unpredictable results.
 *
 * @author Sam Brannen
 * @since 4.2
 * @see Rollback
 * @see org.springframework.test.context.transaction.TransactionalTestExecutionListener
 */
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Rollback(false)
public @interface Commit {
}
