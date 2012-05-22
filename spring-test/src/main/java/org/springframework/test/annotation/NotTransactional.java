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
 * Test annotation to indicate that a method is <i>not transactional</i>.
 *
 * @author Rod Johnson
 * @author Sam Brannen
 * @since 2.0
 * @deprecated as of Spring 3.0, in favor of moving the non-transactional test
 * method to a separate (non-transactional) test class or to a
 * {@link org.springframework.test.context.transaction.BeforeTransaction
 * &#64;BeforeTransaction} or
 * {@link org.springframework.test.context.transaction.AfterTransaction
 * &#64;AfterTransaction} method.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Deprecated
public @interface NotTransactional {
}
